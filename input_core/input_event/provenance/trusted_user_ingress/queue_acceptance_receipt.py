#20260905_kpopmodder: Proves one exact queue entry accepted ownership before publication.
from __future__ import annotations

from .queue_owned_ingress_delivery import QueueOwnedIngressDelivery


_ISSUANCE_TOKEN = object()


class QueueAcceptanceReceipt:
    __slots__ = ("_queue_delivery", "_queue_entry_token")

    def __init__(
        self,
        *,
        queue_delivery: object,
        queue_entry_token: object,
        _issuance_token: object = None,
    ):
        if _issuance_token is not _ISSUANCE_TOKEN:
            raise TypeError(
                "QueueAcceptanceReceipt is issued only by the queue sink"
            )
        self._queue_delivery = queue_delivery
        self._queue_entry_token = queue_entry_token

    @classmethod
    def _issue(
        cls,
        *,
        queue_delivery: object,
        queue_entry_token: object,
    ) -> "QueueAcceptanceReceipt":
        return cls(
            queue_delivery=queue_delivery,
            queue_entry_token=queue_entry_token,
            _issuance_token=_ISSUANCE_TOKEN,
        )

    @property
    def queue_delivery(self):
        return self._queue_delivery

    @property
    def event(self):
        return self._queue_delivery.event

    def matches_registered_delivery(self, delivery: object) -> bool:
        queued = self._queue_delivery
        return (
            type(queued) is QueueOwnedIngressDelivery
            and getattr(queued, "_registry", None)
            is getattr(delivery, "_registry", None)
            and getattr(queued, "_registry_token", None)
            is getattr(delivery, "_registry_token", None)
            and getattr(queued, "_event", None)
            is getattr(delivery, "_event", None)
            and self._queue_entry_token
            is getattr(queued, "_queue_entry_token", None)
        )

    def __repr__(self) -> str:
        return "QueueAcceptanceReceipt(<opaque>)"

    def __copy__(self):
        raise TypeError("queue_acceptance_receipt_is_not_copyable")

    def __deepcopy__(self, _memo):
        raise TypeError("queue_acceptance_receipt_is_not_copyable")

    def __reduce__(self):
        raise TypeError("queue_acceptance_receipt_is_not_serializable")

    def __reduce_ex__(self, _protocol):
        raise TypeError("queue_acceptance_receipt_is_not_serializable")


__all__ = ("QueueAcceptanceReceipt",)
