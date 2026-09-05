#20260905_kpopmodder: Log only bounded VoiceInput delivery failure boundaries.
from __future__ import annotations


class TrustedVoiceDeliveryDiagnosticLogger:
    def __init__(self, log_callback):
        if not callable(log_callback):
            raise TypeError("log_callback must be callable")
        self._log_callback = log_callback

    def log_failure(self, boundary: str) -> None:
        try:
            self._log_callback(
                "[TrustedVoiceInputFinalEnqueueCoordinator] "
                f"delivery failed: boundary={boundary}"
            )
        except Exception:
            return


__all__ = ("TrustedVoiceDeliveryDiagnosticLogger",)
