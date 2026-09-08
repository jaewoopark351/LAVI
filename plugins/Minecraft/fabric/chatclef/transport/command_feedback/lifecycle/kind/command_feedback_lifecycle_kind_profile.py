#20260907_kpopmodder: Classify response lifecycle truth independently from registry admission metadata.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class CommandFeedbackLifecycleKindProfile:
    FINITE_TASK = "finite_task"
    PERSISTENT_TASK = "persistent_task"
    ASYNCHRONOUS_IMMEDIATE = "asynchronous_immediate"
    SPECIALIZED_CONTROL = "specialized_control"
    RESULT_CALLBACK = "result_callback"
    ACCEPTED_SUBMISSION_CAUTION = "accepted_submission_caution"
    SPECIALIZED_CONTROL_RESULT = "specialized_control_result"

    command_name: str
    response_lifecycle_kind: str
    terminal_trigger: str

    def __post_init__(self) -> None:
        if type(self.command_name) is not str or not self.command_name:
            raise ValueError("command lifecycle kind requires a command name")
        if self.response_lifecycle_kind not in {
            self.FINITE_TASK,
            self.PERSISTENT_TASK,
            self.ASYNCHRONOUS_IMMEDIATE,
            self.SPECIALIZED_CONTROL,
        }:
            raise ValueError("command response lifecycle kind is invalid")
        if self.terminal_trigger not in {
            self.RESULT_CALLBACK,
            self.ACCEPTED_SUBMISSION_CAUTION,
            self.SPECIALIZED_CONTROL_RESULT,
        }:
            raise ValueError("command terminal trigger is invalid")


__all__ = ("CommandFeedbackLifecycleKindProfile",)
