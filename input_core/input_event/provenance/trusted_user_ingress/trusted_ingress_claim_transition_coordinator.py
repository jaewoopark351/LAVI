#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from .consumed_ingress_evidence import ConsumedIngressEvidence
from .dispatch_owned_ingress_lease import DispatchOwnedIngressLease
from .ingress_claim_state import IngressClaimState
from .queue_owned_ingress_delivery import QueueOwnedIngressDelivery
from .registered_ingress_delivery import RegisteredIngressDelivery
from .trusted_ingress_claim_state_store import TrustedIngressClaimStateStore


class TrustedIngressClaimTransitionCoordinator:
    def __init__(self, state_store) -> None:
        if type(state_store) is not TrustedIngressClaimStateStore:
            raise TypeError("state_store must be exact")
        self._state_store = state_store

    def acquire_queue_ownership(
        self,
        registry,
        delivery: RegisteredIngressDelivery,
        queue_entry_token: object,
    ) -> QueueOwnedIngressDelivery | None:
        if queue_entry_token is None:
            return None
        with self._state_store.lock:
            record = self.matching_record(
                registry,
                delivery,
                IngressClaimState.REGISTERED,
            )
            if record is None:
                return None
            owner_token = object()
            queued = QueueOwnedIngressDelivery(
                event=record["event"],
                registry=registry,
                registry_token=self._state_store.registry_token,
                record_key=delivery._record_key,
                owner_token=owner_token,
                queue_entry_token=queue_entry_token,
            )
            record["state"] = IngressClaimState.QUEUE_OWNED
            record["owner_token"] = owner_token
            record["binding"] = queued
            return queued

    def accept_registered_for_dispatch(
        self,
        registry,
        delivery: RegisteredIngressDelivery,
    ) -> DispatchOwnedIngressLease | None:
        with self._state_store.lock:
            return self.accept_for_dispatch(
                registry,
                delivery,
                IngressClaimState.REGISTERED,
            )

    def accept_queued_for_dispatch(
        self,
        registry,
        delivery: QueueOwnedIngressDelivery,
    ) -> DispatchOwnedIngressLease | None:
        with self._state_store.lock:
            return self.accept_for_dispatch(
                registry,
                delivery,
                IngressClaimState.QUEUE_OWNED,
            )

    def accept_for_dispatch(
        self,
        registry,
        binding: object,
        expected_state: IngressClaimState,
    ) -> DispatchOwnedIngressLease | None:
        record = self.matching_record(registry, binding, expected_state)
        if record is None:
            return None
        owner_token = object()
        lease = DispatchOwnedIngressLease(
            event=record["event"],
            registry=registry,
            registry_token=self._state_store.registry_token,
            record_key=binding._record_key,
            owner_token=owner_token,
        )
        record["state"] = IngressClaimState.DISPATCH_OWNED
        record["owner_token"] = owner_token
        record["binding"] = lease
        return lease

    def consume_dispatch_owned(
        self,
        registry,
        lease: DispatchOwnedIngressLease,
    ) -> ConsumedIngressEvidence | None:
        with self._state_store.lock:
            record = self.matching_record(
                registry,
                lease,
                IngressClaimState.DISPATCH_OWNED,
            )
            if record is None:
                return None
            evidence = ConsumedIngressEvidence._issue(
                event=record["event"],
                event_signature=record["event_signature"],
                registry_token=self._state_store.registry_token,
            )
            lease._terminal_state = IngressClaimState.CONSUMED
            self.retire_record(lease._record_key, record)
            return evidence

    def abandon(
        self,
        registry,
        binding: object,
        state: IngressClaimState,
    ) -> bool:
        try:
            with self._state_store.lock:
                record = self.matching_record(registry, binding, state)
                if record is None:
                    return False
                binding._terminal_state = IngressClaimState.ABANDONED
                self.retire_record(binding._record_key, record)
                return True
        except Exception:
            return False

    def retire_record(
        self,
        record_key: object,
        record: dict[str, object],
    ) -> None:
        event = record["event"]
        self._state_store.records.pop(record_key, None)
        if self._state_store.live_event_ids.get(event.event_id) is record_key:
            self._state_store.live_event_ids.pop(event.event_id, None)
        if self._state_store.live_event_objects.get(id(event)) is record_key:
            self._state_store.live_event_objects.pop(id(event), None)

    def matching_record(
        self,
        registry,
        binding: object,
        expected_state: IngressClaimState,
    ) -> dict[str, object] | None:
        if getattr(binding, "_registry", None) is not registry:
            return None
        if (
            getattr(binding, "_registry_token", None)
            is not self._state_store.registry_token
        ):
            return None
        record_key = getattr(binding, "_record_key", None)
        record = self._state_store.records.get(record_key)
        if record is None:
            return None
        if record.get("state") is not expected_state:
            return None
        if record.get("binding") is not binding:
            return None
        if record.get("owner_token") is not getattr(binding, "_owner_token", None):
            return None
        if record.get("event") is not getattr(binding, "_event", None):
            return None
        return record

    def inspect_state(self, registry, binding: object) -> IngressClaimState | None:
        with self._state_store.lock:
            if getattr(binding, "_registry", None) is not registry:
                return None
            if (
                getattr(binding, "_registry_token", None)
                is not self._state_store.registry_token
            ):
                return None
            record = self._state_store.records.get(
                getattr(binding, "_record_key", None)
            )
            if record is None or record.get("binding") is not binding:
                return None
            if record.get("owner_token") is not getattr(
                binding,
                "_owner_token",
                None,
            ):
                return None
            state = record.get("state")
            return state if isinstance(state, IngressClaimState) else None


__all__ = ("TrustedIngressClaimTransitionCoordinator",)
