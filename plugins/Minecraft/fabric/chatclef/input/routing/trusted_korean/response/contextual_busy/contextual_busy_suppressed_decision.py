#20260905_kpopmodder: Keep trusted Korean route responsibilities split by module.
#20260909_kpopmodder: Prebuild one deeply immutable contextual-busy failure result.
from __future__ import annotations

from dataclasses import dataclass, field
from types import MappingProxyType
from typing import ClassVar, Mapping


_EMPTY_MAPPING = MappingProxyType({})
_BUSY_RESULT = MappingProxyType({"ok": False, "error": "active_command"})


@dataclass(frozen=True, slots=True)
class ContextualBusySuppressedDecision:
    handled: bool = field(default=True, init=False)
    reason: str = field(default="minecraft_command_busy", init=False)
    response_text: str = field(default="", init=False)
    result: Mapping[str, object] = field(
        default_factory=lambda: _BUSY_RESULT,
        init=False,
    )
    translation: Mapping[str, object] = field(
        default_factory=lambda: _EMPTY_MAPPING,
        init=False,
    )
    publish_external_response: bool = field(default=False, init=False)
    response_source: str = field(default="minecraft_chatclef", init=False)
    response_emission_capability: object = field(default=None, init=False)
    suppress_response: bool = field(default=True, init=False)
    route_kind: str = field(default="command_busy_current_work", init=False)
    response_kind: str = field(default="command_status", init=False)
    response_publication_acknowledgement: object = field(
        default=None,
        init=False,
    )
    presentation_detail_log: str = field(default="", init=False)
    _EMPTY: ClassVar[Mapping[str, object]] = _EMPTY_MAPPING
    _RESULT: ClassVar[Mapping[str, object]] = _BUSY_RESULT


CONTEXTUAL_BUSY_SUPPRESSED_DECISION = ContextualBusySuppressedDecision()


__all__ = (
    "CONTEXTUAL_BUSY_SUPPRESSED_DECISION",
    "ContextualBusySuppressedDecision",
)
