#20260907_kpopmodder: Preserve crafting tracker APIs over the split lifecycle core.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackLifecycleFacade,
)


class CraftingFeedbackTracker:
    DISPATCH_STARTED = CommandFeedbackLifecycleFacade.DISPATCH_STARTED

    def __init__(self, *, lifecycle_facade=None) -> None:
        self._lifecycle = lifecycle_facade or CommandFeedbackLifecycleFacade()

    @property
    def context(self):
        return self._lifecycle.context

    def reserve(self, grant: object) -> bool:
        return self._lifecycle.reserve(grant)

    def bind_reserved(self, active_command: object, metadata: object) -> bool:
        return self._lifecycle.bind_reserved(active_command, metadata)

    def abandon_reservation(self, grant: object) -> bool:
        return self._lifecycle.abandon_reservation(grant)

    def claim_start(self, grant: object, result: object):
        return self._lifecycle.claim_start(grant, result)

    def matches_result(self, **values) -> bool:
        return self._lifecycle.matches_result(**values)

    def record_nonterminal(self, **values) -> bool:
        return self._lifecycle.record_nonterminal(**values)

    def dispatch_started_observed(self, owner_token: object) -> bool:
        return self._lifecycle.dispatch_started_observed(owner_token)

    def inspect(self, **values):
        return self._lifecycle.inspect(**values)

    def inspect_for_publication(self, **values):
        return self._lifecycle.inspect_for_publication(**values)

    def claim_terminal(self, owner_token: object):
        return self._lifecycle.claim_terminal(owner_token)

    def specialized_stop_owns_terminal(self, **values) -> bool:
        return self._lifecycle.specialized_stop_owns_terminal(**values)

    def discard_terminal(self, owner_token: object) -> bool:
        return self._lifecycle.discard_terminal(owner_token)

    def stage_terminal(self, owner_token: object, response: object):
        return self._lifecycle.stage_terminal(owner_token, response)

    def acknowledge_publication(self, permit: object, published: bool):
        return self._lifecycle.acknowledge_publication(permit, published)

    def select_coalesced_terminal(self, permit: object):
        return self._lifecycle.select_coalesced_terminal(permit)

    def matches_before_snapshot(self, owner_token: object, snapshot: object) -> bool:
        return self._lifecycle.matches_before_snapshot(owner_token, snapshot)

    def clear_if_owner(self, owner_token: object) -> bool:
        return self._lifecycle.clear_if_owner(owner_token)

    def clear(self) -> None:
        self._lifecycle.clear()


__all__ = ("CraftingFeedbackTracker",)
