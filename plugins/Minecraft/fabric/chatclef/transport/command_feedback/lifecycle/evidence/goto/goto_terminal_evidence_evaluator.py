#20260913_kpopmodder: Verify bound GOTO arrival and typed failure without changing lifecycle ownership.
from __future__ import annotations

from types import MappingProxyType
from typing import Mapping

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.result.goto.goto_command_binding import GotoCommandBinding
from plugins.Minecraft.fabric.chatclef.result.goto.goto_command_binding_decoder import GotoCommandBindingDecoder
from plugins.Minecraft.fabric.chatclef.result.goto.goto_command_binding_matcher import GotoCommandBindingMatcher
from plugins.Minecraft.fabric.chatclef.result.goto.goto_failure_reasons import GOTO_FAILURE_REASONS
from plugins.Minecraft.fabric.chatclef.result.goto.goto_terminal_payload_decoder import GotoTerminalPayloadDecoder

from ..command_terminal_evidence_evaluation import CommandTerminalEvidenceEvaluation


class GotoTerminalEvidenceEvaluator:
    _OWNERS = MappingProxyType({
        "prepared_goto_terminal": "lavi.minecraft.task.movement.gotopreflight.PreparedGotoTask",
        "legacy_get_to_block_terminal": "lavi.minecraft.task.movement.gotoresult.tracking.ReportedGotoBlockTask",
    })
    _FOREIGN_KEYS = frozenset({
        "request_kind", "control_outcome", "control_reason", "stop_command_invoked",
        "effect_profile_id", "effect_profile_version", "effect_payload",
        "effect_kind", "store_home_result",
    })

    def __init__(self, *, payload_decoder=None, binding_matcher=None) -> None:
        self._payloads = payload_decoder or GotoTerminalPayloadDecoder()
        self._bindings = binding_matcher or GotoCommandBindingMatcher()

    def evaluate(self, result: object, *, data: object, context: object, profile: object):
        if (
            type(result) is not CommandResultDTO
            or not isinstance(data, Mapping)
            or data.get("result_reason") != "matching_task_finished"
            or data.get("result_fidelity") != "callback_plus_matching_user_task_event"
            or result.request_id != getattr(context, "request_id", None)
        ):
            return self._reject("goto_result_identity_invalid")
        if (
            any(key in data for key in self._FOREIGN_KEYS)
            or data.get("operation") not in (None, "goto")
        ):
            return self._reject("goto_semantic_conflict")
        terminal = self._payloads.decode(data)
        if terminal is None:
            return self._reject("goto_payload_invalid")
        if not self._profile_matches(context, profile):
            return self._reject("goto_context_mismatch")
        if getattr(context, "goto_binding_rejected", False) is True:
            return self._reject("goto_binding_mismatch")
        binding = getattr(context, "goto_binding", None)
        if type(binding) is not GotoCommandBinding:
            return self._reject("goto_binding_missing")
        if terminal.binding != binding:
            return self._reject("goto_binding_mismatch")
        if not self._bindings.matches_context(binding, context):
            return self._reject("goto_context_mismatch")
        if "goto_binding" in data:
            if GotoCommandBindingDecoder().decode(data["goto_binding"]) != binding:
                return self._reject("goto_binding_mismatch")
        if self._OWNERS.get(terminal.evidence_kind) != binding.task_owner:
            return self._reject("goto_semantic_conflict")
        if "goal_satisfied" in data and data["goal_satisfied"] is not terminal.goal_satisfied:
            return self._reject("goto_semantic_conflict")
        if terminal.outcome == "ARRIVED":
            if (
                result.status is not CommandResultStatus.COMPLETED
                or result.ok is not True or result.error_code is not None
            ):
                return self._reject("goto_status_conflict")
            if (
                terminal.failure_reason != "NONE"
                or terminal.goal_satisfied is not True
                or terminal.binding_valid is not True
                or terminal.children_quiescent is not True
                or terminal.terminal_dimension != binding.world_dimension
            ):
                return self._reject("goto_semantic_conflict")
            return CommandTerminalEvidenceEvaluation(
                True, terminal, decision_reason="goto_arrival_verified",
            )
        if (
            result.status is not CommandResultStatus.FAILED
            or result.ok is not False
            or result.error_code is not BridgeErrorCode.INTERNAL_ERROR
        ):
            return self._reject("goto_status_conflict")
        if terminal.goal_satisfied or terminal.evidence_kind != "prepared_goto_terminal":
            return self._reject("goto_semantic_conflict")
        if terminal.failure_reason not in GOTO_FAILURE_REASONS:
            return self._reject("goto_failure_unsupported")
        return CommandTerminalEvidenceEvaluation(
            False, failure_projection=terminal, decision_reason="goto_failure_verified",
        )

    @staticmethod
    def _profile_matches(context: object, profile: object) -> bool:
        descriptor = getattr(context, "descriptor", None)
        return bool(
            getattr(descriptor, "command_name", None) == "goto"
            and getattr(descriptor, "evidence_profile_id", None) == "goto_terminal_evidence_v1"
            and getattr(descriptor, "rollout_state", None) == "verified"
            and getattr(profile, "command_name", None) == "goto"
            and getattr(profile, "profile_id", None) == "goto_terminal_evidence_v1"
            and getattr(profile, "rollout_state", None) == "verified"
            and getattr(profile, "success_evaluator_id", None) == "goto_terminal"
        )

    @staticmethod
    def _reject(reason: str) -> CommandTerminalEvidenceEvaluation:
        return CommandTerminalEvidenceEvaluation(False, decision_reason=reason)


__all__ = ("GotoTerminalEvidenceEvaluator",)
