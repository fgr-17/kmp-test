from __future__ import annotations

from abc import ABC, abstractmethod


class NotesAppDriver(ABC):
    """One concrete subclass per client app (web / android / ios / desktop)."""

    @abstractmethod
    def set_online(self, online: bool) -> None:
        raise NotImplementedError

    @abstractmethod
    def add_note(self, text: str) -> None:
        raise NotImplementedError

    @abstractmethod
    def sync_notes(self) -> None:
        raise NotImplementedError

    @abstractmethod
    def visible_notes(self) -> list[tuple[str, str]]:
        """Return (sync_status, text) pairs currently shown by the app."""
        raise NotImplementedError

    def reset(self) -> None:
        """Drop in-memory notes so the next scenario starts from an empty app."""
        return None

    def close(self) -> None:
        return None
