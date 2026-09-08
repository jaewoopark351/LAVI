#20260907_kpopmodder: Isolate extension-level crafting feedback delegation.
from __future__ import annotations


class MinecraftFabricChatClefCraftingFeedbackFacade:
    def __init__(self, adapter) -> None:
        self._adapter = adapter

    def reserve(self, grant: object) -> bool:
        return self._adapter.reserve_crafting_feedback(grant)

    def abandon(self, grant: object) -> bool:
        return self._adapter.abandon_crafting_feedback(grant)

    def claim_start(self, grant: object, result: object) -> bool:
        return self._adapter.claim_crafting_feedback_start(grant, result)

    def inspect_status(self, target_item: str | None):
        return self._adapter.inspect_crafting_feedback_status(target_item)

    def set_terminal_response_callback(self, callback) -> None:
        self._adapter.set_crafting_terminal_response_callback(callback)


__all__ = ("MinecraftFabricChatClefCraftingFeedbackFacade",)
