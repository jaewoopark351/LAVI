#20260905_kpopmodder: Validate exact H5 guard shape before returning bounded rejection metadata.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust.contracts import (
    AutoDepositTrustIntentClassification,
    AutoDepositTrustIntentDecision,
)
from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust.guard.auto_deposit_trust_guard_decode_result import (
    AutoDepositTrustGuardDecodeResult,
)
from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust.guard.auto_deposit_trust_guard_fields import (
    AutoDepositTrustGuardFields,
)
from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust.guard.auto_deposit_trust_guard_marker_detector import (
    AutoDepositTrustGuardMarkerDetector,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)


class AutoDepositTrustGuardIntentDecoder:
    _EXPECTED_SLOTS = frozenset(
        {
            AutoDepositTrustGuardFields.GUARD_SLOT,
            AutoDepositTrustGuardFields.DECISION_SLOT,
            AutoDepositTrustGuardFields.REASON_SLOT,
            AutoDepositTrustGuardFields.MESSAGE_SLOT,
        }
    )

    def __init__(
        self,
        marker_detector: AutoDepositTrustGuardMarkerDetector | None = None,
    ):
        self._marker_detector = marker_detector or AutoDepositTrustGuardMarkerDetector()

    def decode(self, intent: object) -> AutoDepositTrustGuardDecodeResult:
        if not self._marker_detector.has_marker(intent):
            return AutoDepositTrustGuardDecodeResult.not_guard()
        if not isinstance(intent, ChatClefIntentDTO):
            return AutoDepositTrustGuardDecodeResult.invalid()
        if (
            intent.intent_type is not ChatClefIntentType.UNKNOWN
            or intent.source != AutoDepositTrustGuardFields.GUARD_SOURCE
            or set(intent.slots) != self._EXPECTED_SLOTS
            or intent.slots.get(AutoDepositTrustGuardFields.GUARD_SLOT) is not True
            or self._has_semantic_slots(intent)
        ):
            return AutoDepositTrustGuardDecodeResult.invalid()
        try:
            decision = AutoDepositTrustIntentDecision(
                intent.slots.get(AutoDepositTrustGuardFields.DECISION_SLOT)
            )
        except (TypeError, ValueError):
            return AutoDepositTrustGuardDecodeResult.invalid()
        classification = AutoDepositTrustIntentClassification(decision)
        if (
            not classification.guarded
            or intent.slots.get(AutoDepositTrustGuardFields.REASON_SLOT)
            != classification.reason_code
            or intent.slots.get(AutoDepositTrustGuardFields.MESSAGE_SLOT)
            != classification.message
        ):
            return AutoDepositTrustGuardDecodeResult.invalid()
        return AutoDepositTrustGuardDecodeResult.valid(classification)

    def _has_semantic_slots(self, intent: ChatClefIntentDTO) -> bool:
        return any(
            (
                intent.quantity is not None,
                bool(intent.item_phrase.strip()),
                intent.food_units is not None,
                intent.x is not None,
                intent.y is not None,
                intent.z is not None,
                bool(intent.player_name.strip()),
            )
        )
