#20260905_kpopmodder: Log only bounded routed-input failure boundaries.
from __future__ import annotations


class RoutedInputDispatchDiagnosticLogger:
    def __init__(self, log_callback):
        if not callable(log_callback):
            raise TypeError("log_callback must be callable")
        self._log_callback = log_callback

    def log_failure(self, boundary: str) -> None:
        try:
            self._log_callback(
                f"[LLM] routed input dispatch failed: boundary={boundary}"
            )
        except Exception:
            return


__all__ = ("RoutedInputDispatchDiagnosticLogger",)
