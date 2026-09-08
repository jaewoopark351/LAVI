#20260908_kpopmodder: Close the raw STATUS permit-to-snapshot handoff fail closed.
from __future__ import annotations

from dataclasses import replace

from ...publication.command_feedback_publication_acknowledgement import (
    CommandFeedbackPublicationAcknowledgement,
)

from .command_status_publication_handoff_failure import (
    CommandStatusPublicationHandoffFailure,
)


class CommandStatusPublicationHandoff:
    def __init__(
        self,
        *,
        acknowledgement_factory,
        publication_failure_callback,
        diagnostic_custody_factory=None,
        fallback_failure_observer=None,
    ) -> None:
        self._acknowledgement_factory = acknowledgement_factory
        self._publication_failure_callback = publication_failure_callback
        self._diagnostic_custody_factory = diagnostic_custody_factory
        self._fallback_failure_observer = fallback_failure_observer

    def attach(self, *, query: object, snapshot: object, permit: object):
        custody = None
        factory = self._diagnostic_custody_factory
        if callable(factory):
            try:
                custody = factory(query, snapshot)
                if not callable(getattr(custody, "record_once", None)):
                    raise TypeError(
                        "STATUS diagnostic custody must provide record_once"
                    )
            except Exception as error:
                return self._fail_without_custody(
                    query=query,
                    snapshot=snapshot,
                    permit=permit,
                    stage="publication_handoff_diagnostic_custody",
                    error=error,
                )

        try:
            acknowledgement = self._acknowledgement_factory(permit, custody)
            if type(acknowledgement) is not CommandFeedbackPublicationAcknowledgement:
                raise TypeError("STATUS acknowledgement must use the production type")
        except Exception as error:
            return self._fail_with_custody(
                custody=custody,
                permit=permit,
                acknowledgement=None,
                stage="publication_handoff_acknowledgement",
                error=error,
            )

        try:
            return replace(
                snapshot,
                publication_acknowledgement=acknowledgement,
            )
        except Exception as error:
            return self._fail_with_custody(
                custody=custody,
                permit=permit,
                acknowledgement=acknowledgement,
                stage="publication_handoff_snapshot",
                error=error,
            )

    def _fail_without_custody(
        self,
        *,
        query: object,
        snapshot: object,
        permit: object,
        stage: str,
        error: Exception,
    ) -> CommandStatusPublicationHandoffFailure:
        exception_class = type(error).__name__
        observer = self._fallback_failure_observer
        if callable(observer):
            try:
                observer(
                    query,
                    snapshot,
                    stage=stage,
                    exception_class=exception_class,
                )
            except Exception:
                pass
        self._fail_publication_once(permit)
        return CommandStatusPublicationHandoffFailure(
            stage=stage,
            exception_class=exception_class,
        )

    def _fail_with_custody(
        self,
        *,
        custody: object,
        permit: object,
        acknowledgement: object,
        stage: str,
        error: Exception,
    ) -> CommandStatusPublicationHandoffFailure:
        record = getattr(custody, "record_once", None)
        if callable(record):
            try:
                record(stage, type(error).__name__)
            except Exception:
                pass
        if acknowledgement is None:
            self._fail_publication_once(permit)
        else:
            self._fail_acknowledgement_once(acknowledgement)
        return CommandStatusPublicationHandoffFailure(
            stage=stage,
            exception_class=type(error).__name__,
        )

    def _fail_publication_once(self, permit: object) -> None:
        try:
            self._publication_failure_callback(permit, False)
        except Exception:
            pass

    @staticmethod
    def _fail_acknowledgement_once(acknowledgement: object) -> None:
        try:
            acknowledgement.acknowledge(published=False)
        except Exception:
            pass


__all__ = ("CommandStatusPublicationHandoff",)
