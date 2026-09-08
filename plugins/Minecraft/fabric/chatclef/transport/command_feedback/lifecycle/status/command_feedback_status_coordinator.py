#20260907_kpopmodder: Derive read-only status from one live descriptor and approved evidence.
from __future__ import annotations

from ..state.command_feedback_lifecycle_snapshot import (
    CommandFeedbackLifecycleSnapshot,
)
from .command_status_resolution import CommandStatusResolution
from .command_status_target_resolver import CommandStatusTargetResolver


class CommandFeedbackStatusCoordinator:
    def __init__(self, *, evidence_profiles, target_resolver=None) -> None:
        self._evidence_profiles = evidence_profiles
        self._target_resolver = target_resolver or CommandStatusTargetResolver()

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
            )
        descriptor = context.descriptor
        resolution = self._resolve_query(query, descriptor, target_item)
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
        return CommandFeedbackLifecycleSnapshot(state=lifecycle_state, **values)

    def _resolve_query(
        self,
        query: object,
        descriptor: object,
        target_item: str | None,
    ):
        if query is None:
            matched = target_item is None or target_item == descriptor.target_item
            return CommandStatusResolution(
                claimed=matched,
                family_matched=True,
                target_matched=matched,
            )
        return self._target_resolver.resolve(query, descriptor)

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
