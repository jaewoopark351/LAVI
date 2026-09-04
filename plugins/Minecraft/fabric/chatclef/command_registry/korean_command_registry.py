#20260820_kpopmodder: Keep full ChatClef command registry metadata separate from compilers.
#20260827_kpopmodder: Register private-rollout STORE_HOME readiness and source policy.
#20260828_kpopmodder: Public-enable STORE_HOME for user-approved live chat and microphone validation.
#20260829_openai: Register deposit_all as raw-only shadow metadata without Korean readiness.
#20260901_kpopmodder: Consume command metadata from its focused contract package.
#20260905_kpopmodder: Add source-backed H5 rows with independent readiness and migrated ingress sources.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.command_registry.contracts import (
    ChatClefCommandReadinessAxes,
    ChatClefCommandSpec,
)


class KoreanChatClefCommandRegistry:
    _REGISTERED_COMMANDS = (
        "attack",
        "auto_deposit_trust",
        "auto_deposit_trusted_list",
        "auto_deposit_untrust",
        "chatclef",
        "deposit",
        "deposit_all",
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
        "store_home",
        "자동보관등록",
    )
    _SOURCE_REGISTERED_COMMANDS = frozenset(
        {
            "attack",
            "auto_deposit_trust",
            "auto_deposit_trusted_list",
            "auto_deposit_untrust",
            "chatclef",
            "deposit",
            "deposit_all",
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
            "store_home",
            "자동보관등록",
        }
    )
    _PUBLIC_KOREAN_COMMANDS = frozenset(
        {
            "auto_deposit_trust",
            "deposit",
            "equip",
            "food",
            "get",
            "give",
            "goto",
            "meat",
            "store_home",
        }
    )
    _PARSER_READY_COMMANDS = frozenset(
        {
            "auto_deposit_trust",
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
            "store_home",
        }
    )
    _PYTHON_ADMISSION_READY_COMMANDS = frozenset(
        {
            "auto_deposit_trust",
            "deposit",
            "equip",
            "food",
            "get",
            "give",
            "goto",
            "meat",
            "store_home",
        }
    )
    _BRIDGE_LIFECYCLE_READY_COMMANDS = frozenset(
        {
            "auto_deposit_trust",
            "deposit",
            "equip",
            "food",
            "get",
            "give",
            "goto",
            "meat",
            "stop",
            "store_home",
        }
    )
    _GAMEPLAY_VERIFIABLE_COMMANDS = frozenset({"get", "store_home"})
    _SAFETY_TIERS = {
        "attack": "R3",
        "auto_deposit_trust": "R2",
        "auto_deposit_trusted_list": "R0",
        "auto_deposit_untrust": "R2",
        "chatclef": "R4",
        "deposit": "R2",
        "deposit_all": "R2",
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
        "store_home": "R2",
        "자동보관등록": "R2",
    }
    _RESOLVER_DOMAINS = {
        "deposit": "item_target",
        "equip": "equipment_item_target",
        "get": "item_target",
        "give": "player_and_item_target",
    }
    _SLOT_SCHEMAS = {
        "attack": ("target", "count?"),
        "auto_deposit_trust": (),
        "auto_deposit_trusted_list": (),
        "auto_deposit_untrust": ("destinationId?",),
        "chatclef": ("state",),
        "deposit": ("item", "count"),
        "deposit_all": ("items?",),
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
        "store_home": (),
        "자동보관등록": (),
    }

    def __init__(self) -> None:
        missing_parse = self._PUBLIC_KOREAN_COMMANDS - self._PARSER_READY_COMMANDS
        missing_admission = (
            self._PUBLIC_KOREAN_COMMANDS - self._PYTHON_ADMISSION_READY_COMMANDS
        )
        if missing_parse or missing_admission:
            raise RuntimeError(
                "public Korean commands require independent parse and admission readiness"
            )

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
                source_registered=command in self._SOURCE_REGISTERED_COMMANDS,
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
        if command in {
            "auto_deposit_trust",
            "auto_deposit_trusted_list",
            "auto_deposit_untrust",
            "자동보관등록",
        }:
            return "immediate"
        if command in {"chatclef", "gamma", "overlay", "reload_settings", "resetmemory"}:
            return "settings_or_ui"
        if command in {"stop"}:
            return "control"
        if command in {
            "get",
            "deposit",
            "deposit_all",
            "equip",
            "give",
            "goto",
            "follow",
            "food",
            "meat",
            "store_home",
        }:
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
        if command == "auto_deposit_trust":
            return ("lavi_chat_ui", "voice_input_final")
        if command == "store_home":
            return ("lavi_chat_ui", "voice_input_final", "direct_typed")
        if command in self._PUBLIC_KOREAN_COMMANDS:
            return (
                "lavi_chat_ui",
                "voice_input_final",
                "direct_typed",
                "lavi_gui_korean",
            )
        if command == "stop":
            return ("direct_typed",)
        if self._SAFETY_TIERS[command] in {"R3", "R4"}:
            return ("direct_typed_only",)
        return ()
