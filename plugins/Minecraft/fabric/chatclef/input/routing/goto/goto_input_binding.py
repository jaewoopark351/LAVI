#20260913_kpopmodder: Carry automatic-input scope and immutable original XYZ explicitly.
from __future__ import annotations

from dataclasses import dataclass

from plugins.Minecraft.fabric.chatclef.intent.navigation.goto import GotoParseResult


@dataclass(frozen=True, slots=True)
class GotoInputBinding:
    automatic_input: bool
    parse_result: GotoParseResult

    def __post_init__(self) -> None:
        if type(self.automatic_input) is not bool:
            raise TypeError("goto_automatic_input_must_be_bool")
        if type(self.parse_result) is not GotoParseResult:
            raise TypeError("goto_parse_result_required")

    @staticmethod
    def applies_to(event: object) -> bool:
        # Scope selects a required check; these strings never establish trust.
        return getattr(event, "source", None) in (
            "lavi_chat_ui",
            "voice_input_final",
        )
