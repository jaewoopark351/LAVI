#20260901_kpopmodder: Keep command readiness axes in one focused contract file.
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
