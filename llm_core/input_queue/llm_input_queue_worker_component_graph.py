#20260905_kpopmodder: Assemble queue storage, thread state, and worker collaborators.
from __future__ import annotations

from llm_core.input_queue_runtime import LLMInputQueueRuntime

from .claim_aware_lavi_input_queue_sink import ClaimAwareLlmInputQueueSink
from .llm_input_queue_pending_input_clearer import (
    LlmInputQueuePendingInputClearer,
)
from .llm_input_queue_submission_coordinator import (
    LlmInputQueueSubmissionCoordinator,
)
from .storage import LlmInputQueueStore
from .worker_lifecycle import LlmInputQueueWorkerThreadState


class LlmInputQueueWorkerComponentGraph:
    def __init__(self, worker):
        self.queue_store = LlmInputQueueStore()
        self.thread_state = LlmInputQueueWorkerThreadState()
        worker._components = self
        self.queue_runtime = LLMInputQueueRuntime(worker)
        self.claim_aware_input_queue_sink = ClaimAwareLlmInputQueueSink(worker)
        self.submission_coordinator = LlmInputQueueSubmissionCoordinator(worker)
        self.pending_input_clearer = LlmInputQueuePendingInputClearer(worker)


__all__ = ("LlmInputQueueWorkerComponentGraph",)
