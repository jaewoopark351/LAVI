#20260907_kpopmodder: Carry one accepted command START response to presentation sinks.
from __future__ import annotations

import re
from dataclasses import dataclass

from plugins.Minecraft.fabric.chatclef.presentation.command_lifecycle import (
    CommandLifecyclePresentationDetailLogPolicy,
)


@dataclass(frozen=True, slots=True)
class CommandLifecycleStartResponse:
    text: str
    event_id: str
    command_name: str
    presentation_detail_log: str = ""

    ROUTE_KIND = "command_lifecycle"
    RESPONSE_KIND = "command_start"
    _EVENT_ID = re.compile(r"[0-9a-f]{32}\Z", re.ASCII)
    _COMMAND_NAME = re.compile(r"(?:[a-z][a-z0-9_]*|자동보관등록)\Z", re.ASCII)

    def __post_init__(self) -> None:
        if type(self.text) is not str or not self.text:
            raise ValueError("command start response text must be an exact str")
        if (
            type(self.event_id) is not str
            or self._EVENT_ID.fullmatch(self.event_id) is None
        ):
            raise ValueError("command start response event_id is invalid")
        if (
            type(self.command_name) is not str
            or self._COMMAND_NAME.fullmatch(self.command_name) is None
        ):
            raise ValueError("command start response command_name is invalid")
        CommandLifecyclePresentationDetailLogPolicy.validate(
            self.presentation_detail_log
        )

    @property
    def route_kind(self) -> str:
        return self.ROUTE_KIND

    @property
    def response_kind(self) -> str:
        return self.RESPONSE_KIND


__all__ = ("CommandLifecycleStartResponse",)
