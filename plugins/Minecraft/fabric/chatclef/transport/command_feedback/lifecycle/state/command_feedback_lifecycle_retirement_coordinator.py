#20260907_kpopmodder: Added this module to keep one project class per Python file.
#20260907_kpopmodder: Own lifecycle retirement without introducing another lock.
from __future__ import annotations


class CommandFeedbackLifecycleRetirementCoordinator:
    def __init__(self, *, state, publication_coordinator) -> None:
        self._state = state
        self._publications = publication_coordinator

    def reset_active(self) -> None:
        self._state.reset_active()

    def retire_active(self) -> None:
        context = self._state.context
        if context is not None:
            context.admission_grant.retire()
        self._publications.reset()
        self._state.reset_active()

    def clear_if_owner(self, owner_token: object) -> bool:
        context = self._state.context
        if context is None or context.owner_token is not owner_token:
            return False
        self.retire_active()
        return True

    def clear(self) -> None:
        reservation = self._state.reservation
        if reservation is not None:
            reservation.abandon_if_reserved()
        self._state.reservation = None
        self.retire_active()


__all__ = ("CommandFeedbackLifecycleRetirementCoordinator",)
