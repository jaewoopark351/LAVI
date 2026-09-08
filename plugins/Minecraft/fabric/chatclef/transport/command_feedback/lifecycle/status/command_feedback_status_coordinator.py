#20260907_kpopmodder: Derive read-only status from one live descriptor and approved evidence.
#20260908_kpopmodder: Evaluate contextual STATUS ownership under the existing synchronized inspection.
from __future__ import annotations

from ..state.command_feedback_lifecycle_snapshot import (
    CommandFeedbackLifecycleSnapshot,
)
from .claim import (
    CommandStatusDescriptorEligibilityValidator,
    ContextualCommandStatusClaimEvaluation,
    ContextualCommandStatusClaimEvaluator,
    ContextualCommandStatusClaimFailure,
)
from .command_status_resolution import CommandStatusResolution
from .command_status_target_resolver import CommandStatusTargetResolver


class CommandFeedbackStatusCoordinator:
    def __init__(
        self,
        *,
        evidence_profiles,
        target_resolver=None,
        claim_evaluator=None,
        descriptor_validator=None,
    ) -> None:
        self._evidence_profiles = evidence_profiles
        self._target_resolver = target_resolver or CommandStatusTargetResolver()
        self._descriptor_validator = (
            descriptor_validator
            if descriptor_validator is not None
            else CommandStatusDescriptorEligibilityValidator(
                evidence_profiles=evidence_profiles,
            )
        )
        self._claim_evaluator = claim_evaluator or ContextualCommandStatusClaimEvaluator(
            target_resolver=self._target_resolver,
            descriptor_validator=self._descriptor_validator,
        )

    def inspect(
        self,
        *,
        state,
        active_command: object,
        connected: bool,
        quarantine_active: bool,
        query: object = None,
        target_item: str | None = None,
    ) -> CommandFeedbackLifecycleSnapshot:
        context = state.context
        if context is None or state.terminal_claimed:
            addressed = self._addressed(query)
            return CommandFeedbackLifecycleSnapshot(
                state=(
                    CommandFeedbackLifecycleSnapshot.IDLE
                    if addressed
                    else CommandFeedbackLifecycleSnapshot.UNAVAILABLE
                ),
                query_matched=addressed or query is None,
                owner_present=False,
                availability_reason="no_tracked_owner",
                terminal_state=("claimed" if state.terminal_claimed else "none"),
            )
        descriptor = context.descriptor
        admission_grant = getattr(context, "admission_grant", None)
        if not self._descriptor_validator.accepts(
            descriptor,
            admission_grant=admission_grant,
        ):
            return CommandFeedbackLifecycleSnapshot(
                state=CommandFeedbackLifecycleSnapshot.UNAVAILABLE,
                query_matched=False,
                query_family_matched=False,
                query_target_matched=False,
                owner_present=True,
                availability_reason="query_mismatch",
                terminal_state="unclaimed",
            )
        resolution = self._resolve_query(
            query,
            descriptor,
            target_item,
            active_command=active_command,
            owner_token=context.owner_token,
            admission_grant=admission_grant,
            terminal_claimed=state.terminal_claimed,
        )
        query_matched = resolution.claimed
        values = dict(
            descriptor=descriptor,
            command_name=descriptor.command_name,
            requested_family=descriptor.requested_family,
            target_item=descriptor.target_item,
            requested_count=descriptor.requested_count,
            result_reason=state.latest_result_reason,
            query_matched=query_matched,
            query_family_matched=resolution.family_matched,
            query_target_matched=resolution.target_matched,
            owner_present=True,
            terminal_state="unclaimed",
        )
        if (
            not query_matched
            or not connected
            or quarantine_active
            or active_command is not context.owner_token
        ):
            return CommandFeedbackLifecycleSnapshot(
                state=CommandFeedbackLifecycleSnapshot.UNAVAILABLE,
                availability_reason=self._availability_reason(
                    query_matched=query_matched,
                    connected=connected,
                    quarantine_active=quarantine_active,
                    owner_matches=active_command is context.owner_token,
                ),
                **values,
            )
        profile = self._evidence_profiles.profile(descriptor.command_name)
        if (
            state.latest_status == "running"
            and profile.accepts_progress(state.latest_result_reason)
        ):
            lifecycle_state = CommandFeedbackLifecycleSnapshot.RUNNING
        elif state.latest_status == "accepted":
            lifecycle_state = CommandFeedbackLifecycleSnapshot.PENDING
        else:
            lifecycle_state = CommandFeedbackLifecycleSnapshot.UNAVAILABLE
        return CommandFeedbackLifecycleSnapshot(
            state=lifecycle_state,
            availability_reason=(
                "evidence_unavailable"
                if lifecycle_state == CommandFeedbackLifecycleSnapshot.UNAVAILABLE
                else ""
            ),
            **values,
        )

    def _resolve_query(
        self,
        query: object,
        descriptor: object,
        target_item: str | None,
        *,
        active_command: object,
        owner_token: object,
        admission_grant: object,
        terminal_claimed: bool,
    ):
        if query is None:
            matched = target_item is None or target_item == descriptor.target_item
            return CommandStatusResolution(
                claimed=matched,
                family_matched=True,
                target_matched=matched,
            )
        try:
            evaluation = self._claim_evaluator.evaluate(
                query=query,
                descriptor=descriptor,
                admission_grant=admission_grant,
                active_command=active_command,
                owner_token=owner_token,
                terminal_claimed=terminal_claimed,
            )
        except ContextualCommandStatusClaimFailure:
            raise
        except Exception as error:
            raise ContextualCommandStatusClaimFailure(
                exception_class=type(error).__name__,
            ) from None
        if type(evaluation) is not ContextualCommandStatusClaimEvaluation:
            raise ContextualCommandStatusClaimFailure(exception_class="TypeError")
        return CommandStatusResolution(
            claimed=evaluation.claimed,
            family_matched=evaluation.family_matched,
            target_matched=evaluation.target_matched,
        )

    @staticmethod
    def _availability_reason(
        *,
        query_matched: bool,
        connected: bool,
        quarantine_active: bool,
        owner_matches: bool,
    ) -> str:
        if not query_matched:
            return "query_mismatch"
        if not connected:
            return "disconnected"
        if quarantine_active:
            return "quarantined"
        if not owner_matches:
            return "stale_owner"
        return "evidence_unavailable"

    @staticmethod
    def _addressed(query: object) -> bool:
        return getattr(query, "addressed", False) is True

__all__ = ("CommandFeedbackStatusCoordinator",)
