#20260905_kpopmodder: Preserve trusted-route publication custody in a focused module.
#20260908_kpopmodder: Fail an acknowledged STATUS pipeline closed exactly once.
from __future__ import annotations

from .command_status_publication_custody import CommandStatusPublicationCustody
from .command_status_publication_pipeline_failure import (
    CommandStatusPublicationPipelineFailure,
)


class CommandStatusPublicationCustodyGuard:
    def __init__(self, *, custody_policy=None, emergency_decision=None) -> None:
        self._custody_policy = custody_policy
        self._emergency_decision = emergency_decision

    def claim(self, decision: object):
        if (
            self._emergency_decision is not None
            and decision is self._emergency_decision
        ):
            return CommandStatusPublicationCustody(
                decision=decision,
                acknowledgement=None,
                diagnostic_custody=None,
                decision_type=type(decision),
                route_kind=decision.route_kind,
                response_kind=decision.response_kind,
                emergency=True,
            )
        if self._emergency_decision is None or not self._matches(decision):
            return None
        acknowledgement = decision.response_publication_acknowledgement
        return CommandStatusPublicationCustody(
            decision=decision,
            acknowledgement=acknowledgement,
            diagnostic_custody=getattr(
                acknowledgement,
                "publication_failure_diagnostic_custody",
                None,
            ),
            decision_type=type(decision),
            route_kind=decision.route_kind,
            response_kind=decision.response_kind,
        )

    @staticmethod
    def capture_exception(
        custody: object,
        failure: object,
        *,
        stage: str,
        error: Exception,
    ):
        if failure is not None:
            return failure
        return CommandStatusPublicationPipelineFailure(
            stage=stage,
            exception_class=type(error).__name__,
        )

    def complete(
        self,
        *,
        custody: object,
        decision: object,
        failure: object,
    ):
        if custody is None:
            return decision
        if custody.emergency:
            return self._emergency_decision
        resolved_failure = failure or self._identity_failure(custody, decision)
        if resolved_failure is None:
            return decision
        self._record_once(custody, resolved_failure)
        self._acknowledge_false_once(custody.acknowledgement)
        return self._emergency_decision

    def _identity_failure(self, custody, decision):
        acknowledgement = getattr(
            decision,
            "response_publication_acknowledgement",
            None,
        )
        if acknowledgement is not custody.acknowledgement:
            return CommandStatusPublicationPipelineFailure(
                stage="publication_acknowledgement_identity",
                exception_class="none",
            )
        try:
            route_preserved = (
                type(decision) is custody.decision_type
                and decision.handled is True
                and decision.route_kind == custody.route_kind
                and decision.response_kind == custody.response_kind
                and self._matches(decision)
            )
        except Exception:
            route_preserved = False
        if route_preserved:
            return None
        return CommandStatusPublicationPipelineFailure(
            stage="publication_route_identity",
            exception_class="none",
        )

    def _matches(self, decision: object) -> bool:
        matcher = getattr(self._custody_policy, "matches", self._custody_policy)
        if not callable(matcher):
            return False
        try:
            return matcher(decision) is True
        except Exception:
            return False

    @staticmethod
    def _record_once(custody, failure) -> None:
        callback = getattr(custody.diagnostic_custody, "record_once", None)
        if not callable(callback):
            return
        try:
            callback(failure.stage, failure.exception_class)
        except Exception:
            pass

    @staticmethod
    def _acknowledge_false_once(acknowledgement: object) -> None:
        callback = getattr(acknowledgement, "acknowledge", None)
        if not callable(callback):
            return
        try:
            callback(published=False)
        except Exception:
            pass


__all__ = ("CommandStatusPublicationCustodyGuard",)
