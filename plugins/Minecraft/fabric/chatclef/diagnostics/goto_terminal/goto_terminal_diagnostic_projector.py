#20260913_kpopmodder: Select bounded GOTO facts after their authoritative decisions.
from __future__ import annotations

import hashlib
import re

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus

from .goto_terminal_diagnostic_record import GotoTerminalDiagnosticRecord


class GotoTerminalDiagnosticProjector:
    _TOKEN = re.compile(r"[A-Za-z0-9_.-]{1,96}\Z", re.ASCII)
    _EVENT_ID = re.compile(r"[a-f0-9]{32}\Z", re.ASCII)
    _ROLES = frozenset({"result_evaluation", "late_terminal", "immediate_coalesced"})
    _REASONS = frozenset({
        "invalid_result_type", "invalid_result_data", "profile_unavailable",
        "profile_not_verified", "descriptor_not_verified", "unsupported_detail",
        "evaluator_unavailable", "invalid_evaluation_type", "evaluated",
        "rendered",
        "goto_payload_invalid", "goto_context_mismatch", "goto_binding_missing",
        "goto_binding_mismatch", "goto_status_conflict", "goto_semantic_conflict",
        "goto_result_identity_invalid", "goto_failure_unsupported",
        "goto_arrival_verified", "goto_failure_verified",
    })
    _RESULT_REASONS = frozenset({
        "matching_task_finished", "matching_task_stopped", "user_stop_requested",
        "callback_completed_without_user_task", "task_identity_mismatch",
        "task_observation_unclassified", "command_exception", "dispatch_exception",
        "deadline_exceeded", "dispatch_started", "task_failed",
    })
    _FIDELITIES = frozenset({
        "callback_plus_matching_user_task_event",
        "callback_plus_nonmatching_user_task_event", "callback_without_user_task",
        "callback_without_matching_user_task_event", "dispatch_started_only",
        "command_exception_observed", "dispatch_exception_observed",
        "deadline_without_verified_terminal", "unknown",
    })

    def evidence(self, *, role, result, context, profile, evaluation, reason):
        descriptor = getattr(context, "descriptor", None)
        name = getattr(descriptor, "command_name", None) or getattr(context, "command_name", None)
        if type(name) is not str or name != "goto":
            return None
        data = result.data if type(result) is CommandResultDTO and type(result.data) is dict else {}
        return self._record(
            # Evaluation precedes the owner's late/coalesced presentation choice.
            boundary="evidence_decided", role="result_evaluation", reason=reason,
            descriptor=descriptor, identity=context,
            status=getattr(result, "status", None),
            verified=getattr(evaluation, "verified", None), profile=profile,
            rollout=getattr(profile, "rollout_state", None),
            projection=getattr(evaluation, "projection", None), data=data,
            failure_projection=getattr(evaluation, "failure_projection", None),
        )

    def response(self, *, role, fact, profile, text):
        descriptor = getattr(fact, "descriptor", None)
        name = getattr(descriptor, "command_name", None)
        if type(name) is not str or name != "goto":
            return None
        return self._record(
            boundary="response_rendered", role=role, reason="rendered",
            descriptor=descriptor, identity=getattr(fact, "owner_token", None),
            status=getattr(fact, "status", None),
            verified=getattr(fact, "verified", None), profile=profile,
            rollout=getattr(descriptor, "rollout_state", None),
            projection=getattr(fact, "evidence_projection", None),
            data={"result_reason": getattr(fact, "result_reason", None)}, text=text,
            failure_projection=getattr(fact, "failure_projection", None),
        )

    def _record(
        self, *, boundary, role, reason, descriptor, identity, status, verified,
        profile, rollout, projection, data, text=None, failure_projection=None,
    ):
        event_id = getattr(descriptor, "event_id", None)
        if type(status) is CommandResultStatus:
            status = status.value
        output_chars = "none"
        output_sha256 = "none"
        if type(text) is str:
            output_chars = str(len(text))
            output_sha256 = (
                hashlib.sha256(text.encode("utf-8")).hexdigest()
                if len(text) <= 4096 else "too_long"
            )
        generation = getattr(identity, "generation", None)
        return GotoTerminalDiagnosticRecord(
            boundary=boundary,
            owner=("CommandTerminalEvidenceEvaluator" if boundary == "evidence_decided" else "CommandLifecycleResponseRenderer"),
            role=self._choice(role, self._ROLES),
            reason=self._choice(reason, self._REASONS),
            event_id=(event_id if type(event_id) is str and self._EVENT_ID.fullmatch(event_id) else "unknown"),
            request_id=self._token(getattr(identity, "request_id", None)),
            correlation_id=self._token(getattr(identity, "command_message_id", None)),
            session_id=self._token(getattr(identity, "session_id", None)),
            generation=(str(generation) if type(generation) is int and 0 <= generation < 2**63 else "unknown"),
            status=self._choice(status, frozenset(item.value for item in CommandResultStatus)),
            verified=("true" if verified is True else "false" if verified is False else "unknown"),
            profile_id=self._choice(getattr(profile, "profile_id", None), frozenset({"goto_terminal_evidence_v1", "goto_phrase_v1"})),
            rollout_state=self._choice(rollout, frozenset({"disabled", "cautious", "verified"})),
            evaluator_id=self._choice(getattr(profile, "success_evaluator_id", None), frozenset({"cautious_terminal", "goto_terminal"})),
            family=self._choice(getattr(profile, "family", None), frozenset({"movement_goto"})),
            result_reason=self._choice(data.get("result_reason"), self._RESULT_REASONS),
            result_fidelity=self._choice(data.get("result_fidelity"), self._FIDELITIES),
            projection_present="true" if projection is not None else "false",
            output_chars=output_chars,
            output_sha256=output_sha256,
            failure_projection_present="true" if failure_projection is not None else "false",
        )

    @classmethod
    def _token(cls, value):
        return value if type(value) is str and cls._TOKEN.fullmatch(value) else "unknown"

    @staticmethod
    def _choice(value, allowed):
        return value if type(value) is str and value in allowed else "unknown"
