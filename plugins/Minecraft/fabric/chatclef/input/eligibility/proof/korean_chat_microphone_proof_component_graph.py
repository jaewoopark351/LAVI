#20260905_kpopmodder: Composes focused Korean eligibility-proof collaborators.
from __future__ import annotations

from .korean_chat_microphone_proof_lifecycle import (
    KoreanChatMicrophoneProofLifecycle,
)
from .korean_chat_microphone_response_capability_issuer import (
    KoreanChatMicrophoneResponseCapabilityIssuer,
)


class KoreanChatMicrophoneProofComponentGraph:
    def __init__(
        self,
        *,
        event: object,
        consumed_evidence: object,
        owner: object,
    ) -> None:
        self.lifecycle = KoreanChatMicrophoneProofLifecycle(
            event=event,
            consumed_evidence=consumed_evidence,
            owner=owner,
        )
        self.response_capability_issuer = (
            KoreanChatMicrophoneResponseCapabilityIssuer(self.lifecycle)
        )


__all__ = ("KoreanChatMicrophoneProofComponentGraph",)
