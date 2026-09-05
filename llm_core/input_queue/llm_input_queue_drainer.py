#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from core.logger import log_print
from input_core.input_event.provenance.trusted_user_ingress import (
    QueueOwnedIngressDelivery,
)


class LlmInputQueueDrainer:
    def __init__(self, worker, *, log_callback=log_print) -> None:
        if not callable(log_callback):
            raise TypeError("log_callback must be callable")
        self._worker = worker
        self._log_callback = log_callback

    def drain(self) -> None:
        while True:
            next_input = self._take_next_input()
            if next_input is _QUEUE_EMPTY:
                return
            if next_input is _FETCH_FAILED:
                continue
            try:
                response_callback = self._worker.response_callback
                if type(next_input) is QueueOwnedIngressDelivery:
                    response_callback = getattr(
                        self._worker,
                        "queued_response_callback",
                        None,
                    )
                    if not callable(response_callback):
                        next_input.abandon()
                        continue
                response_generator = response_callback(
                    next_input,
                    self._worker.history_callback(),
                    self._worker.system_prompt_callback(),
                )
                for _ in response_generator:
                    pass
            except Exception:
                self._log_bounded_failure("response_callback")
            finally:
                if type(next_input) is QueueOwnedIngressDelivery:
                    next_input.abandon()
                try:
                    self._worker.queue_updated_callback()
                except Exception:
                    self._log_bounded_failure("queue_update")

    def _take_next_input(self):
        try:
            with self._worker.input_queue_lock:
                if self._worker.input_queue.empty():
                    return _QUEUE_EMPTY
                return self._worker.input_queue.get()
        except Exception:
            self._log_bounded_failure("queue_fetch")
            return _FETCH_FAILED

    def _log_bounded_failure(self, boundary: str) -> None:
        try:
            self._log_callback(
                "[LLMInputQueueRuntime] operation failed: "
                f"boundary={boundary}"
            )
        except Exception:
            return


_QUEUE_EMPTY = object()
_FETCH_FAILED = object()


__all__ = ("LlmInputQueueDrainer",)
