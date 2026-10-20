from __future__ import annotations

import time
from typing import Any
from urllib.parse import quote

import requests

from common.driver import NotesAppDriver

DEFAULT_BRIDGE = "http://127.0.0.1:18081"


class DesktopDriver(NotesAppDriver):
    """Talks to the JDK HttpServer bridge started when GREETING_E2E=1."""

    def __init__(self, bridge_url: str = DEFAULT_BRIDGE) -> None:
        self.base = bridge_url.rstrip("/")
        self._wait_until_ready()

    def _wait_until_ready(self, timeout_s: float = 120.0) -> None:
        deadline = time.time() + timeout_s
        last_error: Exception | None = None
        while time.time() < deadline:
            try:
                response = requests.get(f"{self.base}/e2e/health", timeout=2)
                if response.ok:
                    return
            except Exception as exc:  # noqa: BLE001 — retry until bridge is up
                last_error = exc
            time.sleep(0.5)
        raise RuntimeError(f"Desktop E2E bridge not reachable at {self.base}: {last_error}")

    def reset(self) -> None:
        requests.post(f"{self.base}/e2e/reset", timeout=5).raise_for_status()

    def set_online(self, online: bool) -> None:
        value = "true" if online else "false"
        requests.post(f"{self.base}/e2e/online?value={value}", timeout=5).raise_for_status()

    def add_note(self, text: str) -> None:
        requests.post(f"{self.base}/e2e/add?text={quote(text)}", timeout=5).raise_for_status()

    def sync_notes(self) -> None:
        requests.post(f"{self.base}/e2e/sync", timeout=10).raise_for_status()
        time.sleep(0.3)

    def visible_notes(self) -> list[tuple[str, str]]:
        payload: list[dict[str, Any]] = requests.get(f"{self.base}/e2e/notes", timeout=5).json()
        return [(item["syncStatus"].upper(), item["text"]) for item in payload]
