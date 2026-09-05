#20260905_kpopmodder: Preserve activation-state APIs as a thin facade.
from __future__ import annotations

from typing import Any, Callable

from ..generic_crafting_defaults_activation_receipt import (
    GenericCraftingDefaultsActivationReceipt,
)
from ..generic_crafting_defaults_candidate import GenericCraftingDefaultsCandidate
from .state import (
    GenericCraftingActivationRecordStore,
    GenericCraftingActivationTransitionLifecycle,
)


class GenericCraftingActivationStateStore:
    def __init__(
        self,
        *,
        capacity: int,
        context_factory,
        binding_validator,
        record_store=None,
        transition_lifecycle=None,
    ):
        self.context_factory = context_factory
        self.binding_validator = binding_validator
        self._record_store = record_store or GenericCraftingActivationRecordStore(
            capacity=capacity
        )
        self._transition_lifecycle = (
            transition_lifecycle
            or GenericCraftingActivationTransitionLifecycle(
                record_store=self._record_store,
                context_factory=context_factory,
                binding_validator=binding_validator,
            )
        )

    @property
    def capacity(self) -> int:
        return self._record_store.capacity

    @property
    def lock(self):
        return self._record_store.lock

    @property
    def registry_token(self) -> object:
        return self._record_store.registry_token

    @property
    def records(self) -> dict[str, dict[str, object]]:
        return self._record_store.records

    @property
    def admission_owner(self):
        return self._transition_lifecycle.admission_owner

    def bind_admission_owner(
        self,
        owner: object,
        proof_validator: Callable[[object, object], bool],
    ) -> None:
        self._transition_lifecycle.bind_admission_owner(owner, proof_validator)

    def issue(
        self,
        event: object,
        eligibility_proof: object,
        candidate: GenericCraftingDefaultsCandidate,
        *,
        admission_owner: object,
    ) -> tuple[GenericCraftingDefaultsActivationReceipt | None, str]:
        return self._transition_lifecycle.issue(
            event,
            eligibility_proof,
            candidate,
            admission_owner=admission_owner,
        )

    def inspect_issued(self, receipt, eligibility_proof, event) -> bool:
        return self._transition_lifecycle.inspect_issued(
            receipt,
            eligibility_proof,
            event,
        )

    def bind_translation(
        self,
        receipt: object,
        eligibility_proof: object,
        event: object,
        translation: Any,
    ) -> bool:
        return self._transition_lifecycle.bind_translation(
            receipt,
            eligibility_proof,
            event,
            translation,
        )

    def spend(
        self,
        receipt: object,
        eligibility_proof: object,
        request: object,
        translation: Any,
    ) -> bool:
        return self._transition_lifecycle.spend(
            receipt,
            eligibility_proof,
            request,
            translation,
        )

    def abandon_if_live(self, receipt: object) -> None:
        self._transition_lifecycle.abandon_if_live(receipt)

    def close_dispatch(self, eligibility_proof: object) -> None:
        self._transition_lifecycle.close_dispatch(eligibility_proof)

    def state(self, receipt: object) -> str:
        return self._transition_lifecycle.state(receipt)

    @property
    def live_count(self) -> int:
        return self._transition_lifecycle.live_count

    @property
    def record_count(self) -> int:
        return self._record_store.record_count

    def record_for(self, receipt: object) -> dict[str, object] | None:
        return self._transition_lifecycle.record_for(receipt)


__all__ = ("GenericCraftingActivationStateStore",)
