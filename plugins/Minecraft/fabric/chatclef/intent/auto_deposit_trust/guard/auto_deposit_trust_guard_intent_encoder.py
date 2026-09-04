#20260905_kpopmodder: Encode one guarded H5 classification as a non-executable UNKNOWN intent.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust.contracts import (
    AutoDepositTrustIntentClassification,
)
from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust.guard.auto_deposit_trust_guard_fields import (
    AutoDepositTrustGuardFields,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)


class AutoDepositTrustGuardIntentEncoder:
    def encode(
        self,
        original_text: str,
        classification: AutoDepositTrustIntentClassification,
    ) -> ChatClefIntentDTO:
        if not classification.guarded:
            raise ValueError("auto_deposit_trust_guard_requires_guarded_decision")
        return ChatClefIntentDTO(
            intent_type=ChatClefIntentType.UNKNOWN,
            original_text=original_text,
            source=AutoDepositTrustGuardFields.GUARD_SOURCE,
            slots={
                AutoDepositTrustGuardFields.GUARD_SLOT: True,
                AutoDepositTrustGuardFields.DECISION_SLOT: classification.decision.value,
                AutoDepositTrustGuardFields.REASON_SLOT: classification.reason_code,
                AutoDepositTrustGuardFields.MESSAGE_SLOT: classification.message,
            },
        )
