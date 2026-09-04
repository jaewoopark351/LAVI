#20260905_kpopmodder: Detect H5 guard markers without validating or mutating their payload.
from __future__ import annotations

from collections.abc import Mapping

from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust.guard.auto_deposit_trust_guard_fields import (
    AutoDepositTrustGuardFields,
)


class AutoDepositTrustGuardMarkerDetector:
    def has_marker(self, intent: object) -> bool:
        if str(getattr(intent, "source", "")) == AutoDepositTrustGuardFields.GUARD_SOURCE:
            return True
        slots = getattr(intent, "slots", None)
        return (
            isinstance(slots, Mapping)
            and AutoDepositTrustGuardFields.GUARD_SLOT in slots
        )
