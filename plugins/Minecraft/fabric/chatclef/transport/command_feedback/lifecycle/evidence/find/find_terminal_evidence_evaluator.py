#20260914_kpopmodder: Require matching request, command, typed result, and existing lifecycle evidence.
from __future__ import annotations

from typing import Mapping
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.fabric.chatclef.intent.navigation.find import FindRequest
from plugins.Minecraft.fabric.chatclef.result.find import FindTerminalPayload
from ..command_terminal_evidence_evaluation import CommandTerminalEvidenceEvaluation


class FindTerminalEvidenceEvaluator:
    def evaluate(self, result: object, *, data: object, context: object, profile: object):
        descriptor = getattr(context, "descriptor", None)
        if (type(result) is not CommandResultDTO or not isinstance(data, Mapping)
                or not result.request_id or result.request_id != getattr(context, "request_id", None)
                or data.get("result_reason") != "matching_task_finished"
                or data.get("result_fidelity") != "callback_plus_matching_user_task_event"
                or getattr(descriptor, "command_name", None) != "find"
                or getattr(descriptor, "evidence_profile_id", None) != "find_terminal_evidence_v1"
                or getattr(descriptor, "rollout_state", None) != "verified"
                or getattr(profile, "command_name", None) != "find"
                or getattr(profile, "profile_id", None) != "find_terminal_evidence_v1"
                or getattr(profile, "success_evaluator_id", None) != "find_terminal"
                or any(k in data for k in ("request_kind", "control_outcome", "goto_result", "goto_terminal",
                                           "store_home_result", "effect_profile_id", "effect_payload"))
                or data.get("operation") not in (None, "find")):
            return self._reject("find_context_or_lifecycle_mismatch")
        terminal = FindTerminalPayload.from_data(data)
        try:
            expected = FindRequest.parse(descriptor.command)
        except (ValueError, TypeError, AttributeError):
            return self._reject("find_command_invalid")
        if terminal is None or terminal.request != expected:
            return self._reject("find_payload_or_request_mismatch")
        if terminal.code in FindTerminalPayload.SUCCESS:
            if result.status is not CommandResultStatus.COMPLETED or result.ok is not True or result.error_code is not None:
                return self._reject("find_status_conflict")
            return CommandTerminalEvidenceEvaluation(True, terminal, decision_reason="find_observation_verified")
        if result.status is not CommandResultStatus.FAILED or result.ok is not False or result.error_code is not BridgeErrorCode.INTERNAL_ERROR:
            return self._reject("find_status_conflict")
        return CommandTerminalEvidenceEvaluation(False, failure_projection=terminal, decision_reason="find_failure_verified")

    @staticmethod
    def _reject(reason):
        return CommandTerminalEvidenceEvaluation(False, decision_reason=reason)
