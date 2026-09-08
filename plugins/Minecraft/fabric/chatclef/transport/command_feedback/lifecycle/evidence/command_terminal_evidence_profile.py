#20260907_kpopmodder: Describe one closed command terminal-evidence policy.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class CommandTerminalEvidenceProfile:
    DISABLED = "disabled"
    CAUTIOUS = "cautious"
    VERIFIED = "verified"

    command_name: str
    lifecycle_kind: str
    profile_id: str
    rollout_state: str
    success_evaluator_id: str
    progress_reasons: tuple[str, ...] = ("dispatch_started",)
    response_lifecycle_kind: str = "finite_task"

    def __post_init__(self) -> None:
        if self.rollout_state not in {self.DISABLED, self.CAUTIOUS, self.VERIFIED}:
            raise ValueError("command evidence rollout state is invalid")
        if not self.command_name or not self.lifecycle_kind or not self.profile_id:
            raise ValueError("command evidence profile identity is incomplete")
        if self.response_lifecycle_kind not in {
            "finite_task",
            "persistent_task",
            "asynchronous_immediate",
            "specialized_control",
        }:
            raise ValueError("command evidence response lifecycle kind is invalid")

    def accepts_progress(self, reason: object) -> bool:
        return type(reason) is str and reason in self.progress_reasons


__all__ = ("CommandTerminalEvidenceProfile",)
