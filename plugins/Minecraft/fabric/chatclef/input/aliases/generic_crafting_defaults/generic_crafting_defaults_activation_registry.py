#20260905_kpopmodder: Preserve the activation registry API as a thin lifecycle facade.
from __future__ import annotations

from typing import Any, Callable

from plugins.Minecraft.fabric.chatclef.intent.korean_text_normalizer import (
    KoreanTextNormalizer,
)

from .activation import GenericCraftingActivationContextFactory
from .activation.composition import GenericCraftingActivationComponentGraph
from .generic_crafting_defaults_activation_receipt import (
    GenericCraftingDefaultsActivationReceipt,
)
from .generic_crafting_defaults_candidate import GenericCraftingDefaultsCandidate
from .generic_crafting_defaults_translation_projection import (
    GenericCraftingDefaultsTranslationProjection,
)


class GenericCraftingDefaultsActivationRegistry:
    CAPACITY = 4096
    _EVENT_ID_RE = GenericCraftingActivationContextFactory.EVENT_ID_RE

    def __init__(
        self,
        capacity: int = CAPACITY,
        normalizer: KoreanTextNormalizer | None = None,
    ):
        self._component_graph = GenericCraftingActivationComponentGraph(
            capacity=capacity,
            normalizer=normalizer,
        )
        self._component_graph.install_compatibility_seams(self)

    def _bind_admission_owner(
        self,
        owner: object,
        proof_validator: Callable[[object, object], bool],
    ) -> None:
        self._state_store.bind_admission_owner(owner, proof_validator)

    def _issue(
        self,
        event: object,
        eligibility_proof: object,
        candidate: GenericCraftingDefaultsCandidate,
        *,
        admission_owner: object,
    ) -> tuple[GenericCraftingDefaultsActivationReceipt | None, str]:
        return self._state_store.issue(
            event,
            eligibility_proof,
            candidate,
            admission_owner=admission_owner,
        )

    def inspect_issued(
        self,
        receipt: object,
        eligibility_proof: object,
        event: object,
    ) -> bool:
        return self._state_store.inspect_issued(
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
        return self._state_store.bind_translation(
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
        return self._state_store.spend(
            receipt,
            eligibility_proof,
            request,
            translation,
        )

    def abandon_if_live(self, receipt: object) -> None:
        self._state_store.abandon_if_live(receipt)

    def close_dispatch(self, eligibility_proof: object) -> None:
        self._state_store.close_dispatch(eligibility_proof)

    def state(self, receipt: object) -> str:
        return self._state_store.state(receipt)

    @property
    def live_count(self) -> int:
        return self._state_store.live_count

    @property
    def record_count(self) -> int:
        return self._state_store.record_count

    def _activation_values(
        self,
        event: object,
        candidate: GenericCraftingDefaultsCandidate,
    ) -> dict[str, object] | None:
        return self._context_factory.create(event, candidate)

    def _matches_context(
        self,
        record: dict[str, object],
        eligibility_proof: object,
        event: object,
    ) -> bool:
        return self._binding_validator.matches_context(
            record,
            eligibility_proof,
            event,
        )

    def _event_matches_receipt(
        self,
        event: object,
        receipt: GenericCraftingDefaultsActivationReceipt,
    ) -> bool:
        return self._binding_validator.event_matches_receipt(event, receipt)

    def _projection_matches_receipt(
        self,
        projection: GenericCraftingDefaultsTranslationProjection,
        receipt: GenericCraftingDefaultsActivationReceipt,
    ) -> bool:
        return self._binding_validator.projection_matches_receipt(
            projection,
            receipt,
        )

    def _request_matches(
        self,
        receipt: GenericCraftingDefaultsActivationReceipt,
        request: object,
        translation: Any,
        projection: GenericCraftingDefaultsTranslationProjection,
    ) -> bool:
        return self._binding_validator.request_matches(
            receipt,
            request,
            translation,
            projection,
        )

    def _record_for(self, receipt: object) -> dict[str, object] | None:
        return self._state_store.record_for(receipt)

    def _proof_is_valid(self, proof: object, event: object) -> bool:
        return self._binding_validator.proof_is_valid(proof, event)

    def _request_value(self, request: object, name: str) -> object:
        return self._binding_validator.request_value(request, name)

    @property
    def _admission_owner(self):
        return self._state_store.admission_owner

    @property
    def _proof_validator(self):
        return self._binding_validator.proof_validator
