#20260905_kpopmodder: Sequence plain-input queue commit and worker start.
from __future__ import annotations

from .llm_input_queue_submission_component_graph import (
    LlmInputQueueSubmissionComponentGraph,
)


class LlmInputQueueSubmissionCoordinator:
    def __init__(self, worker) -> None:
        self._components = LlmInputQueueSubmissionComponentGraph(worker)

    def submit(self, value) -> None:
        self._components.committer.commit(value)
        self._components.worker_starter.start()


__all__ = ("LlmInputQueueSubmissionCoordinator",)
