#20260905_kpopmodder: Sequence independent post-acceptance queue effects.
from __future__ import annotations

from core.logger import log_print

from .llm_input_queue_acceptance_component_graph import (
    LlmInputQueueAcceptanceComponentGraph,
)


class LlmInputQueueAcceptanceEffects:
    def __init__(self, worker, *, log_callback=log_print) -> None:
        self._components = LlmInputQueueAcceptanceComponentGraph(
            worker,
            log_callback=log_callback,
        )

    def run(self) -> None:
        self._components.display_effect.run()
        self._components.worker_start_effect.run()


__all__ = ("LlmInputQueueAcceptanceEffects",)
