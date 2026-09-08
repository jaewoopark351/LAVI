#20260905_kpopmodder: Preserve routed-input publication responsibilities in focused modules.
#20260908_kpopmodder: Protect injected publication custody across resolver exceptions.
from __future__ import annotations

import threading

from .routed_input_publication_custody_failure import (
    RoutedInputPublicationCustodyFailure,
)
from .routed_input_publication_custody import (
    RoutedInputPublicationCustody,
)


def _never_claim(_decision: object) -> bool:
    return False


def _ignore_failure(
    _decision: object,
    *,
    stage: str,
    exception_class: str,
) -> None:
    return None


class RoutedInputPublicationCustodyGuard:
    def __init__(self, *, publication_acknowledger, router=None) -> None:
        self._publication_acknowledger = publication_acknowledger
        self._lock = threading.Lock()
        self._ports = (_never_claim, _ignore_failure)
        self.bind_router(router)

    def bind_router(self, router: object) -> None:
        if router is None:
            self.clear()
            return
        try:
            predicate = getattr(
                router,
                "claims_routed_input_publication_custody",
                None,
            )
            observer = getattr(
                router,
                "observe_routed_input_publication_failure",
                None,
            )
        except Exception:
            predicate = None
            observer = None
        ports = (
            predicate if callable(predicate) else _never_claim,
            observer if callable(observer) else _ignore_failure,
        )
        with self._lock:
            self._ports = ports

    def clear(self) -> None:
        with self._lock:
            self._ports = (_never_claim, _ignore_failure)

    def capture_ports(self):
        return self._captured_ports()

    def claim(self, decision: object, *, ports=None):
        predicate, observer = ports or self._captured_ports()
        if not self._claims(predicate, decision):
            return None
        try:
            return RoutedInputPublicationCustody(
                decision=decision,
                acknowledgement=getattr(
                    decision,
                    "response_publication_acknowledgement",
                ),
                decision_type=type(decision),
                route_kind=getattr(decision, "route_kind"),
                response_kind=getattr(decision, "response_kind"),
                predicate=predicate,
                failure_observer=observer,
            )
        except Exception:
            return None

    def resolve(
        self,
        *,
        decision: object,
        resolver,
        stage: str,
        custody: object = None,
    ):
        active_custody = custody or self.claim(decision)
        if active_custody is None:
            return resolver.resolve(decision)
        identity_failure = self._identity_failure(active_custody, decision)
        if identity_failure is not None:
            return self.fail(
                active_custody,
                stage=identity_failure.stage,
                exception_class=identity_failure.exception_class,
            )
        try:
            return resolver.resolve(decision)
        except Exception as error:
            return self.fail(
                active_custody,
                stage=stage,
                exception_class=type(error).__name__,
            )

    def validate(self, custody: object, decision: object):
        if custody is None:
            return None
        failure = self._identity_failure(custody, decision)
        if failure is None:
            return None
        return self.fail(
            custody,
            stage=failure.stage,
            exception_class=failure.exception_class,
        )

    def fail(
        self,
        custody: object,
        *,
        stage: str,
        exception_class: str,
    ) -> RoutedInputPublicationCustodyFailure:
        failure = RoutedInputPublicationCustodyFailure(
            stage=stage,
            exception_class=exception_class,
        )
        try:
            self._acknowledge_original_false(custody)
        finally:
            self._observe_original_failure(custody, failure)
        return failure

    def _captured_ports(self):
        with self._lock:
            return self._ports

    @staticmethod
    def _identity_failure(custody: object, decision: object):
        try:
            acknowledgement = getattr(
                decision,
                "response_publication_acknowledgement",
            )
        except Exception:
            acknowledgement = None
        if acknowledgement is not custody.acknowledgement:
            return RoutedInputPublicationCustodyFailure(
                stage="publication_acknowledgement_identity",
                exception_class="none",
            )
        try:
            preserved = (
                type(decision) is custody.decision_type
                and getattr(decision, "handled") is True
                and getattr(decision, "route_kind") == custody.route_kind
                and getattr(decision, "response_kind") == custody.response_kind
                and RoutedInputPublicationCustodyGuard._claims(
                    custody.predicate,
                    decision,
                )
            )
        except Exception:
            preserved = False
        if preserved:
            return None
        return RoutedInputPublicationCustodyFailure(
            stage="publication_route_identity",
            exception_class="none",
        )

    def _acknowledge_original_false(self, custody: object) -> None:
        try:
            self._publication_acknowledger.acknowledge(
                custody.decision,
                published=False,
            )
        except Exception:
            pass

    @staticmethod
    def _observe_original_failure(custody: object, failure: object) -> None:
        try:
            custody.failure_observer(
                custody.decision,
                stage=failure.stage,
                exception_class=failure.exception_class,
            )
        except Exception:
            pass

    @staticmethod
    def _claims(predicate, decision: object) -> bool:
        try:
            return predicate(decision) is True
        except Exception:
            return False


__all__ = ("RoutedInputPublicationCustodyGuard",)
