#20260907_kpopmodder: Carry one verified crafting terminal response to output/TTS.
from __future__ import annotations

import re
from dataclasses import dataclass

from plugins.Minecraft.fabric.chatclef.presentation.command_lifecycle import (
    CommandLifecyclePresentationDetailLogPolicy,
)


@dataclass(frozen=True, slots=True)
class CraftingLifecycleTerminalResponse:
    text: str
    event_id: str
    presentation_detail_log: str = ""

    ROUTE_KIND = "crafting_lifecycle"
    RESPONSE_KIND = "crafting_terminal"
    _EVENT_ID = re.compile(r"[0-9a-f]{32}\Z", re.ASCII)

    def __post_init__(self) -> None:
        if type(self.text) is not str or not self.text:
            raise ValueError("crafting terminal response text must be an exact str")
        if (
            type(self.event_id) is not str
            or self._EVENT_ID.fullmatch(self.event_id) is None
        ):
            raise ValueError("crafting terminal response event_id is invalid")
        CommandLifecyclePresentationDetailLogPolicy.validate(
            self.presentation_detail_log
        )

    @property
    def route_kind(self) -> str:
        return self.ROUTE_KIND

    @property
    def response_kind(self) -> str:
        return self.RESPONSE_KIND


__all__ = ("CraftingLifecycleTerminalResponse",)
