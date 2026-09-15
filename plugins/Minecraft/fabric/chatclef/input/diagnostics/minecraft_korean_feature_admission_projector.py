#20260905_kpopmodder: Coordinate bounded feature-admission diagnostic projection.
from __future__ import annotations

from collections.abc import Mapping

from .minecraft_korean_feature_admission_record import (
    MinecraftKoreanFeatureAdmissionRecord,
)
from .projection import (
    MinecraftKoreanActivationStatusProjector,
    MinecraftKoreanControlledReasonSanitizer,
    MinecraftKoreanFeatureIdentityClassifier,
    MinecraftKoreanIngressClaimStatusProjector,
    MinecraftKoreanLanguageStatusProjector,
    MinecraftKoreanRouteDecisionProjector,
)
from .projection.minecraft_korean_interpretation_rule_projector import MinecraftKoreanInterpretationRuleProjector


class MinecraftKoreanFeatureAdmissionProjector:
    def __init__(self, *, stop_classifier=None, crafting_detector=None):
        self._feature_identity_classifier = (
            MinecraftKoreanFeatureIdentityClassifier(
                stop_classifier=stop_classifier,
                crafting_detector=crafting_detector,
            )
        )
        self._ingress_status_projector = (
            MinecraftKoreanIngressClaimStatusProjector()
        )
        self._language_status_projector = MinecraftKoreanLanguageStatusProjector()
        self._activation_status_projector = (
            MinecraftKoreanActivationStatusProjector()
        )
        self._route_decision_projector = MinecraftKoreanRouteDecisionProjector()
        self._reason_sanitizer = MinecraftKoreanControlledReasonSanitizer()

    def project(
        self,
        *,
        event: object,
        eligibility_reason: object,
        proof_issued: bool,
        route_decision: object,
    ) -> MinecraftKoreanFeatureAdmissionRecord:
        if type(proof_issued) is not bool:
            raise TypeError("proof_issued must be an exact bool")
        route_kind = str(
            getattr(route_decision, "route_kind", "minecraft_chatclef")
            or "minecraft_chatclef"
        )
        handled = getattr(route_decision, "handled", False) is True
        feature_scope, policy_id, phrase_rule_id = (
            self._feature_identity_classifier.classify(
                event,
                proof_issued=proof_issued,
                route_kind=route_kind,
                handled=handled,
            )
        )
        reason = (
            getattr(route_decision, "reason", None)
            if proof_issued
            else eligibility_reason
        )
        if proof_issued and route_kind in {"minecraft_command", "minecraft_chatclef"}:
            selected_rule = MinecraftKoreanInterpretationRuleProjector.project(getattr(route_decision, "translation", None))
            if selected_rule != "none":
                phrase_rule_id = selected_rule
        #20260915_kpopmodder: Observe the already-selected closed runtime ambiguity decision without changing routing.
        result = getattr(route_decision, "result", None)
        if reason == "minecraft_item_command_translation_rejected" and isinstance(result, Mapping):
            actual_reason = result.get("error")
            if type(actual_reason) is str and actual_reason in MinecraftKoreanControlledReasonSanitizer.NAME_REASONS:
                reason = actual_reason
        return MinecraftKoreanFeatureAdmissionRecord(
            event_id=getattr(event, "event_id", None),
            source=getattr(event, "source", None),
            provider_id=getattr(event, "provider_id", None),
            event_kind=getattr(event, "event_kind", None),
            final=getattr(event, "final", None),
            ingress_claim_status=self._ingress_status_projector.project(
                eligibility_reason,
                proof_issued=proof_issued,
            ),
            korean_eligible=self._language_status_projector.contains_hangul(
                getattr(event, "text", None)
            ),
            eligibility_proof_status=("issued" if proof_issued else "not_issued"),
            feature_scope=feature_scope,
            feature_policy_id=policy_id,
            phrase_rule_id=phrase_rule_id,
            feature_activation_status=self._activation_status_projector.project(
                proof_issued=proof_issued,
                feature_scope=feature_scope,
                handled=handled,
                route_decision=route_decision,
            ),
            decision=self._route_decision_projector.project(
                eligibility_reason,
                proof_issued=proof_issued,
                handled=handled,
            ),
            reason=self._reason_sanitizer.sanitize(reason),
        )


__all__ = ("MinecraftKoreanFeatureAdmissionProjector",)
