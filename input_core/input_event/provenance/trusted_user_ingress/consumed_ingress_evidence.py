#20260905_kpopmodder: Preserves the consumed-ingress evidence compatibility API.
from __future__ import annotations

from .evidence import (
    ConsumedIngressEvidenceComponentGraph,
)


_ISSUANCE_TOKEN = object()


class ConsumedIngressEvidence:
    __slots__ = ("_components", "_registry_token")

    def __init__(
        self,
        *,
        event: object,
        event_signature: tuple[object, ...],
        registry_token: object,
        _issuance_token: object = None,
    ):
        if _issuance_token is not _ISSUANCE_TOKEN:
            raise TypeError(
                "ConsumedIngressEvidence is issued only by the claim registry"
            )
        self._registry_token = registry_token
        self._components = ConsumedIngressEvidenceComponentGraph(
            event=event,
            event_signature=event_signature,
            registry_token=registry_token,
        )

    @classmethod
    def _issue(
        cls,
        *,
        event: object,
        event_signature: tuple[object, ...],
        registry_token: object,
    ):
        return cls(
            event=event,
            event_signature=event_signature,
            registry_token=registry_token,
            _issuance_token=_ISSUANCE_TOKEN,
        )

    @property
    def event(self):
        return self._components.lifecycle.event

    @property
    def closed(self) -> bool:
        return self._components.lifecycle.closed

    @property
    def _event(self):
        return self._components.lifecycle.event

    @property
    def _event_signature(self):
        return self._components.lifecycle.event_signature

    @property
    def _lock(self):
        return self._components.lifecycle.lock

    @property
    def _eligibility_owner(self):
        return self._components.lifecycle.eligibility_owner

    @property
    def _response_emission_keys(self):
        return self._components.capability_key_registry.keys

    @property
    def _closed(self):
        return self._components.lifecycle.closed

    def claim_for_eligibility(self, event: object, owner: object) -> bool:
        return self._components.lifecycle.claim_for_eligibility(event, owner)

    def is_live_for(self, event: object, owner: object = None) -> bool:
        return self._components.lifecycle.is_live_for(event, owner)

    def close(self) -> bool:
        return self._components.closer.close()

    def issue_routed_response_emission_capability(
        self,
        event: object,
        owner: object,
        *,
        text: str,
        source: str,
        response_kind: str = "immediate",
    ):
        return self._components.response_capability_issuer.issue(
            event,
            owner,
            text=text,
            source=source,
            response_kind=response_kind,
        )

    def __repr__(self) -> str:
        return "ConsumedIngressEvidence(<opaque>)"

    def __copy__(self):
        raise TypeError("consumed_ingress_evidence_is_not_copyable")

    def __deepcopy__(self, _memo):
        raise TypeError("consumed_ingress_evidence_is_not_copyable")

    def __reduce__(self):
        raise TypeError("consumed_ingress_evidence_is_not_serializable")

    def __reduce_ex__(self, _protocol):
        raise TypeError("consumed_ingress_evidence_is_not_serializable")


__all__ = ("ConsumedIngressEvidence",)
