#20260905_kpopmodder: Preserve the legacy STOP claim registry as a thin facade.
from __future__ import annotations

import re
import threading
from typing import Callable

from .claims import (
    StopControlClaimAuthorityValidator,
    StopControlClaimStateLifecycle,
)
from .stop_control_claim_receipt import StopControlClaimReceipt


class StopControlClaimRegistry:
    DEFAULT_CAPACITY = 4096
    _EVENT_ID = re.compile(r"[0-9a-f]{32}\Z")
    _ALLOWED_NORMALIZED_PHRASES = frozenset(
        {
            "멈춰",
            "멈춰줘",
            "중지",
            "정지",
            "스톱",
            "그만",
            "마크 ai 멈춰",
            "마크 ai 멈춰줘",
            "마인크래프트 ai 멈춰",
            "마인크래프트 ai 멈춰줘",
        }
    )

    def __init__(
        self,
        *,
        capacity: int = DEFAULT_CAPACITY,
        proof_validator: Callable[[object, object], bool] | None = None,
    ):
        if type(capacity) is not int or capacity <= 0:
            raise ValueError("capacity must be a positive int")
        lock = threading.RLock()
        self._authority_validator = StopControlClaimAuthorityValidator(
            lock=lock,
            proof_validator=proof_validator,
        )
        self._state_lifecycle = StopControlClaimStateLifecycle(
            registry_identity=self,
            capacity=capacity,
            event_id_pattern=self._EVENT_ID,
            allowed_normalized_phrases=self._ALLOWED_NORMALIZED_PHRASES,
            authority_validator=self._authority_validator,
            lock=lock,
        )

    def _bind_claim_owner(self, owner: object) -> None:
        self._authority_validator.bind_owner(owner)

    def issue(
        self,
        *,
        event: object,
        eligibility_proof: object,
        normalized_phrase: str,
    ) -> tuple[StopControlClaimReceipt | None, str]:
        return self._state_lifecycle.issue(
            event=event,
            eligibility_proof=eligibility_proof,
            normalized_phrase=normalized_phrase,
        )

    def spend(
        self,
        receipt: object,
        *,
        event: object,
        eligibility_proof: object,
    ) -> tuple[bool, str]:
        return self._state_lifecycle.spend(
            receipt,
            event=event,
            eligibility_proof=eligibility_proof,
        )

    def reset_for_shutdown(self) -> None:
        self._state_lifecycle.reset()

    @property
    def record_count(self) -> int:
        return self._state_lifecycle.record_count

    def accepts_claim_authority(
        self,
        *,
        event: object,
        eligibility_proof: object,
    ) -> bool:
        return self._authority_validator.accepts(
            event=event,
            eligibility_proof=eligibility_proof,
        )


__all__ = ("StopControlClaimRegistry",)
