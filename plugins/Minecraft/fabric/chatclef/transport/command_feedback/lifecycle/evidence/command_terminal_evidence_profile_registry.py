#20260907_kpopmodder: Keep exact registered-command evidence coverage closed and reviewable.
from __future__ import annotations

from types import MappingProxyType

from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)

from .command_terminal_evidence_profile import CommandTerminalEvidenceProfile
from ..kind import CommandFeedbackLifecycleKindProfileRegistry


_EVIDENCE = {
    "attack": ("cautious", "cautious_terminal"),
    "auto_deposit_trust": ("cautious", "cautious_terminal"),
    "auto_deposit_trusted_list": ("cautious", "cautious_terminal"),
    "auto_deposit_untrust": ("cautious", "cautious_terminal"),
    "chatclef": ("cautious", "cautious_terminal"),
    "deposit": ("cautious", "cautious_terminal"),
    "deposit_all": ("cautious", "cautious_terminal"),
    "equip": ("cautious", "cautious_terminal"),
    "follow": ("cautious", "cautious_terminal"),
    "food": ("cautious", "cautious_terminal"),
    "gamer": ("cautious", "cautious_terminal"),
    "gamma": ("cautious", "cautious_terminal"),
    "get": ("verified", "get_acquisition"),
    "give": ("cautious", "cautious_terminal"),
    "goto": ("cautious", "cautious_terminal"),
    "hero": ("cautious", "cautious_terminal"),
    "idle": ("cautious", "cautious_terminal"),
    "locate_structure": ("cautious", "cautious_terminal"),
    "meat": ("cautious", "cautious_terminal"),
    "overlay": ("cautious", "cautious_terminal"),
    "reload_settings": ("cautious", "cautious_terminal"),
    "resetmemory": ("cautious", "cautious_terminal"),
    "scan": ("cautious", "cautious_terminal"),
    "stop": ("cautious", "specialized_stop_control"),
    "store_home": ("verified", "store_home_completion"),
    "자동보관등록": ("cautious", "cautious_terminal"),
}


class CommandTerminalEvidenceProfileRegistry:
    def __init__(self, command_registry=None) -> None:
        registry = command_registry or KoreanChatClefCommandRegistry()
        lifecycle_kinds = CommandFeedbackLifecycleKindProfileRegistry(registry)
        profiles = {}
        for command_name in registry.command_names():
            lifecycle_kind = registry.spec(command_name).lifecycle_kind
            rollout_state, evaluator_id = _EVIDENCE[command_name]
            profiles[command_name] = CommandTerminalEvidenceProfile(
                command_name=command_name,
                lifecycle_kind=lifecycle_kind,
                profile_id=f"{command_name}_terminal_evidence_v1",
                rollout_state=rollout_state,
                success_evaluator_id=evaluator_id,
                response_lifecycle_kind=(
                    lifecycle_kinds.profile(command_name).response_lifecycle_kind
                ),
            )
        expected = tuple(registry.command_names())
        if tuple(profiles) != expected or set(_EVIDENCE) != set(expected):
            raise RuntimeError("command evidence profiles must cover the registry exactly")
        self._profiles = MappingProxyType(profiles)

    def command_names(self) -> tuple[str, ...]:
        return tuple(self._profiles)

    def profile(self, command_name: object) -> CommandTerminalEvidenceProfile:
        name = str(command_name or "").strip().lower()
        try:
            return self._profiles[name]
        except KeyError as error:
            raise KeyError(f"unknown command evidence profile: {name}") from error

    def mapping(self):
        return self._profiles


__all__ = ("CommandTerminalEvidenceProfileRegistry",)
