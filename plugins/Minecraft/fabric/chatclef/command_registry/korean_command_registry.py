#20260820_kpopmodder: Keep full ChatClef command registry metadata separate from compilers.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.command_registry.korean_command_registry_model import (
    ChatClefCommandReadinessAxes,
    ChatClefCommandSpec,
)


class KoreanChatClefCommandRegistry:
    _REGISTERED_COMMANDS = (
        "attack",
        "chatclef",
        "deposit",
        "equip",
        "follow",
        "food",
        "gamer",
        "gamma",
        "get",
        "give",
        "goto",
        "hero",
        "idle",
        "locate_structure",
        "meat",
        "overlay",
        "reload_settings",
        "resetmemory",
        "scan",
        "stop",
    )
    _PUBLIC_KOREAN_COMMANDS = frozenset(
        {
            "deposit",
            "equip",
            "follow",
            "food",
            "get",
            "give",
            "goto",
            "idle",
            "meat",
            "stop",
        }
    )
    _PARSER_READY_COMMANDS = _PUBLIC_KOREAN_COMMANDS | frozenset({"idle"})
    _PYTHON_ADMISSION_READY_COMMANDS = _PUBLIC_KOREAN_COMMANDS | frozenset({"idle"})
    _BRIDGE_LIFECYCLE_READY_COMMANDS = frozenset(
        {"deposit", "equip", "food", "get", "give", "goto", "meat", "stop"}
    )
    _GAMEPLAY_VERIFIABLE_COMMANDS = frozenset({"get"})
    _SAFETY_TIERS = {
        "attack": "R3",
        "chatclef": "R4",
        "deposit": "R2",
        "equip": "R1",
        "follow": "R3",
        "food": "R1",
        "gamer": "R3",
        "gamma": "R0",
        "get": "R1",
        "give": "R2",
        "goto": "R1",
        "hero": "R3",
        "idle": "R3",
        "locate_structure": "R1",
        "meat": "R1",
        "overlay": "R0",
        "reload_settings": "R4",
        "resetmemory": "R4",
        "scan": "R0",
        "stop": "R0",
    }
    _RESOLVER_DOMAINS = {
        "deposit": "item_target",
        "equip": "equipment_item_target",
        "get": "item_target",
        "give": "player_and_item_target",
    }
    _SLOT_SCHEMAS = {
        "attack": ("target", "count?"),
        "chatclef": ("state",),
        "deposit": ("item", "count"),
        "equip": ("equipment_item",),
        "follow": ("player",),
        "food": ("count",),
        "gamer": ("target",),
        "gamma": ("value",),
        "get": ("item", "count"),
        "give": ("player", "item", "count"),
        "goto": ("x", "y", "z"),
        "hero": (),
        "idle": (),
        "locate_structure": ("structure",),
        "meat": ("count",),
        "overlay": ("state",),
        "reload_settings": (),
        "resetmemory": (),
        "scan": ("target",),
        "stop": (),
    }

    def commands(self) -> tuple[ChatClefCommandSpec, ...]:
        return tuple(self.spec(command) for command in self._REGISTERED_COMMANDS)

    def command_names(self) -> tuple[str, ...]:
        return self._REGISTERED_COMMANDS

    def public_korean_command_names(self) -> frozenset[str]:
        return self._PUBLIC_KOREAN_COMMANDS

    def spec(self, command_name: str) -> ChatClefCommandSpec:
        command = str(command_name or "").strip().lower()
        if command not in self._REGISTERED_COMMANDS:
            raise KeyError(f"unknown ChatClef command: {command}")
        return ChatClefCommandSpec(
            command_name=command,
            slot_schema=self._SLOT_SCHEMAS[command],
            resolver_domain=self._RESOLVER_DOMAINS.get(command, "command_specific"),
            lifecycle_kind=self._lifecycle_kind(command),
            safety_tier=self._SAFETY_TIERS[command],
            confirmation_mode=self._confirmation_mode(command),
            allowed_input_sources=self._allowed_input_sources(command),
            serializer_id=f"prefixless_{command}",
            readiness_axes=ChatClefCommandReadinessAxes(
                source_registered=True,
                korean_parse_compile_ready=command in self._PARSER_READY_COMMANDS,
                python_admission_ready=command in self._PYTHON_ADMISSION_READY_COMMANDS,
                bridge_lifecycle_ready=command in self._BRIDGE_LIFECYCLE_READY_COMMANDS,
                gameplay_effect_verifiable=command in self._GAMEPLAY_VERIFIABLE_COMMANDS,
                public_korean_enabled=command in self._PUBLIC_KOREAN_COMMANDS,
            ),
        )

    def is_public_korean_enabled(self, command_name: str) -> bool:
        return str(command_name or "").strip().lower() in self._PUBLIC_KOREAN_COMMANDS

    def _lifecycle_kind(self, command: str) -> str:
        if command in {"chatclef", "gamma", "overlay", "reload_settings", "resetmemory"}:
            return "settings_or_ui"
        if command in {"stop"}:
            return "control"
        if command in {"get", "deposit", "equip", "give", "goto", "follow", "food", "meat"}:
            return "task"
        return "raw_java_task_or_dev"

    def _confirmation_mode(self, command: str) -> str:
        if command in {"deposit", "give"}:
            return "explicit_slot_required"
        if command in self._PUBLIC_KOREAN_COMMANDS:
            return "none"
        tier = self._SAFETY_TIERS[command]
        if tier in {"R3", "R4"}:
            return "direct_typed_confirmation_required"
        return "none"

    def _allowed_input_sources(self, command: str) -> tuple[str, ...]:
        if command in self._PUBLIC_KOREAN_COMMANDS:
            return ("lavi_chat_mic_router", "direct_typed")
        if self._SAFETY_TIERS[command] in {"R3", "R4"}:
            return ("direct_typed_only",)
        return ()
