#20260907_kpopmodder: Preserve one ordinary lifecycle truth behind focused collaborators.
from __future__ import annotations

from .binding import CommandFeedbackBindingCoordinator
from .evidence.command_terminal_evidence_profile_registry import (
    CommandTerminalEvidenceProfileRegistry,
)
from .publication import (
    CommandFeedbackPublicationCoordinator,
    CommandFeedbackPublicationLifecycleCoordinator,
    CommandFeedbackPublicationResolution,
)
from .result import CommandFeedbackResultStateCoordinator
from .state import (
    CommandFeedbackContext,
    CommandFeedbackLifecycleRetirementCoordinator,
    CommandFeedbackLifecycleState,
)
from .status import (
    CommandFeedbackStatusCoordinator,
    CommandFeedbackStatusPublicationCoordinator,
)
from .terminal import (
    CommandFeedbackAcceptedSubmissionTerminalCoordinator,
    CommandFeedbackTerminalLifecycleCoordinator,
    CommandTerminalClaimCoordinator,
)


class CommandFeedbackLifecycleFacade:
    DISPATCH_STARTED = CommandFeedbackResultStateCoordinator.DISPATCH_STARTED

    def __init__(
        self,
        *,
        state=None,
        publication_coordinator=None,
        status_coordinator=None,
        terminal_claim_coordinator=None,
        evidence_profiles=None,
        accepted_submission_terminal_coordinator=None,
    ) -> None:
        profiles = evidence_profiles or CommandTerminalEvidenceProfileRegistry()
        lifecycle_state = state or CommandFeedbackLifecycleState()
        publications = (
            publication_coordinator or CommandFeedbackPublicationCoordinator()
        )
        retirement = CommandFeedbackLifecycleRetirementCoordinator(
            state=lifecycle_state,
            publication_coordinator=publications,
        )
        self._binding = CommandFeedbackBindingCoordinator(
            state=lifecycle_state,
            publication_coordinator=publications,
            retirement=retirement,
        )
        self._results = CommandFeedbackResultStateCoordinator(state=lifecycle_state)
        self._statuses = CommandFeedbackStatusPublicationCoordinator(
            state=lifecycle_state,
            status_coordinator=(
                status_coordinator
                or CommandFeedbackStatusCoordinator(evidence_profiles=profiles)
            ),
            publication_coordinator=publications,
        )
        self._terminals = CommandFeedbackTerminalLifecycleCoordinator(
            state=lifecycle_state,
            terminal_claim_coordinator=(
                terminal_claim_coordinator or CommandTerminalClaimCoordinator()
            ),
            publication_coordinator=publications,
            retirement=retirement,
        )
        self._publication = CommandFeedbackPublicationLifecycleCoordinator(
            state=lifecycle_state,
            publication_coordinator=publications,
            retirement=retirement,
        )
        self._accepted_submission_terminals = (
            accepted_submission_terminal_coordinator
            or CommandFeedbackAcceptedSubmissionTerminalCoordinator()
        )
        self._retirement = retirement

    @property
    def context(self) -> CommandFeedbackContext | None:
        return self._binding.context

    def reserve(self, grant: object) -> bool:
        return self._binding.reserve(grant)

    def bind_reserved(self, active_command: object, metadata: object) -> bool:
        return self._binding.bind_reserved(active_command, metadata)

    def abandon_reservation(self, grant: object) -> bool:
        return self._binding.abandon_reservation(grant)

    def claim_start(self, grant: object, result: object):
        context = self._binding.context
        permit = self._binding.claim_start(grant, result)
        return self._accepted_submission_terminals.stage_if_required(
            context=context,
            start_permit=permit,
            terminal_lifecycle=self._terminals,
        )

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
        return self._results.matches_result(
            websocket=websocket,
            owner_token=owner_token,
            session_id=session_id,
            generation=generation,
            request_id=request_id,
            command_message_id=command_message_id,
        )

    def record_nonterminal(
        self,
        *,
        status: str,
        result_reason: str,
        evidence_sequence: object = None,
    ) -> bool:
        return self._results.record_nonterminal(
            status=status,
            result_reason=result_reason,
            evidence_sequence=evidence_sequence,
        )

    def dispatch_started_observed(self, owner_token: object) -> bool:
        return self._results.dispatch_started_observed(owner_token)

    def inspect(
        self,
        *,
        active_command: object,
        connected: bool,
        quarantine_active: bool,
        target_item: str | None = None,
        query: object = None,
    ):
        return self._statuses.inspect(
            active_command=active_command,
            connected=connected,
            quarantine_active=quarantine_active,
            query=query,
            target_item=target_item,
        )

    def inspect_for_publication(self, **values):
        return self._statuses.inspect_for_publication(**values)

    def inspect_busy_for_publication(self, **values):
        return self._statuses.inspect_busy_for_publication(**values)

    def claim_terminal(self, owner_token: object):
        return self._terminals.claim(owner_token)

    def specialized_stop_owns_terminal(
        self,
        *,
        status: object,
        result_reason: object,
    ) -> bool:
        return self._terminals.specialized_stop_owns_terminal(
            status=status,
            result_reason=result_reason,
        )

    def discard_terminal(self, owner_token: object) -> bool:
        return self._terminals.discard(owner_token)

    def stage_terminal(self, owner_token: object, response: object):
        return self._terminals.stage(owner_token, response)

    def acknowledge_publication(
        self,
        permit: object,
        published: bool,
    ) -> CommandFeedbackPublicationResolution:
        return self._publication.acknowledge(permit, published)

    def select_coalesced_terminal(self, permit: object):
        return self._publication.select_coalesced_terminal(permit)

    def matches_before_snapshot(
        self,
        owner_token: object,
        snapshot: object,
    ) -> bool:
        return self._results.matches_before_snapshot(owner_token, snapshot)

    def clear_if_owner(self, owner_token: object) -> bool:
        return self._retirement.clear_if_owner(owner_token)

    def clear(self) -> None:
        self._retirement.clear()


__all__ = ("CommandFeedbackLifecycleFacade",)
