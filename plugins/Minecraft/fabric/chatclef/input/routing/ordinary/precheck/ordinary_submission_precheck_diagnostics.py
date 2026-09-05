#20260905_kpopmodder: Isolate ordinary submission precheck diagnostics.
from __future__ import annotations


class OrdinarySubmissionPrecheckDiagnostics:
    def __init__(self, router_logger):
        self._router_logger = router_logger

    def report(self, readiness) -> None:
        self._router_logger.log(
            "route rejected by submission precheck: "
            f"reason={readiness.reason} error={readiness.error} "
            f"message={readiness.message}"
        )
        self._router_logger.log_active_command_reconciliation(readiness.details)


__all__ = ("OrdinarySubmissionPrecheckDiagnostics",)
