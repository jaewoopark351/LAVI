#20260914_kpopmodder: Add FIND to the existing command contract without changing other command ownership.
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
    "auto_deposit_trust": ("verified", "instant_command"),
    "auto_deposit_trusted_list": ("verified", "instant_command"),
    "auto_deposit_untrust": ("verified", "instant_command"),
    "chatclef": ("verified", "instant_command"),
    "deposit": ("cautious", "cautious_terminal"),
    "deposit_all": ("cautious", "cautious_terminal"),
    #20260915_kpopmodder: Slot evidence is request-bound; legacy results remain unverified.
    "equip": ("verified", "equip_slots"),
    "find": ("verified", "find_terminal"),
    "follow": ("verified", "instant_command"),
    "food": ("cautious", "cautious_terminal"),
    "gamer": ("cautious", "cautious_terminal"),
    "gamma": ("verified", "instant_command"),
    "get": ("verified", "get_acquisition"),
    "give": ("verified", "instant_command"),
    #20260913_kpopmodder: Only the bound direct-XYZ evaluator can verify GOTO.
    "goto": ("verified", "goto_terminal"),
    "hero": ("cautious", "cautious_terminal"),
    "idle": ("cautious", "cautious_terminal"),
    "locate_structure": ("cautious", "cautious_terminal"),
    "meat": ("cautious", "cautious_terminal"),
    "overlay": ("verified", "instant_command"),
    "reload_settings": ("verified", "instant_command"),
    "resetmemory": ("verified", "instant_command"),
    "scan": ("verified", "instant_command"),
    "stop": ("cautious", "specialized_stop_control"),
    "store_home": ("verified", "store_home_completion"),
    "자동보관등록": ("verified", "instant_command"),
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
