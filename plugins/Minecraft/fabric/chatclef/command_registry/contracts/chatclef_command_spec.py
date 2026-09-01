#20260901_kpopmodder: Keep one normalized ChatClef command specification per file.
from __future__ import annotations

from dataclasses import dataclass

from plugins.Minecraft.fabric.chatclef.command_registry.contracts.chatclef_command_readiness_axes import (
    ChatClefCommandReadinessAxes,
)


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
