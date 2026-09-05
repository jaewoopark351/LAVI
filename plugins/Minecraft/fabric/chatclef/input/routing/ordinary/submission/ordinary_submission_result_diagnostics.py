#20260905_kpopmodder: Isolate ordinary submission-result diagnostics.
from __future__ import annotations


class OrdinarySubmissionResultDiagnostics:
    def __init__(self, *, decision_factory, router_logger):
        self._decision_factory = decision_factory
        self._router_logger = router_logger

    def report(
        self,
        *,
        translation_status: str,
        translation: object,
        result: object,
    ) -> None:
        self._router_logger.log(
            "route handled: "
            f"translation_status={translation_status} "
            f"command={translation.get('command')} "
            f"result_status={self._decision_factory.result_status(result)} "
            f"ok={result.get('ok')}"
        )


__all__ = ("OrdinarySubmissionResultDiagnostics",)
