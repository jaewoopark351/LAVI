#20260905_kpopmodder: Delegate registered, queued, and held ingress leases.
from __future__ import annotations


class LlmTrustedIngressLeaseFacade:
    def __init__(self, lease_dispatch_coordinator):
        self._lease_dispatch_coordinator = lease_dispatch_coordinator

    def accept_registered(self, delivery, history, system_prompt):
        yield from self._lease_dispatch_coordinator.accept_registered(
            delivery,
            history,
            system_prompt,
        )

    def accept_queued(self, queue_delivery, history, system_prompt):
        yield from self._lease_dispatch_coordinator.accept_queued(
            queue_delivery,
            history,
            system_prompt,
        )

    def dispatch_lease(self, lease, history, system_prompt):
        yield from self._lease_dispatch_coordinator.dispatch_lease(
            lease,
            history,
            system_prompt,
        )


__all__ = ("LlmTrustedIngressLeaseFacade",)
