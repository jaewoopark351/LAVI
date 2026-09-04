#20260905_kpopmodder: Admit only the exact deterministic zero-slot H5 translation.
from __future__ import annotations

from collections.abc import Mapping

from .auto_deposit_trust_translation_admission_decision import (
    AutoDepositTrustTranslationAdmissionDecision,
)


class AutoDepositTrustTranslationAdmission:
    COMMAND = "auto_deposit_trust area 16x16"
    INTENT_TYPE = "auto_deposit_trust_area"

    def inspect(
        self,
        translation: object,
    ) -> AutoDepositTrustTranslationAdmissionDecision:
        if not isinstance(translation, Mapping):
            return self._reject()
        intent = translation.get("intent")
        if not isinstance(intent, Mapping):
            return self._reject()
        if (
            translation.get("status") != "validated"
            or translation.get("executable") is not True
            or translation.get("command") != self.COMMAND
            or translation.get("resolved_target") is not None
            or intent.get("intent_type") != self.INTENT_TYPE
            or intent.get("source") != "rule"
            or intent.get("quantity") is not None
            or str(intent.get("item_phrase") or "").strip()
            or intent.get("food_units") is not None
            or intent.get("x") is not None
            or intent.get("y") is not None
            or intent.get("z") is not None
            or str(intent.get("player_name") or "").strip()
            or not isinstance(intent.get("slots"), Mapping)
            or bool(intent.get("slots"))
        ):
            return self._reject()
        return AutoDepositTrustTranslationAdmissionDecision(True)

    def _reject(self) -> AutoDepositTrustTranslationAdmissionDecision:
        return AutoDepositTrustTranslationAdmissionDecision(
            False,
            "auto_deposit_trust_translation_mismatch",
            "The H5 translation did not match the exact rule-only zero-slot command.",
        )


__all__ = ["AutoDepositTrustTranslationAdmission"]
