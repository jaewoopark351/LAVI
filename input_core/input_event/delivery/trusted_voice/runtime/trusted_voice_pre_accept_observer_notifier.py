#20260905_kpopmodder: Notify pre-accept VoiceInput observers with fail-closed status.
from __future__ import annotations


class TrustedVoicePreAcceptObserverNotifier:
    def __init__(self, observers, diagnostics):
        self._observers = tuple(observers)
        if not all(callable(observer) for observer in self._observers):
            raise TypeError("every Voice delivery observer must be callable")
        self._diagnostics = diagnostics

    def notify(self, event: object) -> bool:
        for observer in self._observers:
            try:
                observer(event)
            except Exception:
                self._diagnostics.log_failure("pre_accept_observer")
                return False
        return True


__all__ = ("TrustedVoicePreAcceptObserverNotifier",)
