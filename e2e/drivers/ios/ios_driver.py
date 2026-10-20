from __future__ import annotations

import json
import os
import re
import subprocess
import time

from appium import webdriver
from appium.options.ios import XCUITestOptions
from appium.webdriver.common.appiumby import AppiumBy

from common import tags
from common.driver import NotesAppDriver

DEFAULT_APPIUM = os.environ.get("APPIUM_SERVER_URL", "http://127.0.0.1:4723")
DEFAULT_BUNDLE = os.environ.get("GREETING_IOS_BUNDLE_ID", "com.jetbrains.greetingkmp.GreetingKMP")
DEFAULT_DEVICE = os.environ.get("GREETING_IOS_DEVICE", "iPhone 16")


def _booted_udid(device_name: str) -> str | None:
    """Use a simulator that is already booted.

    Without a platformVersion, XCUITest tries to create a simulator on the
    newest Xcode SDK, which fails when that runtime has no matching device.
    """
    explicit = os.environ.get("GREETING_IOS_UDID")
    if explicit:
        return explicit
    try:
        raw = subprocess.check_output(
            ["xcrun", "simctl", "list", "devices", "booted", "-j"],
            text=True,
        )
    except (subprocess.CalledProcessError, FileNotFoundError):
        return None
    booted = [
        device
        for runtime_devices in json.loads(raw).get("devices", {}).values()
        for device in runtime_devices
        if device.get("isAvailable", True)
    ]
    named = [device for device in booted if device.get("name") == device_name]
    chosen = (named or booted or [None])[0]
    if chosen is None:
        return None
    return chosen.get("udid")


class IosDriver(NotesAppDriver):
    """Appium XCUITest driver. Requires Simulator + Appium + built iosApp."""

    def __init__(self) -> None:
        options = XCUITestOptions()
        options.platform_name = "iOS"
        options.automation_name = "XCUITest"
        options.device_name = DEFAULT_DEVICE
        options.bundle_id = DEFAULT_BUNDLE
        udid = _booted_udid(DEFAULT_DEVICE)
        if udid:
            options.udid = udid
        platform_version = os.environ.get("GREETING_IOS_PLATFORM_VERSION")
        if platform_version:
            options.platform_version = platform_version
        options.set_capability("appium:noReset", True)
        # Appium otherwise launches DeviceHub/Simulator even when simctl
        # already booted the device with no window.
        options.is_headless = os.environ.get("SHOW_UI", "0") != "1"
        self._driver = webdriver.Remote(DEFAULT_APPIUM, options=options)
        self._driver.implicitly_wait(10)

    def _id(self, accessibility_id: str):
        return self._driver.find_element(AppiumBy.ACCESSIBILITY_ID, accessibility_id)

    def reset(self) -> None:
        self._driver.terminate_app(DEFAULT_BUNDLE)
        self._driver.activate_app(DEFAULT_BUNDLE)
        self._id(tags.STATUS)

    def set_online(self, online: bool) -> None:
        toggle = self._id(tags.ONLINE_SWITCH)
        value = str(toggle.get_attribute("value"))
        is_on = value in ("1", "true", "True")
        if is_on != online:
            # The row is one accessibility element and does not toggle.
            # The knob is a nested switch; click that object, not a raw point.
            knobs = self._driver.find_elements(
                AppiumBy.IOS_CLASS_CHAIN,
                '**/XCUIElementTypeSwitch[`name == "online_switch"`]/XCUIElementTypeSwitch',
            )
            if knobs:
                try:
                    knobs[0].click()
                except Exception:
                    self._tap_switch_knob()
            else:
                self._tap_switch_knob()
            time.sleep(0.3)

    def _tap_switch_knob(self) -> None:
        source = self._driver.page_source
        match = re.search(
            r'XCUIElementTypeSwitch[^>]*accessible="false"[^>]*'
            r'x="(\d+)" y="(\d+)" width="(\d+)" height="(\d+)"',
            source,
        )
        if not match:
            self._id(tags.ONLINE_SWITCH).click()
            return
        x, y, width, height = (int(group) for group in match.groups())
        self._driver.execute_script(
            "mobile: tap",
            {"x": x + width // 2, "y": y + height // 2},
        )

    def add_note(self, text: str) -> None:
        field = self._id(tags.NOTE_INPUT)
        field.clear()
        field.send_keys(text)
        self._id(tags.ADD_NOTE).click()
        time.sleep(0.3)

    def sync_notes(self) -> None:
        self._id(tags.SYNC).click()
        deadline = time.time() + 8
        while time.time() < deadline:
            if any(status == "SYNCED" for status, _ in self.visible_notes()):
                return
            status = self._id(tags.STATUS).text or ""
            if "Sync failed" in status or "Still offline" in status or "Nothing pending" in status:
                return
            time.sleep(0.4)

    def visible_notes(self) -> list[tuple[str, str]]:
        raw = self._driver.page_source
        notes: list[tuple[str, str]] = []
        seen: set[tuple[str, str]] = set()
        for match in re.finditer(
            r"\[(PENDING|SYNCED)]\s*([^\"<]+)",
            raw,
            flags=re.IGNORECASE,
        ):
            item = (match.group(1).upper(), match.group(2).strip())
            if item not in seen:
                seen.add(item)
                notes.append(item)
        return notes

    def close(self) -> None:
        self._driver.quit()
