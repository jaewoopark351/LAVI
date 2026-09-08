#20260908_kpopmodder: Prebuild one deeply immutable fail-closed STATUS decision.
from __future__ import annotations

from dataclasses import dataclass, field
from types import MappingProxyType
from typing import ClassVar, Mapping


_EMPTY_MAPPING = MappingProxyType({})


@dataclass(frozen=True, slots=True)
class CommandStatusEmergencyDecision:
    handled: bool = True
    reason: str = "command_status_publication_fail_closed"
    response_text: str = ""
    result: Mapping[str, object] = field(default_factory=lambda: _EMPTY_MAPPING)
    translation: Mapping[str, object] = field(
        default_factory=lambda: _EMPTY_MAPPING
    )
    publish_external_response: bool = False
    response_source: str = "minecraft_chatclef"
    response_emission_capability: object = None
    suppress_response: bool = True
    route_kind: str = "command_status_query"
    response_kind: str = "command_status"
    response_publication_acknowledgement: object = None
    presentation_detail_log: str = ""
    _EMPTY: ClassVar[Mapping[str, object]] = _EMPTY_MAPPING


COMMAND_STATUS_EMERGENCY_DECISION = CommandStatusEmergencyDecision()


__all__ = (
    "COMMAND_STATUS_EMERGENCY_DECISION",
    "CommandStatusEmergencyDecision",
)
