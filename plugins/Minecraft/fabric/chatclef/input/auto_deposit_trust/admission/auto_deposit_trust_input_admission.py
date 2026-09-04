#20260905_kpopmodder: Admit only exact adapter-stamped local Chat and VoiceInput-final tuples.
from __future__ import annotations

import re

from input_core.input_event.contracts import LaviInputEvent

from .auto_deposit_trust_input_admission_decision import (
    AutoDepositTrustInputAdmissionDecision,
)


class AutoDepositTrustInputAdmission:
    _EVENT_ID_RE = re.compile(r"^[0-9a-f]{32}$", re.ASCII)
    _TRUSTED_TUPLES = frozenset(
        {
            ("lavi_chat_ui", "lavi_chat_ui", "chat_submit", True),
            ("voice_input_final", "VoiceInput", "final_transcript", True),
        }
    )

    def inspect(
        self,
        event: LaviInputEvent,
    ) -> AutoDepositTrustInputAdmissionDecision:
        source = getattr(event, "source", None)
        provider_id = getattr(event, "provider_id", None)
        event_kind = getattr(event, "event_kind", None)
        final = getattr(event, "final", None)
        event_id = getattr(event, "event_id", None)

        if type(source) is not str:
            return self._reject(
                "auto_deposit_trust_input_provenance_invalid",
                "The input provenance or event identifier is invalid.",
            )
        if source not in {"lavi_chat_ui", "voice_input_final"}:
            return self._reject(
                "auto_deposit_trust_input_source_not_allowed",
                "This input source cannot register automatic-deposit destinations.",
            )
        if type(final) is not bool:
            return self._reject(
                "auto_deposit_trust_input_provenance_invalid",
                "The input provenance or event identifier is invalid.",
            )
        if final is not True:
            return self._reject(
                "auto_deposit_trust_input_not_final",
                "Only a final Chat or microphone input can run this command.",
            )
        if (
            type(provider_id) is not str
            or type(event_kind) is not str
            or (source, provider_id, event_kind, final) not in self._TRUSTED_TUPLES
            or type(event_id) is not str
            or self._EVENT_ID_RE.fullmatch(event_id) is None
        ):
            return self._reject(
                "auto_deposit_trust_input_provenance_invalid",
                "The input provenance or event identifier is invalid.",
            )
        return AutoDepositTrustInputAdmissionDecision(True)

    def _reject(
        self,
        reason_code: str,
        message: str,
    ) -> AutoDepositTrustInputAdmissionDecision:
        return AutoDepositTrustInputAdmissionDecision(
            False,
            reason_code,
            message,
        )
