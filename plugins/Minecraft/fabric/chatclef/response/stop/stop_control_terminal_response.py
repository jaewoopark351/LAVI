#20260905_kpopmodder: Carry verified STOP terminal text with bounded ingress identity.
from __future__ import annotations

import re
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class StopControlTerminalResponse:
    text: str
    event_id: str

    ROUTE_KIND = "stop_control"
    RESPONSE_KIND = "stop_terminal"
    _EVENT_ID = re.compile(r"[0-9a-f]{32}\Z", re.ASCII)

    def __post_init__(self) -> None:
        if type(self.text) is not str or not self.text:
            raise ValueError("STOP terminal response text must be an exact str")
        if (
            type(self.event_id) is not str
            or self._EVENT_ID.fullmatch(self.event_id) is None
        ):
            raise ValueError("STOP terminal response event_id is invalid")

    @property
    def route_kind(self) -> str:
        return self.ROUTE_KIND

    @property
    def response_kind(self) -> str:
        return self.RESPONSE_KIND


__all__ = ("StopControlTerminalResponse",)
