#20260905_kpopmodder: Carry one opaque in-process capability bound to an H5 ingress event.
from __future__ import annotations


class AutoDepositTrustInputClaimReceipt:
    __slots__ = (
        "_event_id",
        "_source",
        "_provider_id",
        "_event_kind",
        "_registry_token",
        "_nonce",
    )

    def __init__(
        self,
        *,
        event_id: str,
        source: str,
        provider_id: str,
        event_kind: str,
        registry_token: object,
        nonce: object,
    ):
        self._event_id = event_id
        self._source = source
        self._provider_id = provider_id
        self._event_kind = event_kind
        self._registry_token = registry_token
        self._nonce = nonce

    @property
    def event_id(self) -> str:
        return self._event_id

    @property
    def source(self) -> str:
        return self._source

    @property
    def provider_id(self) -> str:
        return self._provider_id

    @property
    def event_kind(self) -> str:
        return self._event_kind

    def __repr__(self) -> str:
        return "AutoDepositTrustInputClaimReceipt(<opaque>)"

    def __reduce__(self):
        raise TypeError("auto_deposit_trust_claim_receipt_is_not_serializable")

    def __reduce_ex__(self, _protocol):
        raise TypeError("auto_deposit_trust_claim_receipt_is_not_serializable")
