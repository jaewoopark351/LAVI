#20260909_kpopmodder: Revalidate one observed busy owner atomically before issuing STATUS custody.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_active_command import (
    FabricChatClefActiveCommand,
)

from ...admission.command_feedback_admission_grant import (
    CommandFeedbackAdmissionGrant,
)
from ...descriptor.command_feedback_descriptor import CommandFeedbackDescriptor
from ...publication.command_feedback_publication_permit import (
    CommandFeedbackPublicationPermit,
)
from ...state.command_feedback_context import CommandFeedbackContext
from ...state.command_feedback_lifecycle_snapshot import (
    CommandFeedbackLifecycleSnapshot,
)
from ..claim.descriptor_validation.command_status_descriptor_eligibility_validator import (
    CommandStatusDescriptorEligibilityValidator,
)
from .command_busy_observed_identity import CommandBusyObservedIdentity


class CommandBusyStatusInspector:
    def __init__(
        self,
        *,
        state,
        status_coordinator,
        publication_coordinator,
        descriptor_validator=None,
    ) -> None:
        self._state = state
        self._statuses = status_coordinator
        self._publications = publication_coordinator
        self._descriptor_validator = (
            descriptor_validator
            if descriptor_validator is not None
            else CommandStatusDescriptorEligibilityValidator()
        )

    def inspect_for_publication(
        self,
        *,
        observed_identity: object,
        active_websocket: object,
        active_session_id: object,
        active_generation: object,
        active_command: object,
        connected: object,
        quarantine_active: object,
    ):
        if type(observed_identity) is not CommandBusyObservedIdentity:
            return None, None
        try:
            context = self._state.context
            if not self._matches_live_owner(
                observed_identity=observed_identity,
                context=context,
                active_websocket=active_websocket,
                active_session_id=active_session_id,
                active_generation=active_generation,
                active_command=active_command,
                connected=connected,
                quarantine_active=quarantine_active,
            ):
                return None, None
            descriptor = context.descriptor
            admission_grant = context.admission_grant
            if not self._descriptor_validator.accepts(
                descriptor,
                admission_grant=admission_grant,
            ):
                return None, None
            snapshot = self._statuses.inspect(
                state=self._state,
                active_command=active_command,
                connected=connected,
                quarantine_active=quarantine_active,
                query=None,
                target_item=None,
            )
            if not self._publishable(snapshot, descriptor):
                return snapshot, None
        except Exception:
            return None, None

        permit = self._publications.issue(CommandFeedbackPublicationPermit.STATUS)
        return snapshot, permit

    def _matches_live_owner(
        self,
        *,
        observed_identity: CommandBusyObservedIdentity,
        context: object,
        active_websocket: object,
        active_session_id: object,
        active_generation: object,
        active_command: object,
        connected: object,
        quarantine_active: object,
    ) -> bool:
        if (
            connected is not True
            or quarantine_active is not False
            or active_websocket is None
            or type(context) is not CommandFeedbackContext
            or type(active_command) is not FabricChatClefActiveCommand
            or self._state.terminal_claimed is not False
            or self._state.lifecycle_token is None
            or self._state.reservation is not None
            or not self._valid_identity_values(
                session_id=active_session_id,
                generation=active_generation,
                request_id=active_command.request_id,
                command_message_id=active_command.command_message_id,
            )
            or not self._valid_identity_values(
                session_id=active_command.session_id,
                generation=active_command.generation,
                request_id=active_command.request_id,
                command_message_id=active_command.command_message_id,
            )
            or not self._valid_identity_values(
                session_id=context.session_id,
                generation=context.generation,
                request_id=context.request_id,
                command_message_id=context.command_message_id,
            )
        ):
            return False
        descriptor = context.descriptor
        admission_grant = context.admission_grant
        start_permit = self._state.start_publication_permit
        if (
            type(descriptor) is not CommandFeedbackDescriptor
            or type(admission_grant) is not CommandFeedbackAdmissionGrant
            or admission_grant.descriptor is not descriptor
            or self._state.lifecycle_grant is not admission_grant
            or type(start_permit) is not CommandFeedbackPublicationPermit
            or start_permit.kind != CommandFeedbackPublicationPermit.START
            or start_permit.lifecycle_token is not self._state.lifecycle_token
            or context.owner_token is not active_command
            or context.websocket is not active_websocket
            or active_command.websocket is not active_websocket
            or active_command.command != descriptor.command
            or active_command.source != descriptor.command_source
        ):
            return False
        return bool(
            active_session_id == observed_identity.active_session_id
            and active_generation == observed_identity.active_generation
            and active_command.session_id == observed_identity.active_session_id
            and active_command.generation == observed_identity.active_generation
            and active_command.request_id == observed_identity.active_request_id
            and active_command.command_message_id
            == observed_identity.active_command_message_id
            and context.session_id == observed_identity.active_session_id
            and context.generation == observed_identity.active_generation
            and context.request_id == observed_identity.active_request_id
            and context.command_message_id
            == observed_identity.active_command_message_id
        )

    @staticmethod
    def _valid_identity_values(
        *,
        session_id: object,
        generation: object,
        request_id: object,
        command_message_id: object,
    ) -> bool:
        return bool(
            type(generation) is int
            and 0 < generation <= CommandBusyObservedIdentity.MAX_GENERATION
            and all(
                type(value) is str
                and bool(value)
                and value == value.strip()
                and len(value) <= CommandBusyObservedIdentity.MAX_TEXT_LENGTH
                for value in (session_id, request_id, command_message_id)
            )
        )

    @staticmethod
    def _publishable(snapshot: object, descriptor: object) -> bool:
        return bool(
            type(snapshot) is CommandFeedbackLifecycleSnapshot
            and snapshot.state
            in {
                CommandFeedbackLifecycleSnapshot.RUNNING,
                CommandFeedbackLifecycleSnapshot.PENDING,
            }
            and snapshot.descriptor is descriptor
            and snapshot.query_matched is True
            and snapshot.query_family_matched is True
            and snapshot.query_target_matched is True
            and snapshot.owner_present is True
            and snapshot.availability_reason == ""
            and snapshot.terminal_state == "unclaimed"
        )


__all__ = ("CommandBusyStatusInspector",)
