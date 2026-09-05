#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations


class StopControlClaimAuthorityValidator:
    def __init__(self, *, lock: object, proof_validator=None):
        self._lock = lock
        self._proof_validator = proof_validator or self._default_proof_validator
        self._claim_owner: object | None = None

    def bind_owner(self, owner: object) -> None:
        if owner is None:
            raise ValueError("STOP claim owner is required")
        with self._lock:
            if self._claim_owner is None:
                self._claim_owner = owner
                return
            if self._claim_owner is not owner:
                raise RuntimeError(
                    "STOP claim registry already has a different owner"
                )

    def accepts(self, *, event: object, eligibility_proof: object) -> bool:
        try:
            with self._lock:
                return self._proof_validator(eligibility_proof, event) is True
        except Exception:
            return False

    def _default_proof_validator(self, proof: object, event: object) -> bool:
        from plugins.Minecraft.fabric.chatclef.input.eligibility import (
            KoreanChatMicrophoneEligibilityProof,
        )

        if type(proof) is not KoreanChatMicrophoneEligibilityProof:
            return False
        try:
            return (
                self._claim_owner is not None
                and proof.matches_event(event, self._claim_owner) is True
            )
        except Exception:
            return False


__all__ = ("StopControlClaimAuthorityValidator",)
