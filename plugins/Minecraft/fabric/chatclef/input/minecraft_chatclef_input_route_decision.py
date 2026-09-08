#20260803_kpopmodder: Keep Minecraft input route decisions explicit and testable.
#20260905_kpopmodder: Carry typed route and response ownership metadata.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from plugins.Minecraft.fabric.chatclef.presentation.command_lifecycle import (
    CommandLifecyclePresentationDetailLogPolicy,
)


@dataclass(frozen=True)
class MinecraftChatClefInputRouteDecision:
    handled: bool
    reason: str
    response_text: str = ""
    result: dict[str, Any] = field(default_factory=dict)
    translation: dict[str, Any] = field(default_factory=dict)
    publish_external_response: bool = False
    response_source: str = "minecraft_chatclef"
    response_emission_capability: Any = None
    suppress_response: bool = False
    route_kind: str = "minecraft_chatclef"
    response_kind: str = "immediate"
    response_publication_acknowledgement: Any = None
    presentation_detail_log: str = ""

    def __post_init__(self) -> None:
        CommandLifecyclePresentationDetailLogPolicy.validate(
            self.presentation_detail_log
        )

    @classmethod
    def not_handled(cls, reason: str) -> "MinecraftChatClefInputRouteDecision":
        return cls(handled=False, reason=str(reason or "not_handled"))

    @classmethod
    def handled_result(
        cls,
        *,
        reason: str,
        response_text: str,
        result: dict[str, Any] | None = None,
        translation: dict[str, Any] | None = None,
        publish_external_response: bool = False,
        response_source: str = "minecraft_chatclef",
        response_emission_capability: Any = None,
        suppress_response: bool = False,
        route_kind: str = "minecraft_chatclef",
        response_kind: str = "immediate",
        response_publication_acknowledgement: Any = None,
        presentation_detail_log: str = "",
    ) -> "MinecraftChatClefInputRouteDecision":
        return cls(
            handled=True,
            reason=str(reason or "handled"),
            response_text=str(response_text or ""),
            result=dict(result or {}),
            translation=dict(translation or {}),
            publish_external_response=publish_external_response,
            response_source=str(response_source or "minecraft_chatclef"),
            response_emission_capability=response_emission_capability,
            suppress_response=suppress_response,
            route_kind=str(route_kind or "minecraft_chatclef"),
            response_kind=str(response_kind or "immediate"),
            response_publication_acknowledgement=(
                response_publication_acknowledgement
            ),
            presentation_detail_log=(
                CommandLifecyclePresentationDetailLogPolicy.validate(
                    presentation_detail_log
                )
            ),
        )
