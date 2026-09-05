#20260905_kpopmodder: Notify post-accept VoiceInput observers without revoking queue ownership.
from __future__ import annotations


class TrustedVoicePostAcceptObserverNotifier:
    def __init__(self, observers, diagnostics):
        self._observers = tuple(observers)
        if not all(callable(observer) for observer in self._observers):
            raise TypeError("every Voice delivery observer must be callable")
        self._diagnostics = diagnostics

    def notify(self, event: object) -> None:
        for observer in self._observers:
            try:
                observer(event)
            except Exception:
                self._diagnostics.log_failure("post_accept_observer")


__all__ = ("TrustedVoicePostAcceptObserverNotifier",)
