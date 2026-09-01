#20260827_kpopmodder: Enforce STORE_HOME source and staged readiness before public submission.
#20260901_kpopmodder: Depend on the canonical command specification contract.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.command_registry.contracts import (
    ChatClefCommandSpec,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission.store_home_command_admission_decision import (
    StoreHomeCommandAdmissionDecision,
)


class StoreHomeCommandAdmission:
    COMMAND_NAME = "store_home"

    def inspect(
        self,
        command_name: str,
        source: str,
        spec: ChatClefCommandSpec,
    ) -> StoreHomeCommandAdmissionDecision:
        if command_name != self.COMMAND_NAME:
            return StoreHomeCommandAdmissionDecision(True)
        if source not in spec.allowed_input_sources:
            return StoreHomeCommandAdmissionDecision(
                False,
                "store_home_source_not_allowed",
                "STORE_HOME input source is not admitted.",
            )
        readiness = spec.readiness_axes
        if not (
            readiness.source_registered
            and readiness.korean_parse_compile_ready
            and readiness.python_admission_ready
        ):
            return StoreHomeCommandAdmissionDecision(
                False,
                "store_home_python_admission_not_ready",
                "STORE_HOME Python admission is not ready.",
            )
        if readiness.public_korean_enabled and not (
            readiness.bridge_lifecycle_ready
            and readiness.gameplay_effect_verifiable
        ):
            return StoreHomeCommandAdmissionDecision(
                False,
                "store_home_public_readiness_incomplete",
                "STORE_HOME cannot be public-enabled before bridge and "
                "gameplay verification.",
            )
        return StoreHomeCommandAdmissionDecision(True)
