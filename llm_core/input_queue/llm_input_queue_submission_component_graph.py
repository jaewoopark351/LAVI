#20260905_kpopmodder: Assemble plain-input queue commit and worker-start owners.
from __future__ import annotations

from .llm_input_queue_submission_committer import (
    LlmInputQueueSubmissionCommitter,
)
from .llm_input_queue_submission_worker_starter import (
    LlmInputQueueSubmissionWorkerStarter,
)


class LlmInputQueueSubmissionComponentGraph:
    def __init__(self, worker):
        self.committer = LlmInputQueueSubmissionCommitter(worker)
        self.worker_starter = LlmInputQueueSubmissionWorkerStarter(worker)


__all__ = ("LlmInputQueueSubmissionComponentGraph",)
