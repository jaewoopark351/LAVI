#20260915_kpopmodder: Bind an observed native result to the existing request and descriptor.
from collections.abc import Mapping

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.result.instant import InstantCommandPayload
from ..command_terminal_evidence_evaluation import CommandTerminalEvidenceEvaluation


class InstantCommandEvidenceEvaluator:
    def evaluate(self, result, *, data, context, profile):
        descriptor = getattr(context, "descriptor", None)
        terminal = InstantCommandPayload.from_data(data)
        if (type(result) is not CommandResultDTO or not isinstance(data, Mapping)
                or not result.request_id or result.request_id != getattr(context, "request_id", None)
                or terminal is None
                or data.get("result_reason") != "instant_command_observed"
                or data.get("result_fidelity") != "command_callback_plus_native_result"
                or terminal.command != getattr(descriptor, "command", "").lstrip("@").strip()
                or terminal.command_name != InstantCommandPayload.native_name(getattr(descriptor, "command_name", None))
                or getattr(descriptor, "evidence_profile_id", None) != getattr(profile, "profile_id", None)
                or terminal.command_name != InstantCommandPayload.native_name(getattr(profile, "command_name", None))
                or any(key in data for key in ("request_kind", "control_outcome", "goto_result", "goto_terminal",
                                               "find", "store_home_result", "effect_payload"))):
            return CommandTerminalEvidenceEvaluation(False, decision_reason="instant_context_or_payload_mismatch")
        expected = {"completed": CommandResultStatus.COMPLETED, "failed": CommandResultStatus.FAILED,
                    "unknown": CommandResultStatus.UNKNOWN}[terminal.outcome]
        if (result.status is not expected or result.ok is not expected.ok
                or (terminal.outcome == "completed" and result.error_code is not None)):
            return CommandTerminalEvidenceEvaluation(False, decision_reason="instant_status_conflict")
        if terminal.outcome == "completed":
            return CommandTerminalEvidenceEvaluation(True, terminal, decision_reason="instant_readback_verified")
        return CommandTerminalEvidenceEvaluation(False, failure_projection=terminal,
                                                 decision_reason="instant_" + terminal.outcome + "_observed")
