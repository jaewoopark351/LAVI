#20260905_kpopmodder: Log bounded post-acceptance queue effect failures.
from __future__ import annotations


class LlmInputQueueAcceptanceDiagnosticLogger:
    def __init__(self, log_callback):
        if not callable(log_callback):
            raise TypeError("log_callback must be callable")
        self._log_callback = log_callback

    def log_failure(self, boundary: str) -> None:
        try:
            self._log_callback(
                "[ClaimAwareLlmInputQueueSink] "
                f"post-acceptance effect failed: boundary={boundary}"
            )
        except Exception:
            return


__all__ = ("LlmInputQueueAcceptanceDiagnosticLogger",)
