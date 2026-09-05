#20260905_kpopmodder: Owns the single dispatch-to-consume transition for one claim.
from __future__ import annotations


class DispatchOwnedIngressLease:
    __slots__ = (
        "_event",
        "_owner_token",
        "_record_key",
        "_registry",
        "_registry_token",
        "_terminal_state",
    )

    def __init__(
        self,
        *,
        event: object,
        registry: object,
        registry_token: object,
        record_key: object,
        owner_token: object,
    ):
        self._event = event
        self._registry = registry
        self._registry_token = registry_token
        self._record_key = record_key
        self._owner_token = owner_token
        self._terminal_state = None

    @property
    def event(self):
        return self._event

    @property
    def state(self):
        return self._registry.inspect_state(self) or self._terminal_state

    def consume_for_eligibility(self):
        return self._registry._consume_dispatch_owned(self)

    def abandon(self) -> bool:
        return self._registry._abandon_dispatch_owned(self)

    def __repr__(self) -> str:
        return "DispatchOwnedIngressLease(<opaque>)"

    def __copy__(self):
        raise TypeError("dispatch_owned_ingress_lease_is_not_copyable")

    def __deepcopy__(self, _memo):
        raise TypeError("dispatch_owned_ingress_lease_is_not_copyable")

    def __reduce__(self):
        raise TypeError("dispatch_owned_ingress_lease_is_not_serializable")

    def __reduce_ex__(self, _protocol):
        raise TypeError("dispatch_owned_ingress_lease_is_not_serializable")


__all__ = ("DispatchOwnedIngressLease",)
