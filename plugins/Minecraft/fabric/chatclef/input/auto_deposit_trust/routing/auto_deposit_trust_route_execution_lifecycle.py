#20260905_kpopmodder: Own H5 claim acquisition, release, and fail-closed execution.
from __future__ import annotations


class AutoDepositTrustRouteExecutionLifecycle:
    def __init__(
        self,
        *,
        claim_owner: object,
        claim_registry: object,
        submission_route_lock: object,
        decision_factory: object,
        router_logger: object,
    ):
        self._claim_owner = claim_owner
        self._claim_registry = claim_registry
        self._submission_route_lock = submission_route_lock
        self._decision_factory = decision_factory
        self._router_logger = router_logger

    def execute(self, event: object, route_sequence: object) -> object:
        receipt = None
        try:
            rejection = route_sequence.inspect_input(event)
            if rejection is not None:
                return rejection
            with self._submission_route_lock:
                receipt, claim_error = self._claim_registry.claim(
                    event,
                    claim_owner=self._claim_owner,
                )
                if receipt is None:
                    return self._decision_factory.input_rejection(
                        claim_error,
                        "This input event cannot be claimed for submission.",
                    )
                unavailable = route_sequence.inspect_availability()
                if unavailable is not None:
                    return unavailable
                return route_sequence.route_claimed(event, receipt)
        except Exception as error:
            self._router_logger.log(
                "H5 route failed closed: "
                f"error={type(error).__name__}: {error}"
            )
            return self._decision_factory.input_rejection(
                "auto_deposit_trust_input_internal_error",
                "The automatic-deposit registration request failed safely.",
            )
        finally:
            self._abandon_receipt(receipt)

    def _abandon_receipt(self, receipt: object) -> None:
        if receipt is None:
            return
        try:
            self._claim_registry.abandon_if_issued(receipt)
        except Exception as error:
            self._router_logger.log(
                "H5 receipt abandonment failed safely: "
                f"error={type(error).__name__}: {error}"
            )


__all__ = ("AutoDepositTrustRouteExecutionLifecycle",)
