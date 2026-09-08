#20260907_kpopmodder: Own terminal CAS and specialized STOP speech arbitration.
from __future__ import annotations


class CommandTerminalClaimCoordinator:
    USER_STOP_REQUESTED = "user_stop_requested"

    def claim(self, state, owner_token: object):
        context = state.context
        if (
            context is None
            or context.owner_token is not owner_token
            or state.terminal_claimed
        ):
            return None
        state.terminal_claimed = True
        context.admission_grant.retire()
        return context

    def specialized_stop_owns_terminal(
        self,
        *,
        status: object,
        result_reason: object,
    ) -> bool:
        #20260907_kpopmodder: A status/reason pair cannot prove a live STOP
        # owner. Preserve the compatibility method, but fail closed; production
        # arbitration uses CommandStopTerminalArbitrator with the exact tracker.
        del status, result_reason
        return False


__all__ = ("CommandTerminalClaimCoordinator",)
