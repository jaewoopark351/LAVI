#20260908_kpopmodder: Protect an acknowledged STATUS decision across optional-result validation.
from __future__ import annotations


class AcknowledgedOptionalRouteResultGuard:
    def __init__(
        self,
        *,
        custody_policy=None,
        emergency_decision=None,
    ) -> None:
        self._custody_policy = custody_policy
        self._emergency_decision = emergency_decision

    def validate(self, decision: object, validator):
        if (
            self._emergency_decision is not None
            and decision is self._emergency_decision
        ):
            return decision
        if not self._matches(decision) or self._emergency_decision is None:
            return validator.validate(decision)

        acknowledgement = getattr(
            decision,
            "response_publication_acknowledgement",
            None,
        )
        try:
            validated = validator.validate(decision)
        except Exception as error:
            return self._fail_closed(
                acknowledgement,
                stage="optional_route_result_validation",
                exception_class=type(error).__name__,
            )
        if (
            validated is not decision
            or getattr(
                validated,
                "response_publication_acknowledgement",
                None,
            )
            is not acknowledgement
        ):
            return self._fail_closed(
                acknowledgement,
                stage="optional_route_result_acknowledgement_identity",
                exception_class="none",
            )
        return validated

    def _matches(self, decision: object) -> bool:
        policy = self._custody_policy
        matcher = getattr(policy, "matches", policy)
        if not callable(matcher):
            return False
        try:
            return matcher(decision) is True
        except Exception:
            return False

    def _fail_closed(
        self,
        acknowledgement: object,
        *,
        stage: str,
        exception_class: str,
    ):
        custody = getattr(
            acknowledgement,
            "publication_failure_diagnostic_custody",
            None,
        )
        record = getattr(custody, "record_once", None)
        if callable(record):
            try:
                record(stage, exception_class)
            except Exception:
                pass
        callback = getattr(acknowledgement, "acknowledge", None)
        if callable(callback):
            try:
                callback(published=False)
            except Exception:
                pass
        return self._emergency_decision


__all__ = ("AcknowledgedOptionalRouteResultGuard",)
