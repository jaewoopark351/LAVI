#20260905_kpopmodder: Preserves the trusted-ingress claim registry API.
from __future__ import annotations

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent

from .consumed_ingress_evidence import ConsumedIngressEvidence
from .composition import TrustedUserInputIngressClaimRegistryComponentGraph
from .dispatch_owned_ingress_lease import DispatchOwnedIngressLease
from .ingress_claim_state import IngressClaimState
from .queue_owned_ingress_delivery import QueueOwnedIngressDelivery
from .registered_ingress_delivery import RegisteredIngressDelivery
from .trusted_ingress_registration_validator import (
    TrustedIngressRegistrationValidator,
)


class TrustedUserInputIngressClaimRegistry:
    CAPACITY = 4096
    _EVENT_ID_RE = TrustedIngressRegistrationValidator._EVENT_ID_RE

    def __init__(self, capacity: int = CAPACITY):
        self._components = TrustedUserInputIngressClaimRegistryComponentGraph(
            capacity=capacity
        )
        self._claim_lifecycle = self._components.claim_lifecycle
        self._state_store = self._claim_lifecycle.state_store
        self._capacity = self._state_store.capacity
        self._lock = self._state_store.lock
        self._registry_token = self._state_store.registry_token
        self._producer_factory_token = None
        self._records = self._state_store.records
        self._live_event_ids = self._state_store.live_event_ids
        self._live_event_objects = self._state_store.live_event_objects
        self._registration_validator = self._claim_lifecycle.registration_validator
        self._registration_coordinator = (
            self._claim_lifecycle.registration_coordinator
        )
        self._transition_coordinator = self._claim_lifecycle.transition_coordinator
        self._consumed_evidence_authority = (
            self._components.consumed_evidence_authority
        )
        self._consumed_evidence_validator = (
            self._consumed_evidence_authority.validator
        )
        self._trusted_response_capability_consumer = (
            self._components.response_capability_consumer
        )
        self._response_emission_capability_consumer = (
            self._trusted_response_capability_consumer.consumer
        )

    def _bind_producer_factory(self, factory_token: object) -> object:
        registry_token = self._claim_lifecycle.bind_producer_factory(factory_token)
        self._producer_factory_token = self._state_store.producer_factory_token
        return registry_token

    def _register_from_producer(
        self,
        *,
        ticket: object,
        capability: object,
        event: object,
    ) -> tuple[RegisteredIngressDelivery | None, str]:
        return self._claim_lifecycle.register(
            self,
            ticket=ticket,
            capability=capability,
            event=event,
        )

    def _producer_registration_rejection(
        self,
        *,
        ticket: object,
        capability: object,
        event: object,
    ) -> str:
        return self._registration_validator.rejection_reason(
            ticket=ticket,
            capability=capability,
            event=event,
        )

    def _valid_event_fields(self, event: LaviInputEvent) -> bool:
        return self._registration_validator.valid_event_fields(event)

    def _event_signature(self, event: LaviInputEvent) -> tuple[object, ...]:
        return self._registration_validator.event_signature(event)

    def _acquire_queue_ownership(
        self,
        delivery: RegisteredIngressDelivery,
        queue_entry_token: object,
    ) -> QueueOwnedIngressDelivery | None:
        return self._claim_lifecycle.acquire_queue_ownership(
            self,
            delivery,
            queue_entry_token,
        )

    def _accept_registered_for_dispatch(
        self,
        delivery: RegisteredIngressDelivery,
    ) -> DispatchOwnedIngressLease | None:
        return self._claim_lifecycle.accept_registered_for_dispatch(
            self,
            delivery,
        )

    def _accept_queued_for_dispatch(
        self,
        delivery: QueueOwnedIngressDelivery,
    ) -> DispatchOwnedIngressLease | None:
        return self._claim_lifecycle.accept_queued_for_dispatch(
            self,
            delivery,
        )

    def _accept_for_dispatch(
        self,
        binding: object,
        expected_state: IngressClaimState,
    ) -> DispatchOwnedIngressLease | None:
        return self._claim_lifecycle.accept_for_dispatch(
            self,
            binding,
            expected_state,
        )

    def _consume_dispatch_owned(
        self,
        lease: DispatchOwnedIngressLease,
    ) -> ConsumedIngressEvidence | None:
        return self._claim_lifecycle.consume_dispatch_owned(self, lease)

    def _abandon_registered(self, delivery: RegisteredIngressDelivery) -> bool:
        return self._abandon(delivery, IngressClaimState.REGISTERED)

    def _abandon_queue_owned(self, delivery: QueueOwnedIngressDelivery) -> bool:
        return self._abandon(delivery, IngressClaimState.QUEUE_OWNED)

    def _abandon_dispatch_owned(self, lease: DispatchOwnedIngressLease) -> bool:
        return self._abandon(lease, IngressClaimState.DISPATCH_OWNED)

    def _abandon(self, binding: object, state: IngressClaimState) -> bool:
        return self._claim_lifecycle.abandon(self, binding, state)

    def _retire_record(
        self,
        record_key: object,
        record: dict[str, object],
    ) -> None:
        self._claim_lifecycle.retire_record(record_key, record)

    def _matching_record(
        self,
        binding: object,
        expected_state: IngressClaimState,
    ) -> dict[str, object] | None:
        return self._claim_lifecycle.matching_record(
            self,
            binding,
            expected_state,
        )

    def inspect_state(self, binding: object) -> IngressClaimState | None:
        return self._claim_lifecycle.inspect_state(self, binding)

    def validate_consumed_evidence(
        self,
        event: object,
        evidence: object,
    ) -> bool:
        return self._consumed_evidence_authority.validate(event, evidence)

    def consume_routed_response_emission_capability(
        self,
        capability: object,
        *,
        event: object,
        text: str,
        source: str,
        response_kind: str = "immediate",
    ) -> bool:
        return self._trusted_response_capability_consumer.consume(
            capability,
            event=event,
            text=text,
            source=source,
            response_kind=response_kind,
        )

    @property
    def live_count(self) -> int:
        return self._state_store.live_count


__all__ = ("TrustedUserInputIngressClaimRegistry",)
