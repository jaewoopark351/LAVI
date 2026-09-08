#20260907_kpopmodder: Added this module to keep one project class per Python file.
#20260907_kpopmodder: Own correlated nonterminal result-state projection under the external lock.
from __future__ import annotations

from typing import Mapping


class CommandFeedbackResultStateCoordinator:
    DISPATCH_STARTED = "dispatch_started"

    def __init__(self, *, state) -> None:
        self._state = state

    def matches_result(
        self,
        *,
        websocket: object,
        owner_token: object,
        session_id: object,
        generation: object,
        request_id: object,
        command_message_id: object,
    ) -> bool:
        context = self._state.context
        return bool(
            context is not None
            and not self._state.terminal_claimed
            and context.websocket is websocket
            and context.owner_token is owner_token
            and context.session_id == session_id
            and context.generation == generation
            and context.request_id == request_id
            and context.command_message_id == command_message_id
        )

    def record_nonterminal(
        self,
        *,
        status: str,
        result_reason: str,
        evidence_sequence: object = None,
    ) -> bool:
        if self._state.context is None or self._state.terminal_claimed:
            return False
        if type(evidence_sequence) is not int or evidence_sequence < 1:
            return False
        previous = self._state.latest_evidence_sequence
        if previous is not None and evidence_sequence <= previous:
            return False
        self._state.latest_status = str(status or "").strip().lower()
        self._state.latest_result_reason = str(result_reason or "").strip()
        self._state.latest_evidence_sequence = evidence_sequence
        if (
            self._state.latest_status == "running"
            and self._state.latest_result_reason == self.DISPATCH_STARTED
        ):
            self._state.dispatch_started_observed = True
        return True

    def dispatch_started_observed(self, owner_token: object) -> bool:
        context = self._state.context
        return bool(
            context is not None
            and context.owner_token is owner_token
            and not self._state.terminal_claimed
            and self._state.dispatch_started_observed
        )

    def matches_before_snapshot(self, owner_token: object, snapshot: object) -> bool:
        context = self._state.context
        if context is None or context.owner_token is not owner_token:
            return False
        if not isinstance(snapshot, Mapping):
            return False
        return bool(
            snapshot.get("active_session_id") == context.session_id
            and snapshot.get("active_generation") == context.generation
            and snapshot.get("active_request_id") == context.request_id
            and snapshot.get("active_command_message_id")
            == context.command_message_id
            and snapshot.get("active_command") == context.command
            and snapshot.get("active_command_source") == context.command_source
            and snapshot.get("active_started_at_ms") == context.accepted_at_ms
        )


__all__ = ("CommandFeedbackResultStateCoordinator",)
