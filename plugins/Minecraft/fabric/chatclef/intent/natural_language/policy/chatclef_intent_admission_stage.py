#20260905_kpopmodder: Own deterministic pre-translation intent admission decisions.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import (
    ChatClefIntentStatus,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)
from plugins.Minecraft.fabric.chatclef.intent.store_home import (
    StoreHomeIntentClassification,
)


class ChatClefIntentAdmissionStage:
    def __init__(
        self,
        *,
        schema_validator: object,
        guard_decoder: object,
        rejection_factory: object,
    ):
        self._schema_validator = schema_validator
        self._guard_decoder = guard_decoder
        self._rejection_factory = rejection_factory

    def inspect(self, intent: object) -> object | None:
        guard = self._guard_decoder.decode(intent)
        if guard.is_valid:
            classification = guard.classification
            if classification is None:
                raise RuntimeError(
                    "valid_auto_deposit_trust_guard_missing_classification"
                )
            return self._rejection_factory.create(
                ChatClefIntentStatus.INVALID,
                guard.reason_code,
                guard.message,
                intent,
                {"auto_deposit_trust_decision": classification.decision.value},
            )
        if guard.is_invalid:
            return self._rejection_factory.create(
                ChatClefIntentStatus.INVALID,
                guard.reason_code,
                guard.message,
                intent,
            )
        valid, reason_code, message = self._schema_validator.validate(intent)
        if not valid:
            return self._rejection_factory.create(
                ChatClefIntentStatus.INVALID,
                reason_code,
                message,
                intent,
            )
        store_home = StoreHomeIntentClassification.guarded_from_intent(intent)
        if store_home is not None:
            return self._rejection_factory.create(
                ChatClefIntentStatus.INVALID,
                store_home.reason_code,
                store_home.message,
                intent,
                {"store_home_decision": store_home.decision.value},
            )
        if intent.intent_type is ChatClefIntentType.UNKNOWN:
            return self._rejection_factory.create(
                ChatClefIntentStatus.UNKNOWN,
                "unknown_intent",
                "Korean command did not match a supported ChatClef intent.",
                intent,
            )
        return None


__all__ = ("ChatClefIntentAdmissionStage",)
