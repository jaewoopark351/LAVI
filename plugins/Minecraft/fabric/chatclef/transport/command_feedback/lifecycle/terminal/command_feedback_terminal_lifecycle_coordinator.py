#20260907_kpopmodder: Added this module to keep one project class per Python file.
#20260907_kpopmodder: Own terminal claim, discard, and staged-publication state transitions.
from __future__ import annotations


class CommandFeedbackTerminalLifecycleCoordinator:
    def __init__(
        self,
        *,
        state,
        terminal_claim_coordinator,
        publication_coordinator,
        retirement,
    ) -> None:
        self._state = state
        self._claims = terminal_claim_coordinator
        self._publications = publication_coordinator
        self._retirement = retirement

    def claim(self, owner_token: object):
        return self._claims.claim(self._state, owner_token)

    def specialized_stop_owns_terminal(
        self,
        *,
        status: object,
        result_reason: object,
    ) -> bool:
        return self._claims.specialized_stop_owns_terminal(
            status=status,
            result_reason=result_reason,
        )

    def discard(self, owner_token: object) -> bool:
        context = self._state.context
        if context is None or context.owner_token is not owner_token:
            return False
        self._retirement.retire_active()
        return True

    def stage(self, owner_token: object, response: object):
        context = self._state.context
        if (
            not self._state.terminal_claimed
            or context is None
            or context.owner_token is not owner_token
        ):
            return None
        terminal, retire = self._publications.stage_terminal(
            self._state.lifecycle_token,
            response,
        )
        if retire:
            self._retirement.reset_active()
        return terminal


__all__ = ("CommandFeedbackTerminalLifecycleCoordinator",)
