from __future__ import annotations

import os
import re
import time

from playwright.sync_api import sync_playwright

from common import tags
from common.driver import NotesAppDriver

DEFAULT_WEB_URL = os.environ.get("GREETING_WEB_URL", "http://127.0.0.1:8080/?e2e=1")


class WebDriver(NotesAppDriver):
    """Playwright driver for webApp (drives the `#e2e-overlay` DOM when ?e2e=1)."""

    def __init__(self, base_url: str = DEFAULT_WEB_URL) -> None:
        show_ui = os.environ.get("SHOW_UI", "0") == "1"
        self._playwright = sync_playwright().start()
        self._browser = self._playwright.chromium.launch(
            headless=not show_ui,
            slow_mo=250 if show_ui else 0,
        )
        self._page = self._browser.new_page()
        self._page.goto(base_url, wait_until="domcontentloaded")
        self._page.wait_for_selector("#e2e-overlay", timeout=120_000)
        self._overlay = self._page.locator("#e2e-overlay")

    def reset(self) -> None:
        self._page.reload(wait_until="domcontentloaded")
        self._page.wait_for_selector("#e2e-overlay", timeout=120_000)
        self._overlay = self._page.locator("#e2e-overlay")

    def set_online(self, online: bool) -> None:
        box = self._overlay.locator(f"#{tags.ONLINE_SWITCH}")
        if box.is_checked() != online:
            box.click()

    def add_note(self, text: str) -> None:
        self._overlay.locator(f"#{tags.NOTE_INPUT}").fill(text)
        self._overlay.locator(f"#{tags.ADD_NOTE}").click()
        self._overlay.locator(f"#{tags.NOTES_LIST}").get_by_text(text).wait_for(timeout=5_000)

    def sync_notes(self) -> None:
        self._overlay.locator(f"#{tags.SYNC}").click()
        self._page.wait_for_function(
            """() => {
                const el = document.querySelector('#e2e-overlay #status');
                const text = (el && el.textContent) || '';
                return text.startsWith('Synced')
                    || text.startsWith('Nothing')
                    || text.startsWith('Still')
                    || text.startsWith('Sync failed');
            }""",
            timeout=15_000,
        )
        time.sleep(0.2)

    def visible_notes(self) -> list[tuple[str, str]]:
        raw = self._overlay.locator(f"#{tags.NOTES_LIST}").inner_text()
        notes: list[tuple[str, str]] = []
        for part in raw.split("|"):
            part = part.strip()
            match = re.match(r"\[(PENDING|SYNCED)]\s*(.+)", part, flags=re.IGNORECASE)
            if match:
                notes.append((match.group(1).upper(), match.group(2).strip()))
        return notes

    def close(self) -> None:
        self._browser.close()
        self._playwright.stop()
