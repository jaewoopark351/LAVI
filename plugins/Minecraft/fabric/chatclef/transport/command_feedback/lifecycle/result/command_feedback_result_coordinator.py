#20260907_kpopmodder: Project correlated result facts without selecting user-facing phrases.
from __future__ import annotations

from typing import Mapping

from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus

from ..evidence.command_terminal_evidence_evaluation import (
    CommandTerminalEvidenceEvaluation,
)
from ..kind import (
    CommandFeedbackLifecycleKindProfile,
    CommandFeedbackLifecycleKindProfileRegistry,
)
from ..terminal.command_terminal_fact import CommandTerminalFact
from ..terminal.command_stop_terminal_arbitrator import (
    CommandStopTerminalArbitrator,
)
from .command_result_correlator import CommandResultCorrelator
from .diagnostics import CommandTerminalEvidenceFailureReporter


class CommandFeedbackResultCoordinator:
    TERMINAL = frozenset(
        {
            CommandResultStatus.COMPLETED,
            CommandResultStatus.REJECTED,
            CommandResultStatus.FAILED,
            CommandResultStatus.CANCELLED,
            CommandResultStatus.DEADLINE_EXCEEDED,
            CommandResultStatus.UNKNOWN,
        }
    )

    def __init__(
        self,
        *,
        tracker,
        evidence_evaluator,
        correlator=None,
        lifecycle_kind_profiles=None,
        stop_terminal_arbitrator=None,
        evidence_failure_reporter=None,
    ) -> None:
        self._tracker = tracker
        self._evidence_evaluator = evidence_evaluator
        self._correlator = correlator or CommandResultCorrelator()
        self._lifecycle_kinds = (
            lifecycle_kind_profiles or CommandFeedbackLifecycleKindProfileRegistry()
        )
        self._stop_terminal_arbitrator = (
            stop_terminal_arbitrator or CommandStopTerminalArbitrator()
        )
        self._evidence_failures = (
            evidence_failure_reporter
            or CommandTerminalEvidenceFailureReporter()
        )

    def accept_fact(
        self,
        *,
        websocket: object,
        envelope: object,
        result: object,
        outcome: object,
        expected_active: object,
    ) -> CommandTerminalFact | None:
        accepted = bool(
            getattr(outcome, "accepted", False) is True
            and getattr(outcome, "reason", "") == "accepted"
        )
        status = getattr(result, "status", CommandResultStatus.UNKNOWN)
        correlated = self._correlator.correlated(
            tracker=self._tracker,
            websocket=websocket,
            envelope=envelope,
            result=result,
            outcome=outcome,
            expected_active=expected_active,
        )
        if not correlated:
            if accepted and status in self.TERMINAL and expected_active is not None:
                self._tracker.clear_if_owner(expected_active)
            return None
        data = getattr(result, "data", None)
        result_data = data if isinstance(data, Mapping) else {}
        result_reason = str(result_data.get("result_reason") or "")
        if status not in self.TERMINAL:
            self._tracker.record_nonterminal(
                status=getattr(status, "value", str(status)),
                result_reason=result_reason,
                evidence_sequence=result_data.get("evidence_sequence"),
            )
            return None
        if self._stop_terminal_arbitrator.specialized_stop_owns_terminal(
            status=status,
            result_reason=result_reason,
            expected_active=expected_active,
        ):
            self._tracker.discard_terminal(expected_active)
            return None
        descriptor = getattr(getattr(self._tracker, "context", None), "descriptor", None)
        try:
            response_lifecycle_kind = self._lifecycle_kinds.profile(
                getattr(descriptor, "command_name", "")
            ).response_lifecycle_kind
        except KeyError:
            self._tracker.discard_terminal(expected_active)
            return None
        if (
            response_lifecycle_kind
            == CommandFeedbackLifecycleKindProfile.PERSISTENT_TASK
            and status is CommandResultStatus.COMPLETED
        ):
            self._tracker.discard_terminal(expected_active)
            return None
        dispatch_started = self._tracker.dispatch_started_observed(expected_active)
        context = self._tracker.claim_terminal(expected_active)
        if context is None:
            return None
        evaluation = CommandTerminalEvidenceEvaluation(False)
        if status is CommandResultStatus.COMPLETED:
            evaluation = self._evaluate_completed(
                result=result,
                context=context,
                status=getattr(status, "value", str(status)),
            )
        fact = CommandTerminalFact(
            descriptor=context.descriptor,
            status=getattr(status, "value", str(status)),
            verified=evaluation.verified,
            dispatch_started=dispatch_started,
            result_reason=result_reason,
            event_id=context.event_id,
            owner_token=expected_active,
            evidence_projection=evaluation.projection,
        )
        return self._tracker.stage_terminal(expected_active, fact)

    def _evaluate_completed(
        self,
        *,
        result: object,
        context: object,
        status: str,
    ) -> CommandTerminalEvidenceEvaluation:
        try:
            evaluate = getattr(self._evidence_evaluator, "evaluate", None)
            if callable(evaluate):
                evaluation = evaluate(result, context=context)
                if type(evaluation) is CommandTerminalEvidenceEvaluation:
                    return evaluation
                return CommandTerminalEvidenceEvaluation(False)
            verified = getattr(self._evidence_evaluator, "verified", None)
            if not callable(verified):
                return CommandTerminalEvidenceEvaluation(False)
            decision = verified(result, context=context)
            return CommandTerminalEvidenceEvaluation(
                decision if type(decision) is bool else False
            )
        except Exception as error:
            try:
                self._evidence_failures.report(
                    command_name=getattr(context.descriptor, "command_name", ""),
                    status=status,
                    error=error,
                )
            except Exception:
                pass
            return CommandTerminalEvidenceEvaluation(False)


__all__ = ("CommandFeedbackResultCoordinator",)
