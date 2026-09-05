#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from input_core.input_event.provenance.trusted_user_ingress import (
    QueueOwnedIngressDelivery,
    RegisteredIngressDelivery,
)


class TrustedIngressLeaseDispatchCoordinator:
    def __init__(self, *, predict_callback) -> None:
        if not callable(predict_callback):
            raise TypeError("predict_callback must be callable")
        self._predict_callback = predict_callback

    def accept_registered(self, delivery, history, system_prompt):
        if type(delivery) is not RegisteredIngressDelivery:
            return
        lease = delivery.accept_for_dispatch()
        if lease is None:
            return
        yield from self.dispatch_lease(lease, history, system_prompt)

    def accept_queued(self, queue_delivery, history, system_prompt):
        if type(queue_delivery) is not QueueOwnedIngressDelivery:
            return
        lease = queue_delivery.accept_for_dispatch()
        if lease is None:
            return
        yield from self.dispatch_lease(lease, history, system_prompt)

    def dispatch_lease(self, lease, history, system_prompt):
        evidence = None
        try:
            evidence = lease.consume_for_eligibility()
            if evidence is None:
                return
            yield from self._predict_callback(
                evidence.event,
                history,
                system_prompt,
                trusted_ingress_evidence=evidence,
            )
        finally:
            if evidence is not None:
                evidence.close()
            lease.abandon()


__all__ = ("TrustedIngressLeaseDispatchCoordinator",)
