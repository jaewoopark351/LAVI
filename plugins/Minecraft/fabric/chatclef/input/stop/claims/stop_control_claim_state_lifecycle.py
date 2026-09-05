#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.stop.stop_control_claim_receipt import (
    StopControlClaimReceipt,
)
from plugins.Minecraft.fabric.chatclef.input.stop.stop_control_claim_record import (
    StopControlClaimRecord,
)

from plugins.Minecraft.fabric.chatclef.input.stop.claims.stop_control_claim_record_store import StopControlClaimRecordStore


class StopControlClaimStateLifecycle:
    def __init__(
        self,
        *,
        registry_identity: object,
        capacity: int,
        event_id_pattern: object,
        allowed_normalized_phrases: frozenset[str],
        authority_validator: object,
        lock: object,
    ):
        self._registry_identity = registry_identity
        self._event_id_pattern = event_id_pattern
        self._allowed_normalized_phrases = allowed_normalized_phrases
        self._authority_validator = authority_validator
        self._record_store = StopControlClaimRecordStore(
            capacity=capacity,
            lock=lock,
        )

    def issue(
        self,
        *,
        event: object,
        eligibility_proof: object,
        normalized_phrase: str,
    ) -> tuple[StopControlClaimReceipt | None, str]:
        event_id = getattr(event, "event_id", None)
        if not self._valid_event_id(event_id):
            return None, "invalid_event_id"
        if (
            type(normalized_phrase) is not str
            or normalized_phrase not in self._allowed_normalized_phrases
        ):
            return None, "invalid_stop_phrase"
        if not self._authority_validator.accepts(
            event=event,
            eligibility_proof=eligibility_proof,
        ):
            return None, "invalid_eligibility_proof"
        def transition(records, capacity):
            if event_id in records:
                return None, "duplicate_or_spent_stop_claim"
            if len(records) >= capacity:
                return None, "stop_claim_capacity_exhausted"
            nonce = object()
            receipt = StopControlClaimReceipt._issue(
                registry=self._registry_identity,
                record_key=event_id,
                nonce=nonce,
            )
            records[event_id] = StopControlClaimRecord(
                event=event,
                proof=eligibility_proof,
                phrase=normalized_phrase,
                nonce=nonce,
                receipt=receipt,
            )
            return receipt, "issued"
        return self._record_store.transact(transition)

    def spend(
        self,
        receipt: object,
        *,
        event: object,
        eligibility_proof: object,
    ) -> tuple[bool, str]:
        if type(receipt) is not StopControlClaimReceipt:
            return False, "invalid_stop_claim_receipt"
        def transition(records, _capacity):
            if receipt._registry is not self._registry_identity:
                return False, "foreign_stop_claim_receipt"
            record = records.get(receipt._record_key)
            if (
                record is None
                or record.receipt is not receipt
                or record.nonce is not receipt._nonce
            ):
                return False, "invalid_stop_claim_receipt"
            if record.spent:
                return False, "duplicate_or_spent_stop_claim"
            if record.event is not event or record.proof is not eligibility_proof:
                return False, "stop_claim_binding_mismatch"
            proof_is_live = self._authority_validator.accepts(
                event=event,
                eligibility_proof=eligibility_proof,
            )
            record.spent = True
            if not proof_is_live:
                return False, "closed_eligibility_proof"
            return True, "spent"
        return self._record_store.transact(transition)

    def reset(self) -> None:
        self._record_store.clear()

    @property
    def record_count(self) -> int:
        return self._record_store.count

    def _valid_event_id(self, event_id: object) -> bool:
        return (
            type(event_id) is str
            and self._event_id_pattern.fullmatch(event_id) is not None
        )


__all__ = ("StopControlClaimStateLifecycle",)
