#20260908_kpopmodder: Validate strong STORE_HOME completion evidence without rendering or lifecycle mutation.
from __future__ import annotations

from typing import Mapping

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.result.store_home import (
    StoreHomeTerminalPayload,
)

from ..command_terminal_evidence_evaluation import (
    CommandTerminalEvidenceEvaluation,
)


class StoreHomeTerminalEvidenceEvaluator:
    _COMMAND_NAME = "store_home"
    _PROFILE_ID = "store_home_terminal_evidence_v1"
    _EVALUATOR_ID = "store_home_completion"
    _ALLOWED_SOURCES = frozenset({"lavi_chat_ui", "voice_input_final"})
    _MAX_REASON_LENGTH = 256
    _STOP_ONLY_KEYS = frozenset(
        {
            "control_outcome",
            "control_reason",
            "target_scope",
            "target_resolution",
            "requested_target_request_id",
            "requested_target_command_message_id",
            "requested_target_session_id",
            "requested_target_server_connection_generation",
            "resolved_target_request_id",
            "resolved_target_command_message_id",
            "resolved_target_session_id",
            "resolved_target_server_connection_generation",
            "target_state_before",
            "target_state_after",
            "original_result_delivery",
            "stop_command_invoked",
            "connection_generation",
            "java_socket_generation",
            "executed_client_tick",
            "verified_client_tick",
        }
    )
    _GET_RESERVED_KEYS = frozenset(
        {
            "effect_profile_id",
            "effect_profile_version",
            "effect_payload",
            "effect_kind",
            "target_item",
            "target_match_ids",
            "quantity_semantics",
            "requested_count",
            "requested_delta",
            "before_target_count",
            "after_target_count",
            "target_count_delta",
            "effect_observation_status",
            "effect_observation_reason",
        }
    )

    def evaluate(
        self,
        result: object,
        *,
        data: object,
        context: object,
        profile: object,
    ) -> CommandTerminalEvidenceEvaluation:
        if not self._descriptor_agrees(context=context, profile=profile):
            return CommandTerminalEvidenceEvaluation(False)
        if (
            type(result) is not CommandResultDTO
            or result.status is not CommandResultStatus.COMPLETED
            or result.ok is not True
            or result.error_code is not None
            or not isinstance(data, Mapping)
            or data.get("result_reason") != "matching_task_finished"
            or data.get("result_fidelity")
            != "callback_plus_matching_user_task_event"
        ):
            return CommandTerminalEvidenceEvaluation(False)
        if (
            "request_kind" in data
            or data.get("operation") == "stop_ai"
            or any(key in data for key in self._STOP_ONLY_KEYS)
            or any(key in data for key in self._GET_RESERVED_KEYS)
        ):
            return CommandTerminalEvidenceEvaluation(False)
        terminal = StoreHomeTerminalPayload.from_data(data)
        if (
            terminal is None
            or terminal.result != "COMPLETED"
            or terminal.goal_satisfied is not True
            or terminal.remaining_stacks != 0
            or not 1 <= len(terminal.reason) <= self._MAX_REASON_LENGTH
        ):
            return CommandTerminalEvidenceEvaluation(False)
        return CommandTerminalEvidenceEvaluation(True, terminal)

    def _descriptor_agrees(self, *, context: object, profile: object) -> bool:
        descriptor = getattr(context, "descriptor", None)
        command = getattr(descriptor, "command", None)
        source = getattr(descriptor, "command_source", None)
        return bool(
            type(command) is str
            and " ".join(command.split()) == self._COMMAND_NAME
            and getattr(descriptor, "command_name", None) == self._COMMAND_NAME
            and getattr(descriptor, "form_kind", None) == "trusted_translation"
            and getattr(descriptor, "detail_level", None) == "typed"
            and getattr(descriptor, "evidence_profile_id", None) == self._PROFILE_ID
            and getattr(descriptor, "rollout_state", None) == "verified"
            and source == getattr(descriptor, "input_source", None)
            and source in self._ALLOWED_SOURCES
            and getattr(profile, "command_name", None) == self._COMMAND_NAME
            and getattr(profile, "profile_id", None) == self._PROFILE_ID
            and getattr(profile, "rollout_state", None) == "verified"
            and getattr(profile, "success_evaluator_id", None)
            == self._EVALUATOR_ID
        )


__all__ = ("StoreHomeTerminalEvidenceEvaluator",)
