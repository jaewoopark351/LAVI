#20260907_kpopmodder: Expose generalized feedback only through the existing command lock.
from __future__ import annotations

from dataclasses import replace

from .publication.command_feedback_publication_acknowledgement import (
    CommandFeedbackPublicationAcknowledgement,
)
from .publication.command_feedback_publication_permit import (
    CommandFeedbackPublicationPermit,
)
from .status.publication.command_status_publication_handoff import (
    CommandStatusPublicationHandoff,
)


class CommandFeedbackServerApi:
    def __init__(
        self,
        *,
        connection_ownership,
        command_lock,
        terminal_listener,
        terminal_delivery,
        status_publication_failure_diagnostic_custody_factory=None,
        status_publication_handoff_failure_observer=None,
    ) -> None:
        self._connection_ownership = connection_ownership
        self._command_lock = command_lock
        self._terminal_listener = terminal_listener
        self._terminal_delivery = terminal_delivery
        self._status_publication_handoff = CommandStatusPublicationHandoff(
            acknowledgement_factory=self._status_acknowledgement,
            publication_failure_callback=self._acknowledge_publication,
            diagnostic_custody_factory=(
                status_publication_failure_diagnostic_custody_factory
            ),
            fallback_failure_observer=(
                status_publication_handoff_failure_observer
            ),
        )

    def reserve(self, grant: object) -> bool:
        with self._command_lock:
            return self._connection_ownership.reserve_command_feedback(grant)

    def abandon(self, grant: object) -> bool:
        with self._command_lock:
            return self._connection_ownership.abandon_command_feedback(grant)

    def claim_start(self, grant: object, result: object):
        with self._command_lock:
            permit = self._connection_ownership.claim_command_feedback_start(
                grant,
                result,
            )
        return self._acknowledgement(permit)

    def inspect_status(self, query: object = None, *, target_item: str | None = None):
        with self._command_lock:
            snapshot, permit = (
                self._connection_ownership
                .inspect_command_feedback_status_for_publication(
                    query=query,
                    target_item=target_item,
                )
            )
        if (
            type(permit) is CommandFeedbackPublicationPermit
            and permit.kind == CommandFeedbackPublicationPermit.STATUS
        ):
            return self._status_publication_handoff.attach(
                query=query,
                snapshot=snapshot,
                permit=permit,
            )
        acknowledgement = self._acknowledgement(permit)
        return snapshot if acknowledgement is None else replace(
            snapshot,
            publication_acknowledgement=acknowledgement,
        )

    def set_terminal_callback(self, callback) -> None:
        self._terminal_listener.set_callback(callback)

    def set_command_lifecycle_terminal_response_callback(self, callback) -> None:
        self.set_terminal_callback(callback)

    def set_command_terminal_response_callback(self, callback) -> None:
        self.set_command_lifecycle_terminal_response_callback(callback)

    def _acknowledgement(
        self,
        permit: object,
        *,
        publication_failure_diagnostic_custody=None,
    ):
        if type(permit) is not CommandFeedbackPublicationPermit:
            return None
        return CommandFeedbackPublicationAcknowledgement(
            permit=permit,
            callback=self._acknowledge_publication,
            coalesced_terminal_selector=self._select_coalesced_terminal,
            publication_failure_diagnostic_custody=(
                publication_failure_diagnostic_custody
            ),
        )

    def _status_acknowledgement(self, permit: object, custody: object):
        return self._acknowledgement(
            permit,
            publication_failure_diagnostic_custody=custody,
        )

    def _select_coalesced_terminal(self, permit: object):
        with self._command_lock:
            return (
                self._connection_ownership
                .select_command_feedback_coalesced_terminal(permit)
            )

    def _acknowledge_publication(
        self,
        permit: CommandFeedbackPublicationPermit,
        published: bool,
    ) -> bool:
        with self._command_lock:
            resolution = (
                self._connection_ownership
                .acknowledge_command_feedback_publication(permit, published)
            )
        terminal = getattr(resolution, "terminal_response", None)
        if terminal is not None:
            self._terminal_delivery.publish(terminal)
        return getattr(resolution, "accepted", False) is True


__all__ = ("CommandFeedbackServerApi",)
