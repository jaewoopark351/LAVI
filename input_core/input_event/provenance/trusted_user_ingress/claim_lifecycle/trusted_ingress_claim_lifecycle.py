#20260905_kpopmodder: Owns registration and state transitions for ingress claims.
from __future__ import annotations

from ..ingress_claim_state import IngressClaimState
from ..trusted_ingress_claim_state_store import TrustedIngressClaimStateStore
from ..trusted_ingress_claim_transition_coordinator import (
    TrustedIngressClaimTransitionCoordinator,
)
from ..trusted_ingress_registration_coordinator import (
    TrustedIngressRegistrationCoordinator,
)
from ..trusted_ingress_registration_validator import (
    TrustedIngressRegistrationValidator,
)


class TrustedIngressClaimLifecycle:
    def __init__(self, *, capacity: int) -> None:
        self.state_store = TrustedIngressClaimStateStore(capacity=capacity)
        self.registration_validator = TrustedIngressRegistrationValidator(
            registry_token=self.state_store.registry_token,
            producer_factory_token_callback=lambda: (
                self.state_store.producer_factory_token
            ),
        )
        self.registration_coordinator = TrustedIngressRegistrationCoordinator(
            state_store=self.state_store,
            validator=self.registration_validator,
        )
        self.transition_coordinator = TrustedIngressClaimTransitionCoordinator(
            self.state_store
        )

    def bind_producer_factory(self, factory_token: object) -> object:
        return self.state_store.bind_producer_factory(factory_token)

    def register(self, registry, *, ticket, capability, event):
        return self.registration_coordinator.register(
            registry,
            ticket=ticket,
            capability=capability,
            event=event,
        )

    def acquire_queue_ownership(self, registry, delivery, queue_entry_token):
        return self.transition_coordinator.acquire_queue_ownership(
            registry,
            delivery,
            queue_entry_token,
        )

    def accept_registered_for_dispatch(self, registry, delivery):
        return self.transition_coordinator.accept_registered_for_dispatch(
            registry,
            delivery,
        )

    def accept_queued_for_dispatch(self, registry, delivery):
        return self.transition_coordinator.accept_queued_for_dispatch(
            registry,
            delivery,
        )

    def accept_for_dispatch(
        self,
        registry,
        binding,
        expected_state: IngressClaimState,
    ):
        return self.transition_coordinator.accept_for_dispatch(
            registry,
            binding,
            expected_state,
        )

    def consume_dispatch_owned(self, registry, lease):
        return self.transition_coordinator.consume_dispatch_owned(registry, lease)

    def abandon(self, registry, binding, state: IngressClaimState) -> bool:
        return self.transition_coordinator.abandon(registry, binding, state)

    def retire_record(self, record_key, record) -> None:
        self.transition_coordinator.retire_record(record_key, record)

    def matching_record(self, registry, binding, expected_state):
        return self.transition_coordinator.matching_record(
            registry,
            binding,
            expected_state,
        )

    def inspect_state(self, registry, binding):
        return self.transition_coordinator.inspect_state(registry, binding)


__all__ = ("TrustedIngressClaimLifecycle",)
