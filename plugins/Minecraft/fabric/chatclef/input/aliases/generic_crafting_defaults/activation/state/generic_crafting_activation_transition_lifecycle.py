#20260905_kpopmodder: Own validated activation state transitions only.
from __future__ import annotations

from typing import Any, Callable

from ...generic_crafting_defaults_activation_receipt import (
    GenericCraftingDefaultsActivationReceipt,
)
from ...generic_crafting_defaults_activation_state import (
    GenericCraftingDefaultsActivationState,
)
from ...generic_crafting_defaults_candidate import (
    GenericCraftingDefaultsCandidate,
)
from ...generic_crafting_defaults_translation_projection import (
    GenericCraftingDefaultsTranslationProjection,
)


class GenericCraftingActivationTransitionLifecycle:
    def __init__(self, *, record_store, context_factory, binding_validator):
        self._record_store = record_store
        self._context_factory = context_factory
        self._binding_validator = binding_validator
        self.admission_owner = None

    def bind_admission_owner(
        self,
        owner: object,
        proof_validator: Callable[[object, object], bool],
    ) -> None:
        if owner is None or not callable(proof_validator):
            raise TypeError(
                "activation admission owner and proof validator are required"
            )
        with self._record_store.lock:
            if self.admission_owner is None:
                self.admission_owner = owner
                self._binding_validator.bind_proof_validator(proof_validator)
                return
            if self.admission_owner is not owner:
                raise RuntimeError(
                    "activation registry already has a different admission owner"
                )

    def issue(
        self,
        event: object,
        eligibility_proof: object,
        candidate: GenericCraftingDefaultsCandidate,
        *,
        admission_owner: object,
    ) -> tuple[GenericCraftingDefaultsActivationReceipt | None, str]:
        with self._record_store.lock:
            if admission_owner is not self.admission_owner:
                return None, "generic_crafting_activation_owner_invalid"
            if not self._binding_validator.proof_is_valid(
                eligibility_proof,
                event,
            ):
                return None, "generic_crafting_eligibility_proof_invalid"
            values = self._context_factory.create(event, candidate)
            if values is None:
                return None, "generic_crafting_activation_candidate_invalid"
            event_id = values["event_id"]
            if self._record_store.contains(event_id):
                return None, "generic_crafting_activation_duplicate"
            if self._record_store.is_at_capacity():
                return None, "generic_crafting_activation_capacity_exhausted"
            nonce = object()
            receipt = GenericCraftingDefaultsActivationReceipt(
                **values,
                registry_token=self._record_store.registry_token,
                nonce=nonce,
            )
            self._record_store.put(
                event_id,
                {
                    "receipt": receipt,
                    "nonce": nonce,
                    "event": event,
                    "proof": eligibility_proof,
                    "state": (
                        GenericCraftingDefaultsActivationState.ISSUED_UNBOUND
                    ),
                    "projection": None,
                },
            )
            return receipt, ""

    def inspect_issued(
        self,
        receipt: object,
        eligibility_proof: object,
        event: object,
    ) -> bool:
        with self._record_store.lock:
            record = self.record_for(receipt)
            return bool(
                record is not None
                and record.get("state")
                is GenericCraftingDefaultsActivationState.ISSUED_UNBOUND
                and self._binding_validator.matches_context(
                    record,
                    eligibility_proof,
                    event,
                )
            )

    def bind_translation(
        self,
        receipt: object,
        eligibility_proof: object,
        event: object,
        translation: Any,
    ) -> bool:
        try:
            projection = GenericCraftingDefaultsTranslationProjection.from_value(
                translation
            )
        except Exception:
            return False
        with self._record_store.lock:
            record = self.record_for(receipt)
            if (
                record is None
                or record.get("state")
                is not GenericCraftingDefaultsActivationState.ISSUED_UNBOUND
                or not self._binding_validator.matches_context(
                    record,
                    eligibility_proof,
                    event,
                )
                or not self._binding_validator.projection_matches_receipt(
                    projection,
                    receipt,
                )
            ):
                return False
            record["projection"] = projection
            record["state"] = (
                GenericCraftingDefaultsActivationState.TRANSLATION_BOUND
            )
            return True

    def spend(
        self,
        receipt: object,
        eligibility_proof: object,
        request: object,
        translation: Any,
    ) -> bool:
        try:
            projection = GenericCraftingDefaultsTranslationProjection.from_value(
                translation
            )
        except Exception:
            return False
        with self._record_store.lock:
            record = self.record_for(receipt)
            if (
                record is None
                or record.get("state")
                is not GenericCraftingDefaultsActivationState.TRANSLATION_BOUND
                or record.get("projection") != projection
                or record.get("proof") is not eligibility_proof
                or not self._binding_validator.proof_is_valid(
                    eligibility_proof,
                    record.get("event"),
                )
                or not self._binding_validator.request_matches(
                    receipt,
                    request,
                    translation,
                    projection,
                )
            ):
                return False
            record["state"] = GenericCraftingDefaultsActivationState.SPENT
            record["request_id"] = self._binding_validator.request_value(
                request,
                "request_id",
            )
            return True

    def abandon_if_live(self, receipt: object) -> None:
        try:
            with self._record_store.lock:
                record = self.record_for(receipt)
                if record is not None and record.get("state") in {
                    GenericCraftingDefaultsActivationState.ISSUED_UNBOUND,
                    GenericCraftingDefaultsActivationState.TRANSLATION_BOUND,
                }:
                    record["state"] = (
                        GenericCraftingDefaultsActivationState.ABANDONED
                    )
        except Exception:
            return

    def close_dispatch(self, eligibility_proof: object) -> None:
        with self._record_store.lock:
            for event_id in self._record_store.event_ids_for_proof(
                eligibility_proof
            ):
                self._record_store.remove(event_id)

    def state(self, receipt: object) -> str:
        with self._record_store.lock:
            record = self.record_for(receipt)
            state = record.get("state") if record is not None else None
            if isinstance(state, GenericCraftingDefaultsActivationState):
                return state.value
            return "INVALID"

    @property
    def live_count(self) -> int:
        with self._record_store.lock:
            return sum(
                1
                for record in self._record_store.records.values()
                if record.get("state")
                in {
                    GenericCraftingDefaultsActivationState.ISSUED_UNBOUND,
                    GenericCraftingDefaultsActivationState.TRANSLATION_BOUND,
                }
            )

    def record_for(self, receipt: object) -> dict[str, object] | None:
        with self._record_store.lock:
            if not self._binding_validator.receipt_belongs_to_registry(
                receipt,
                self._record_store.registry_token,
            ):
                return None
            record = self._record_store.get(receipt.event_id)
            if (
                record is None
                or not self._binding_validator.record_matches_receipt(
                    record,
                    receipt,
                )
            ):
                return None
            return record


__all__ = ("GenericCraftingActivationTransitionLifecycle",)
