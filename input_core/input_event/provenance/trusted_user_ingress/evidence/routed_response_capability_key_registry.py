#20260905_kpopmodder: Owns one evidence object's bounded response-capability keys.
from __future__ import annotations


class RoutedResponseCapabilityKeyRegistry:
    def __init__(self) -> None:
        self._keys: set[tuple[str, str]] = set()

    @property
    def keys(self) -> set[tuple[str, str]]:
        return self._keys

    def reserve_locked(self, key: tuple[str, str]) -> bool:
        if key in self._keys:
            return False
        self._keys.add(key)
        return True

    def clear_locked(self) -> None:
        self._keys.clear()


__all__ = ("RoutedResponseCapabilityKeyRegistry",)
