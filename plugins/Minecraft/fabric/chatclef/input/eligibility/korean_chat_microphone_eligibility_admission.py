#20260905_kpopmodder: Admit exact trusted Chat/final-microphone events containing Hangul.
from __future__ import annotations

import re
import unicodedata

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent
from input_core.input_event.provenance.trusted_user_ingress import (
    ConsumedIngressEvidence,
)

from .korean_chat_microphone_eligibility_proof import (
    KoreanChatMicrophoneEligibilityProof,
)


class KoreanChatMicrophoneEligibilityAdmission:
    _TUPLES = frozenset(
        {
            ("lavi_chat_ui", "lavi_chat_ui", "chat_submit", True),
            ("voice_input_final", "VoiceInput", "final_transcript", True),
        }
    )
    _EVENT_ID = re.compile(r"[0-9a-f]{32}\Z", re.ASCII)

    def __init__(self, consumed_evidence_validator=None):
        if (
            consumed_evidence_validator is not None
            and not callable(consumed_evidence_validator)
        ):
            raise TypeError("consumed_evidence_validator must be callable")
        self._consumed_evidence_validator = consumed_evidence_validator

    def issue(
        self,
        *,
        event: object,
        consumed_ingress_evidence: object,
        owner: object,
    ) -> tuple[KoreanChatMicrophoneEligibilityProof | None, str]:
        if type(event) is not LaviInputEvent:
            return None, "input_event_type_invalid"
        if type(consumed_ingress_evidence) is not ConsumedIngressEvidence:
            return None, "consumed_ingress_evidence_invalid"
        if owner is None:
            return None, "eligibility_owner_missing"
        if self._consumed_evidence_validator is None:
            return None, "consumed_ingress_authority_unavailable"
        try:
            evidence_is_authoritative = self._consumed_evidence_validator(
                event,
                consumed_ingress_evidence,
            )
        except Exception:
            return None, "consumed_ingress_authority_failed"
        if evidence_is_authoritative is not True:
            return None, "consumed_ingress_evidence_foreign"
        if not self._valid_event(event):
            return None, "input_event_shape_invalid"
        source_tuple = (
            event.source,
            event.provider_id,
            event.event_kind,
            event.final,
        )
        if source_tuple not in self._TUPLES:
            return None, "input_source_not_eligible"
        if not self._contains_hangul(event.text):
            return None, "input_language_not_korean"
        if not consumed_ingress_evidence.claim_for_eligibility(event, owner):
            return None, "consumed_ingress_evidence_spent"
        return (
            KoreanChatMicrophoneEligibilityProof._issue(
                event=event,
                consumed_evidence=consumed_ingress_evidence,
                owner=owner,
            ),
            "eligible",
        )

    def _valid_event(self, event: LaviInputEvent) -> bool:
        return (
            type(event.text) is str
            and type(event.source) is str
            and type(event.provider_id) is str
            and type(event.event_kind) is str
            and type(event.final) is bool
            and type(event.event_id) is str
            and self._EVENT_ID.fullmatch(event.event_id) is not None
        )

    def _contains_hangul(self, text: str) -> bool:
        normalized = unicodedata.normalize("NFKC", text)
        return any(
            "\u1100" <= character <= "\u11ff"
            or "\u3130" <= character <= "\u318f"
            or "\ua960" <= character <= "\ua97f"
            or "\uac00" <= character <= "\ud7a3"
            or "\ud7b0" <= character <= "\ud7ff"
            for character in normalized
        )


__all__ = ("KoreanChatMicrophoneEligibilityAdmission",)
