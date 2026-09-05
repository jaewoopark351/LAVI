#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from .ingress_claim_state import IngressClaimState
from .registered_ingress_delivery import RegisteredIngressDelivery
from .trusted_ingress_claim_state_store import TrustedIngressClaimStateStore
from .trusted_ingress_registration_validator import (
    TrustedIngressRegistrationValidator,
)


class TrustedIngressRegistrationCoordinator:
    def __init__(self, *, state_store, validator) -> None:
        if type(state_store) is not TrustedIngressClaimStateStore:
            raise TypeError("state_store must be exact")
        if type(validator) is not TrustedIngressRegistrationValidator:
            raise TypeError("validator must be exact")
        self._state_store = state_store
        self._validator = validator

    def register(
        self,
        registry,
        *,
        ticket: object,
        capability: object,
        event: object,
    ) -> tuple[RegisteredIngressDelivery | None, str]:
        with self._state_store.lock:
            reason = self._validator.rejection_reason(
                ticket=ticket,
                capability=capability,
                event=event,
            )
            if reason:
                return None, reason
            record_key = ticket._invocation_token
            if record_key in self._state_store.records:
                return None, "trusted_ingress_duplicate_invocation"
            if event.event_id in self._state_store.live_event_ids:
                return None, "trusted_ingress_duplicate_event_id"
            if id(event) in self._state_store.live_event_objects:
                return None, "trusted_ingress_duplicate_event_object"
            if len(self._state_store.records) >= self._state_store.capacity:
                return None, "trusted_ingress_capacity_exhausted"
            owner_token = object()
            delivery = RegisteredIngressDelivery(
                event=event,
                registry=registry,
                registry_token=self._state_store.registry_token,
                record_key=record_key,
                owner_token=owner_token,
            )
            self._state_store.records[record_key] = {
                "binding": delivery,
                "event": event,
                "event_signature": self._validator.event_signature(event),
                "owner_token": owner_token,
                "state": IngressClaimState.REGISTERED,
            }
            self._state_store.live_event_ids[event.event_id] = record_key
            self._state_store.live_event_objects[id(event)] = record_key
            return delivery, ""


__all__ = ("TrustedIngressRegistrationCoordinator",)
