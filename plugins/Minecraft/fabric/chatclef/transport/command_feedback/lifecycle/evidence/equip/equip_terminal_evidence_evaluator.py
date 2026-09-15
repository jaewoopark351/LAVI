#20260915_kpopmodder: Bind decoded equipment observations to the existing request and natural terminal proof.
from collections.abc import Mapping

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.result.equip import EquipEffectPayloadDecoder
from ..command_terminal_evidence_evaluation import CommandTerminalEvidenceEvaluation


class EquipTerminalEvidenceEvaluator:
    def evaluate(self, result, *, data, context, profile):
        descriptor = getattr(context, "descriptor", None)
        if (type(result) is not CommandResultDTO or not isinstance(data, Mapping)
                or result.status is not CommandResultStatus.COMPLETED or result.ok is not True
                or result.error_code is not None or not result.request_id
                or result.request_id != getattr(context, "request_id", None)
                or data.get("result_reason") != "matching_task_finished"
                or data.get("result_fidelity") != "callback_plus_matching_user_task_event"
                or any(data.get(key) is not True for key in (
                    "dispatch_returned", "finish_callback_received", "task_finished_event_received"))
                or data.get("failure_type") != ""
                or getattr(descriptor, "command_name", None) != "equip"
                or getattr(descriptor, "evidence_profile_id", None) != "equip_terminal_evidence_v1"
                or getattr(profile, "profile_id", None) != "equip_terminal_evidence_v1"
                or getattr(profile, "success_evaluator_id", None) != "equip_slots"
                or any(key in data for key in ("request_kind", "control_outcome", "goto_result",
                    "goto_terminal", "goto_binding", "find", "store_home_result", "instant_command"))):
            return self._reject("equip_context_or_lifecycle_mismatch")
        payload = EquipEffectPayloadDecoder.decode(data)
        if payload is None:
            return self._reject("equip_payload_unavailable_or_invalid")
        root = data.get("bound_root_task")
        ownership = data.get("ownership")
        if (payload.command != getattr(descriptor, "command", "").lstrip("@").strip()
                or payload.request_id != result.request_id
                or payload.session_id != getattr(context, "session_id", None)
                or payload.server_connection_generation != getattr(context, "generation", None)
                or type(getattr(context, "generation", None)) is not int
                or not isinstance(ownership, Mapping)
                or type(ownership.get("connection_generation")) is not int
                or ownership["connection_generation"] != payload.server_connection_generation
                or ownership.get("request_id") != payload.request_id
                or ownership.get("session_id") != payload.session_id or ownership.get("detached") is not False
                or not isinstance(root, Mapping) or root.get("identity") != payload.task_identity
                or root.get("available") is not True
                or root.get("class_name") != "adris.altoclef.tasks.misc.EquipArmorTask"):
            return self._reject("equip_request_or_task_binding_mismatch")
        if not self._targets_match(payload, descriptor):
            return self._reject("equip_requested_targets_mismatch")
        if payload.satisfied:
            return CommandTerminalEvidenceEvaluation(True, payload, decision_reason="equip_" + payload.outcome)
        return CommandTerminalEvidenceEvaluation(False, failure_projection=payload,
                                                 decision_reason="equip_" + payload.outcome)

    @staticmethod
    def _reject(reason):
        return CommandTerminalEvidenceEvaluation(False, decision_reason=reason)

    @staticmethod
    def _targets_match(payload, descriptor):
        # ItemList merges identical native tokens and emits HashMap order, not input order.
        material = payload.command.removeprefix("equip ").lower()
        if material in {"leather", "iron", "gold", "diamond", "netherite"}:
            prefix = "golden" if material == "gold" else material
            expected = {f"minecraft:{prefix}_{part}": 1 for part in ("helmet", "chestplate", "leggings", "boots")}
        else:
            expected = {}
            for target in getattr(descriptor, "targets", ()):
                name, count = target.canonical_target, target.requested_count
                expected[name] = expected.get(name, 0) + count
            # Existing single-item descriptors predate the typed target-list field.
            if not expected:
                name = getattr(descriptor, "target_item", None)
                count = getattr(descriptor, "requested_count", None)
                if type(name) is str and name and type(count) is int and count > 0:
                    expected[name] = count
            if not expected or any(count > 2147483647 for count in expected.values()):
                return False
        actual = {target[3]: target[1] for target in payload.targets}
        return len(actual) == len(payload.targets) and actual == expected
