#20260905_kpopmodder: Validate the exact live trusted Korean proof boundary.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.eligibility import (
    KoreanChatMicrophoneEligibilityProof,
)


class TrustedKoreanProofValidator:
    def __init__(self, *, owner: object):
        self._owner = owner

    def is_live(self, proof: object, event: object) -> bool:
        if type(proof) is not KoreanChatMicrophoneEligibilityProof:
            return False
        try:
            return proof.matches_event(event, self._owner) is True
        except Exception:
            return False


__all__ = ("TrustedKoreanProofValidator",)
