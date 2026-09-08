#20260907_kpopmodder: Added this module to keep one project class per Python file.
#20260907_kpopmodder: Own read-only status inspection and its ordered publication permit.
from __future__ import annotations

from ..publication.command_feedback_publication_permit import (
    CommandFeedbackPublicationPermit,
)
from .busy import CommandBusyStatusInspector


class CommandFeedbackStatusPublicationCoordinator:
    def __init__(
        self,
        *,
        state,
        status_coordinator,
        publication_coordinator,
        busy_status_inspector=None,
    ) -> None:
        self._state = state
        self._statuses = status_coordinator
        self._publications = publication_coordinator
        self._busy = busy_status_inspector or CommandBusyStatusInspector(
            state=state,
            status_coordinator=status_coordinator,
            publication_coordinator=publication_coordinator,
        )

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
            state=self._state,
            active_command=active_command,
            connected=connected,
            quarantine_active=quarantine_active,
            query=query,
            target_item=target_item,
        )

    def inspect_for_publication(self, **values):
        snapshot = self.inspect(**values)
        if (
            self._state.context is None
            or self._state.terminal_claimed
            or snapshot.query_matched is not True
        ):
            return snapshot, None
        permit = self._publications.issue(CommandFeedbackPublicationPermit.STATUS)
        return snapshot, permit

    def inspect_busy_for_publication(self, **values):
        return self._busy.inspect_for_publication(**values)


__all__ = ("CommandFeedbackStatusPublicationCoordinator",)
