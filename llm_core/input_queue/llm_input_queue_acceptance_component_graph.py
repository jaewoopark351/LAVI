#20260905_kpopmodder: Assemble independent queue display and worker-start effects.
from __future__ import annotations

from .llm_input_queue_acceptance_diagnostic_logger import (
    LlmInputQueueAcceptanceDiagnosticLogger,
)
from .llm_input_queue_acceptance_display_effect import (
    LlmInputQueueAcceptanceDisplayEffect,
)
from .llm_input_queue_acceptance_worker_start_effect import (
    LlmInputQueueAcceptanceWorkerStartEffect,
)


class LlmInputQueueAcceptanceComponentGraph:
    def __init__(self, worker, *, log_callback):
        self.diagnostics = LlmInputQueueAcceptanceDiagnosticLogger(log_callback)
        self.display_effect = LlmInputQueueAcceptanceDisplayEffect(
            worker=worker,
            diagnostics=self.diagnostics,
        )
        self.worker_start_effect = LlmInputQueueAcceptanceWorkerStartEffect(
            worker=worker,
            diagnostics=self.diagnostics,
        )


__all__ = ("LlmInputQueueAcceptanceComponentGraph",)
