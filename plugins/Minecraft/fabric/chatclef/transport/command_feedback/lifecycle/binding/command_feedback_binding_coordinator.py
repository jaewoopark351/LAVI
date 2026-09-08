#20260907_kpopmodder: Added this module to keep one project class per Python file.
#20260907_kpopmodder: Own reservation-to-active-context binding as one atomic lifecycle stage.
from __future__ import annotations

from typing import Mapping

from ..admission.command_feedback_admission_grant import CommandFeedbackAdmissionGrant
from ..state.command_feedback_context import CommandFeedbackContext


class CommandFeedbackBindingCoordinator:
    def __init__(self, *, state, publication_coordinator, retirement) -> None:
        self._state = state
        self._publications = publication_coordinator
        self._retirement = retirement

    @property
    def context(self) -> CommandFeedbackContext | None:
        return None if self._state.terminal_claimed else self._state.context

    def reserve(self, grant: object) -> bool:
        if type(grant) is not CommandFeedbackAdmissionGrant:
            return False
        if (
            self._state.reservation is not None
            or self._state.context is not None
            or self._state.lifecycle_token is not None
        ):
            return False
        if not grant.reserve():
            return False
        self._state.reservation = grant
        return True

    def bind_reserved(self, active_command: object, metadata: object) -> bool:
        grant = self._state.reservation
        if grant is None:
            return False
        if not isinstance(metadata, Mapping):
            self._retire_reservation(grant)
            return False
        input_event = metadata.get("input_event")
        descriptor = grant.descriptor
        if (
            not isinstance(input_event, Mapping)
            or getattr(active_command, "command", None) != descriptor.command
            or getattr(active_command, "source", None) != descriptor.command_source
            or input_event.get("event_id") != descriptor.event_id
            or input_event.get("source") != descriptor.input_source
            or input_event.get("provider_id") != descriptor.provider_id
            or input_event.get("event_kind") != descriptor.event_kind
            or input_event.get("final") is not True
        ):
            self._retire_reservation(grant)
            return False
        if not grant.bind():
            self._retire_reservation(grant)
            return False
        self._state.reservation = None
        context = CommandFeedbackContext(
            websocket=getattr(active_command, "websocket"),
            owner_token=active_command,
            admission_grant=grant,
            descriptor=descriptor,
            session_id=getattr(active_command, "session_id"),
            generation=getattr(active_command, "generation"),
            request_id=getattr(active_command, "request_id"),
            command_message_id=getattr(active_command, "command_message_id"),
            accepted_at_ms=getattr(active_command, "started_at_ms"),
        )
        self._state.context = context
        self._state.latest_status = "accepted"
        self._state.latest_result_reason = ""
        self._state.latest_evidence_sequence = None
        self._state.dispatch_started_observed = False
        self._state.start_claimed = False
        self._state.terminal_claimed = False
        self._state.lifecycle_token = object()
        self._state.lifecycle_grant = grant
        self._state.start_publication_permit = self._publications.begin(
            self._state.lifecycle_token,
            descriptor.response_lifecycle_kind,
        )
        return True

    def abandon_reservation(self, grant: object) -> bool:
        if self._state.reservation is not grant:
            if self._state.lifecycle_grant is grant and not self._state.start_claimed:
                self._retirement.retire_active()
                return True
            return False
        self._state.reservation = None
        grant.abandon_if_reserved()
        return True

    def claim_start(self, grant: object, result: object):
        context = self._state.context
        if (
            context is None
            or context.admission_grant is not grant
            or self._state.start_claimed
            or not self._accepted_submission_matches(context, result)
        ):
            return None
        self._state.start_claimed = True
        return self._state.start_publication_permit

    def _retire_reservation(self, grant: CommandFeedbackAdmissionGrant) -> None:
        if self._state.reservation is grant:
            self._state.reservation = None
        grant.abandon_if_reserved()

    @staticmethod
    def _accepted_submission_matches(
        context: CommandFeedbackContext,
        result: object,
    ) -> bool:
        if not isinstance(result, Mapping) or result.get("ok") is not True:
            return False
        status = result.get("status")
        if not isinstance(status, Mapping):
            return False
        data = status.get("data")
        return bool(
            isinstance(data, Mapping)
            and status.get("ok") is True
            and status.get("status") == "accepted"
            and status.get("request_id") == context.request_id
            and data.get("session_id") == context.session_id
            and type(data.get("connection_generation")) is int
            and data.get("connection_generation") == context.generation
            and data.get("command_message_id") == context.command_message_id
        )


__all__ = ("CommandFeedbackBindingCoordinator",)
