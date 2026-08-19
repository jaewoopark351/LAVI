#20260820_kpopmodder: Model ChatClef command readiness axes separately from Korean parsing.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class ChatClefCommandReadinessAxes:
    source_registered: bool
    korean_parse_compile_ready: bool
    python_admission_ready: bool
    bridge_lifecycle_ready: bool
    gameplay_effect_verifiable: bool
    public_korean_enabled: bool

    def to_dict(self) -> dict[str, bool]:
        return {
            "SOURCE_REGISTERED": self.source_registered,
            "KOREAN_PARSE_COMPILE_READY": self.korean_parse_compile_ready,
            "PYTHON_ADMISSION_READY": self.python_admission_ready,
            "BRIDGE_LIFECYCLE_READY": self.bridge_lifecycle_ready,
            "GAMEPLAY_EFFECT_VERIFIABLE": self.gameplay_effect_verifiable,
            "PUBLIC_KOREAN_ENABLED": self.public_korean_enabled,
        }


@dataclass(frozen=True)
class ChatClefCommandSpec:
    command_name: str
    slot_schema: tuple[str, ...]
    resolver_domain: str
    lifecycle_kind: str
    safety_tier: str
    confirmation_mode: str
    allowed_input_sources: tuple[str, ...]
    serializer_id: str
    readiness_axes: ChatClefCommandReadinessAxes

    def __post_init__(self) -> None:
        object.__setattr__(self, "command_name", str(self.command_name or ""))
        object.__setattr__(self, "slot_schema", tuple(str(v) for v in self.slot_schema))
        object.__setattr__(self, "resolver_domain", str(self.resolver_domain or ""))
        object.__setattr__(self, "lifecycle_kind", str(self.lifecycle_kind or ""))
        object.__setattr__(self, "safety_tier", str(self.safety_tier or ""))
        object.__setattr__(self, "confirmation_mode", str(self.confirmation_mode or ""))
        object.__setattr__(
            self,
            "allowed_input_sources",
            tuple(str(v) for v in self.allowed_input_sources),
        )
        object.__setattr__(self, "serializer_id", str(self.serializer_id or ""))

    def to_dict(self) -> dict[str, object]:
        return {
            "command_name": self.command_name,
            "slot_schema": list(self.slot_schema),
            "resolver_domain": self.resolver_domain,
            "lifecycle_kind": self.lifecycle_kind,
            "safety_tier": self.safety_tier,
            "confirmation_mode": self.confirmation_mode,
            "allowed_input_sources": list(self.allowed_input_sources),
            "serializer_id": self.serializer_id,
            "readiness_axes": self.readiness_axes.to_dict(),
        }
