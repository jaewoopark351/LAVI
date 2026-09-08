#20260907_kpopmodder: Hold mutable lifecycle facts only under the external command lock.
from __future__ import annotations


class CommandFeedbackLifecycleState:
    def __init__(self) -> None:
        self.reservation = None
        self.context = None
        self.latest_status = ""
        self.latest_result_reason = ""
        self.latest_evidence_sequence = None
        self.dispatch_started_observed = False
        self.start_claimed = False
        self.terminal_claimed = False
        self.lifecycle_token = None
        self.lifecycle_grant = None
        self.start_publication_permit = None

    def reset_active(self) -> None:
        self.context = None
        self.latest_status = ""
        self.latest_result_reason = ""
        self.latest_evidence_sequence = None
        self.dispatch_started_observed = False
        self.start_claimed = False
        self.terminal_claimed = False
        self.lifecycle_token = None
        self.lifecycle_grant = None
        self.start_publication_permit = None


__all__ = ("CommandFeedbackLifecycleState",)
