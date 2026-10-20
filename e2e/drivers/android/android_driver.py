from __future__ import annotations

import os
import re
import subprocess
import sys
import time
from pathlib import Path

from appium import webdriver
from appium.options.android import UiAutomator2Options
from appium.webdriver.common.appiumby import AppiumBy

from common.driver import NotesAppDriver

REPO = Path(__file__).resolve().parents[3]
DEFAULT_APP = os.environ.get(
    "GREETING_ANDROID_APP",
    str(REPO / "androidApp/build/outputs/apk/debug/androidApp-debug.apk"),
)
DEFAULT_APPIUM = os.environ.get("APPIUM_SERVER_URL", "http://127.0.0.1:4723")


def _android_sdk() -> str:
    for key in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        value = os.environ.get(key)
        if value:
            return value
    home = os.path.expanduser("~")
    if sys.platform == "darwin":
        return os.path.join(home, "Library", "Android", "sdk")
    local = os.environ.get("LOCALAPPDATA")
    if local:
        return os.path.join(local, "Android", "Sdk")
    return os.path.join(home, "Android", "Sdk")


class AndroidDriver(NotesAppDriver):
    """Appium UiAutomator2 driver.

    Compose testTags are not exposed as Android resource-ids in this app, so
    the driver uses the visible controls UiAutomator already sees.
    """

    def __init__(self) -> None:
        # App talks to 127.0.0.1:8081; reverse maps that to the host backend.
        subprocess.check_call(
            [self._adb(), "reverse", "tcp:8081", "tcp:8081"],
            stdout=subprocess.DEVNULL,
        )
        options = UiAutomator2Options()
        options.platform_name = "Android"
        options.automation_name = "UiAutomator2"
        options.app = os.path.abspath(DEFAULT_APP)
        options.app_package = "com.jetbrains.greetingkmp"
        options.app_activity = ".MainActivity"
        options.set_capability("appium:autoGrantPermissions", True)
        options.set_capability("appium:appWaitActivity", "*")
        options.set_capability("appium:enforceAppInstall", True)
        self._driver = webdriver.Remote(DEFAULT_APPIUM, options=options)
        self._driver.implicitly_wait(15)
        self._wait_until_ready()

    def reset(self) -> None:
        self._driver.terminate_app("com.jetbrains.greetingkmp")
        self._driver.activate_app("com.jetbrains.greetingkmp")
        self._wait_until_ready()

    def _wait_until_ready(self) -> None:
        self._ui('new UiSelector().text("Add note")')
        # Greetings load asynchronously and push the notes controls down.
        # Wait until that block settles so later taps hit the buttons.
        deadline = time.time() + 20
        while time.time() < deadline:
            source = self._driver.page_source
            if "successful launch" in source or "Error occurred" in source:
                break
            time.sleep(0.3)

    def _ui(self, selector: str):
        return self._driver.find_element(AppiumBy.ANDROID_UIAUTOMATOR, selector)

    def set_online(self, online: bool) -> None:
        switch = self._ui("new UiSelector().checkable(true)")
        checked = str(switch.get_attribute("checked")).lower() == "true"
        if checked != online:
            switch.click()

    def add_note(self, text: str) -> None:
        field = self._driver.find_element(AppiumBy.CLASS_NAME, "android.widget.EditText")
        field.click()
        field.clear()
        field.send_keys(text)
        self._dismiss_keyboard()
        self._click_labeled("Add note")
        time.sleep(0.5)

    def sync_notes(self) -> None:
        self._dismiss_keyboard()
        self._click_labeled("Sync")
        deadline = time.time() + 8
        while time.time() < deadline:
            if any(status == "SYNCED" for status, _ in self.visible_notes()):
                return
            if "Sync failed" in self._driver.page_source or "Still offline" in self._driver.page_source:
                return
            time.sleep(0.4)

    def _dismiss_keyboard(self) -> None:
        shown = subprocess.run(
            [self._adb(), "shell", "dumpsys", "input_method"],
            check=False,
            capture_output=True,
            text=True,
        ).stdout
        if "mInputShown=true" not in shown:
            return
        try:
            self._driver.hide_keyboard()
        except Exception:
            pass
        time.sleep(0.3)

    def _adb(self) -> str:
        return os.path.join(_android_sdk(), "platform-tools", "adb")

    def _click_labeled(self, label: str) -> None:
        resource_id = "add_note" if label == "Add note" else "sync"
        node = self._ui(f'new UiSelector().resourceId("{resource_id}")')
        rect = node.rect
        x = int(rect["x"] + rect["width"] / 2)
        y = int(rect["y"] + rect["height"] / 2)
        subprocess.check_call(
            [self._adb(), "shell", "input", "tap", str(x), str(y)],
            stdout=subprocess.DEVNULL,
        )

    def visible_notes(self) -> list[tuple[str, str]]:
        raw = self._driver.page_source
        notes: list[tuple[str, str]] = []
        for match in re.finditer(
            r"\[(PENDING|SYNCED)]\s*([^<\"]+)",
            raw,
            flags=re.IGNORECASE,
        ):
            notes.append((match.group(1).upper(), match.group(2).strip()))
        return notes

    def close(self) -> None:
        self._driver.quit()
