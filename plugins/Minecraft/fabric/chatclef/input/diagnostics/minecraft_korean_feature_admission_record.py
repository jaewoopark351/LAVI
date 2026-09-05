#20260905_kpopmodder: Carry only bounded Korean feature-admission state, never input text.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class MinecraftKoreanFeatureAdmissionRecord:
    event_id: object
    source: object
    provider_id: object
    event_kind: object
    final: object
    ingress_claim_status: object
    korean_eligible: object
    eligibility_proof_status: object
    feature_scope: object
    feature_policy_id: object
    phrase_rule_id: object
    feature_activation_status: object
    decision: object
    reason: object


__all__ = ("MinecraftKoreanFeatureAdmissionRecord",)
