#20260907_kpopmodder: Keep exact command-to-response-lifecycle coverage closed and reviewable.
from __future__ import annotations

from types import MappingProxyType

from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)

from .command_feedback_lifecycle_kind_profile import (
    CommandFeedbackLifecycleKindProfile,
)


_RESPONSE_LIFECYCLE_KINDS = {
    "attack": "finite_task",
    "auto_deposit_trust": "asynchronous_immediate",
    "auto_deposit_trusted_list": "asynchronous_immediate",
    "auto_deposit_untrust": "asynchronous_immediate",
    "chatclef": "asynchronous_immediate",
    "deposit": "finite_task",
    "deposit_all": "finite_task",
    "equip": "finite_task",
    "follow": "persistent_task",
    "food": "finite_task",
    "gamer": "finite_task",
    "gamma": "asynchronous_immediate",
    "get": "finite_task",
    "give": "finite_task",
    "goto": "finite_task",
    "hero": "persistent_task",
    "idle": "persistent_task",
    "locate_structure": "finite_task",
    "meat": "finite_task",
    "overlay": "asynchronous_immediate",
    "reload_settings": "asynchronous_immediate",
    "resetmemory": "asynchronous_immediate",
    "scan": "asynchronous_immediate",
    "stop": "specialized_control",
    "store_home": "finite_task",
    "자동보관등록": "asynchronous_immediate",
}

_TERMINAL_TRIGGERS = {
    "attack": "result_callback",
    "auto_deposit_trust": "result_callback",
    "auto_deposit_trusted_list": "result_callback",
    "auto_deposit_untrust": "result_callback",
    "chatclef": "result_callback",
    "deposit": "result_callback",
    "deposit_all": "result_callback",
    "equip": "result_callback",
    "follow": "result_callback",
    "food": "result_callback",
    "gamer": "result_callback",
    "gamma": "accepted_submission_caution",
    "get": "result_callback",
    "give": "result_callback",
    "goto": "result_callback",
    "hero": "result_callback",
    "idle": "result_callback",
    "locate_structure": "result_callback",
    "meat": "result_callback",
    "overlay": "result_callback",
    "reload_settings": "result_callback",
    "resetmemory": "result_callback",
    "scan": "result_callback",
    "stop": "specialized_control_result",
    "store_home": "result_callback",
    "자동보관등록": "result_callback",
}


class CommandFeedbackLifecycleKindProfileRegistry:
    def __init__(self, command_registry=None) -> None:
        commands = command_registry or KoreanChatClefCommandRegistry()
        expected = tuple(commands.command_names())
        if tuple(_RESPONSE_LIFECYCLE_KINDS) != expected:
            raise RuntimeError(
                "response lifecycle kinds must cover the command registry exactly"
            )
        if tuple(_TERMINAL_TRIGGERS) != expected:
            raise RuntimeError(
                "terminal triggers must cover the command registry exactly"
            )
        self._profiles = MappingProxyType(
            {
                command_name: CommandFeedbackLifecycleKindProfile(
                    command_name=command_name,
                    response_lifecycle_kind=_RESPONSE_LIFECYCLE_KINDS[command_name],
                    terminal_trigger=_TERMINAL_TRIGGERS[command_name],
                )
                for command_name in expected
            }
        )

    def command_names(self) -> tuple[str, ...]:
        return tuple(self._profiles)

    def profile(self, command_name: object) -> CommandFeedbackLifecycleKindProfile:
        name = str(command_name or "").strip().lower()
        try:
            return self._profiles[name]
        except KeyError as error:
            raise KeyError(f"unknown response lifecycle kind: {name}") from error

    def mapping(self):
        return self._profiles


__all__ = ("CommandFeedbackLifecycleKindProfileRegistry",)
