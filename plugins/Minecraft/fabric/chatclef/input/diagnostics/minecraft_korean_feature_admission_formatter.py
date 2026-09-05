#20260905_kpopmodder: Format the exact canonical bounded feature-admission field set.
from __future__ import annotations

import re
from types import MappingProxyType

from .minecraft_korean_feature_admission_record import (
    MinecraftKoreanFeatureAdmissionRecord,
)


class MinecraftKoreanFeatureAdmissionFormatter:
    EVENT_NAME = "minecraft_korean_feature_admission"
    CANONICAL_FIELDS = (
        "event_id",
        "source",
        "provider_id",
        "event_kind",
        "final",
        "ingress_claim_status",
        "korean_eligible",
        "eligibility_proof_status",
        "feature_scope",
        "feature_policy_id",
        "phrase_rule_id",
        "feature_activation_status",
        "decision",
        "reason",
    )
    _FEATURE_SCOPES = frozenset({"A", "B", "C", "none"})
    _FIELD_VALUES = MappingProxyType(
        {
            "source": frozenset({"lavi_chat_ui", "voice_input_final"}),
            "provider_id": frozenset({"lavi_chat_ui", "VoiceInput"}),
            "event_kind": frozenset({"chat_submit", "final_transcript"}),
            "ingress_claim_status": frozenset({"consumed", "rejected"}),
            "eligibility_proof_status": frozenset({"issued", "not_issued"}),
            "feature_activation_status": frozenset(
                {
                    "activated",
                    "handled_without_activation",
                    "not_activated",
                    "not_attempted",
                }
            ),
            "decision": frozenset({"handled", "fallthrough", "rejected"}),
        }
    )
    _EVENT_ID = re.compile(r"[0-9a-f]{32}\Z", re.ASCII)
    _CONTROLLED_CODE = re.compile(r"[a-z0-9_]{1,160}\Z", re.ASCII)

    def format(self, record: object) -> str:
        if type(record) is not MinecraftKoreanFeatureAdmissionRecord:
            raise TypeError("feature admission record must be exact")
        fields = {name: getattr(record, name) for name in self.CANONICAL_FIELDS}
        if fields["feature_scope"] not in self._FEATURE_SCOPES:
            fields["feature_scope"] = "invalid"
        for name in ("final", "korean_eligible"):
            if type(fields[name]) is not bool:
                fields[name] = "invalid"
        if (
            type(fields["event_id"]) is not str
            or self._EVENT_ID.fullmatch(fields["event_id"]) is None
        ):
            fields["event_id"] = "invalid"
        for name, allowed in self._FIELD_VALUES.items():
            if fields[name] not in allowed:
                fields[name] = "invalid"
        for name in ("feature_policy_id", "phrase_rule_id", "reason"):
            value = fields[name]
            if value != "none" and (
                type(value) is not str or self._CONTROLLED_CODE.fullmatch(value) is None
            ):
                fields[name] = "invalid"
        return f"event={self.EVENT_NAME} " + " ".join(
            f"{name}={_bounded_atom(fields[name])}" for name in self.CANONICAL_FIELDS
        )


def _bounded_atom(value: object) -> str:
    if value is None or value == "":
        return "none"
    if type(value) is bool:
        return "true" if value else "false"
    if type(value) is int:
        return str(value) if -(2**63) <= value < 2**63 else "invalid"
    if type(value) is not str or len(value) > 160:
        return "invalid"
    if any(character.isspace() or character == "=" for character in value):
        return "invalid"
    return value


__all__ = ("MinecraftKoreanFeatureAdmissionFormatter",)
