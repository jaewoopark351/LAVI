#20260907_kpopmodder: Lock all registered raw names to an explicit source-reviewed grammar.
from __future__ import annotations

from types import MappingProxyType

from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)

from .command_feedback_raw_form_profile import CommandFeedbackRawFormProfile


_GRAMMARS = {
    "attack": "attack_target_optional_count",
    "auto_deposit_trust": "automatic_trust",
    "auto_deposit_trusted_list": "no_arguments",
    "auto_deposit_untrust": "automatic_untrust",
    "chatclef": "on_off_state",
    "deposit": "optional_item_list",
    "deposit_all": "optional_item_list",
    "equip": "required_equipment_list",
    "follow": "optional_player",
    "food": "positive_integer",
    "gamer": "no_arguments",
    "gamma": "optional_finite_double",
    "get": "required_item_list",
    "give": "give_item",
    "goto": "goto_target",
    "hero": "no_arguments",
    "idle": "no_arguments",
    "locate_structure": "structure",
    "meat": "positive_integer",
    "overlay": "on_off_state",
    "reload_settings": "no_arguments",
    "resetmemory": "no_arguments",
    "scan": "optional_block",
    "stop": "no_arguments",
    "store_home": "no_arguments",
    "자동보관등록": "korean_automatic_trust",
}


class CommandFeedbackRawFormProfileRegistry:
    def __init__(self, command_registry=None) -> None:
        commands = command_registry or KoreanChatClefCommandRegistry()
        expected = tuple(commands.command_names())
        if tuple(_GRAMMARS) != expected:
            raise RuntimeError("raw form grammars must cover the registry exactly")
        self._profiles = MappingProxyType(
            {
                command_name: CommandFeedbackRawFormProfile(
                    command_name=command_name,
                    grammar_id=_GRAMMARS[command_name],
                )
                for command_name in expected
            }
        )

    def command_names(self) -> tuple[str, ...]:
        return tuple(self._profiles)

    def profile(self, command_name: object) -> CommandFeedbackRawFormProfile:
        name = str(command_name or "").strip()
        try:
            return self._profiles[name]
        except KeyError as error:
            raise KeyError(f"unknown raw command form profile: {name}") from error

    def mapping(self):
        return self._profiles


__all__ = ("CommandFeedbackRawFormProfileRegistry",)
