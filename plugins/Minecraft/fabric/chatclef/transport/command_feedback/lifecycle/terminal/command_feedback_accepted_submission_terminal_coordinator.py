#20260907_kpopmodder: Stage cautious terminal truth for accepted commands with no Java callback.
from __future__ import annotations

from ..kind import (
    CommandFeedbackLifecycleKindProfile,
    CommandFeedbackLifecycleKindProfileRegistry,
)
from .command_terminal_fact import CommandTerminalFact


class CommandFeedbackAcceptedSubmissionTerminalCoordinator:
    STATUS = "accepted_without_result_callback"
    REASON = "accepted_submission_without_result_callback"

    def __init__(self, *, lifecycle_kind_profiles=None) -> None:
        self._profiles = (
            lifecycle_kind_profiles
            or CommandFeedbackLifecycleKindProfileRegistry()
        )

    def stage_if_required(
        self,
        *,
        context: object,
        start_permit: object,
        terminal_lifecycle,
    ):
        if context is None or start_permit is None:
            return start_permit
        descriptor = getattr(context, "descriptor", None)
        try:
            profile = self._profiles.profile(
                getattr(descriptor, "command_name", "")
            )
        except KeyError:
            return start_permit
        if (
            profile.terminal_trigger
            != CommandFeedbackLifecycleKindProfile.ACCEPTED_SUBMISSION_CAUTION
        ):
            return start_permit
        owner_token = getattr(context, "owner_token", None)
        claimed_context = terminal_lifecycle.claim(owner_token)
        if claimed_context is not context:
            return start_permit
        fact = CommandTerminalFact(
            descriptor=descriptor,
            status=self.STATUS,
            verified=False,
            dispatch_started=False,
            result_reason=self.REASON,
            event_id=getattr(context, "event_id", ""),
            owner_token=owner_token,
        )
        terminal_lifecycle.stage(owner_token, fact)
        return start_permit


__all__ = ("CommandFeedbackAcceptedSubmissionTerminalCoordinator",)
