#20260905_kpopmodder: Owns the terminal closed state for one provider-binding component.
from __future__ import annotations


class ProviderBindingLifecycleState:
    """Make provider-binding shutdown one-way and idempotent."""

    def __init__(self) -> None:
        self._closed = False

    @property
    def closed(self) -> bool:
        return self._closed

    def close_once(self) -> bool:
        if self._closed:
            return False
        self._closed = True
        return True


__all__ = ("ProviderBindingLifecycleState",)
