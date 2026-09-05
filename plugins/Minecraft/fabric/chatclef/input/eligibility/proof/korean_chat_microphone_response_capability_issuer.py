#20260905_kpopmodder: Issues response authority from one live Korean proof.
from __future__ import annotations

from .korean_chat_microphone_proof_lifecycle import (
    KoreanChatMicrophoneProofLifecycle,
)


class KoreanChatMicrophoneResponseCapabilityIssuer:
    def __init__(self, lifecycle: KoreanChatMicrophoneProofLifecycle) -> None:
        if type(lifecycle) is not KoreanChatMicrophoneProofLifecycle:
            raise TypeError("lifecycle must be exact")
        self._lifecycle = lifecycle

    def issue(
        self,
        event: object,
        owner: object,
        *,
        text: str,
        source: str,
        response_kind: str = "immediate",
    ):
        if event is not self._lifecycle.event or owner is not self._lifecycle.owner:
            return None
        with self._lifecycle.lock:
            if not self._lifecycle.matches_event_locked(event):
                return None
            try:
                return (
                    self._lifecycle.consumed_evidence
                    .issue_routed_response_emission_capability(
                        event,
                        owner,
                        text=text,
                        source=source,
                        response_kind=response_kind,
                    )
                )
            except Exception:
                return None


__all__ = ("KoreanChatMicrophoneResponseCapabilityIssuer",)
