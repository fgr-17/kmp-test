from __future__ import annotations

import os
from typing import Any

import requests

DEFAULT_CLOUD = os.environ.get("GREETING_CLOUD_URL", "http://127.0.0.1:8081")


class CloudClient:
    def __init__(self, base_url: str = DEFAULT_CLOUD) -> None:
        self.base_url = base_url.rstrip("/")

    def health(self) -> dict[str, Any]:
        response = requests.get(f"{self.base_url}/health", timeout=5)
        response.raise_for_status()
        return response.json()

    def list_notes(self) -> list[dict[str, Any]]:
        response = requests.get(f"{self.base_url}/notes", timeout=5)
        response.raise_for_status()
        return response.json()

    def contains_text(self, text: str) -> bool:
        return any(note.get("text") == text for note in self.list_notes())

    def clear(self) -> None:
        response = requests.delete(f"{self.base_url}/notes", timeout=5)
        response.raise_for_status()
