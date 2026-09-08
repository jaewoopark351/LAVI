#20260907_kpopmodder: Added this module to keep one project class per Python file.
#20260907_kpopmodder: Own publication acknowledgement and initial coalesced selection.
from __future__ import annotations

from .command_feedback_publication_resolution import (
    CommandFeedbackPublicationResolution,
)


class CommandFeedbackPublicationLifecycleCoordinator:
    def __init__(self, *, state, publication_coordinator, retirement) -> None:
        self._state = state
        self._publications = publication_coordinator
        self._retirement = retirement

    def acknowledge(
        self,
        permit: object,
        published: bool,
    ) -> CommandFeedbackPublicationResolution:
        resolution = self._publications.acknowledge(permit, published)
        if resolution.retire_lifecycle:
            self._retirement.reset_active()
        return resolution

    def select_coalesced_terminal(self, permit: object):
        return self._publications.select_coalesced_terminal(
            self._state.lifecycle_token,
            permit,
        )


__all__ = ("CommandFeedbackPublicationLifecycleCoordinator",)
