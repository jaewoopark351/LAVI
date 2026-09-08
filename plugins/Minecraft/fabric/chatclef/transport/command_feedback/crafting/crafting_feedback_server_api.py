#20260907_kpopmodder: Preserve crafting server calls over the generalized lock facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackServerApi,
)

from .publication import CraftingFeedbackTerminalPublication


class CraftingFeedbackServerApi:
    def __init__(self, **values) -> None:
        values = dict(values)
        terminal_delivery = values.get("terminal_delivery")
        terminal_presenter = values.pop("terminal_presenter", None)
        if terminal_presenter is None:
            terminal_presenter = getattr(
                terminal_delivery,
                "terminal_response_factory",
                None,
            )
        if not callable(terminal_presenter) and not callable(
            getattr(terminal_presenter, "present", None)
        ):
            from .compatibility import CraftingFeedbackLegacyTerminalPresenter

            terminal_presenter = CraftingFeedbackLegacyTerminalPresenter()
        values["terminal_delivery"] = CraftingFeedbackTerminalPublication(
            terminal_presenter=terminal_presenter,
            terminal_delivery=terminal_delivery,
        )
        self._api = CommandFeedbackServerApi(**values)

    def reserve(self, grant: object) -> bool:
        return self._api.reserve(grant)

    def abandon(self, grant: object) -> bool:
        return self._api.abandon(grant)

    def claim_start(self, grant: object, result: object):
        return self._api.claim_start(grant, result)

    def inspect_status(self, target_item: str | None):
        return self._api.inspect_status(target_item=target_item)

    def set_terminal_callback(self, callback) -> None:
        self._api.set_terminal_callback(callback)


__all__ = ("CraftingFeedbackServerApi",)
