#20260914_kpopmodder: Verify FIND meaning separately from completed query satisfaction.
from __future__ import annotations

from typing import Mapping

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.diagnostics import FabricChatClefDiagnostics
from plugins.Minecraft.fabric.chatclef.result.find import FindCommandBinding, FindTerminalPayload
from plugins.Minecraft.fabric.chatclef.result.find.find_terminal_payload import FAILURES, FOUND
from plugins.Minecraft.fabric.chatclef.transport.find_catalog import FindCatalogSnapshot

from ..command_terminal_evidence_evaluation import CommandTerminalEvidenceEvaluation


class FindTerminalEvidenceEvaluator:
    _FOREIGN = frozenset({"request_kind", "control_outcome", "control_reason", "stop_command_invoked",
        "store_home_result", "goto_terminal", "goto_binding", "target_item", "requested_count", "target_count_delta"})

    def __init__(self, *, catalog_provider=None, diagnostics=None):
        self._catalog_provider = catalog_provider or (lambda: None)
        self._diagnostics = diagnostics or FabricChatClefDiagnostics()

    def evaluate(self, result, *, data, context, profile):
        def reject(reason):
            return self._reject(reason, getattr(result, "request_id", "unbound"))

        descriptor = getattr(context, "descriptor", None)
        binding = getattr(descriptor, "find_binding", None)
        if (type(result) is not CommandResultDTO or not isinstance(data, Mapping)
                or result.request_id != getattr(context, "request_id", None)
                or data.get("result_reason") != "matching_task_finished"
                or data.get("result_fidelity") != "callback_plus_matching_user_task_event"
                or any(key in data for key in self._FOREIGN)
                or getattr(descriptor, "command_name", None) != "find"
                or getattr(descriptor, "evidence_profile_id", None) != "find_terminal_evidence_v1"
                or getattr(descriptor, "rollout_state", None) != "verified"
                or getattr(profile, "command_name", None) != "find"
                or getattr(profile, "profile_id", None) != "find_terminal_evidence_v1"
                or getattr(profile, "rollout_state", None) != "verified"
                or getattr(profile, "response_lifecycle_kind", None) != "finite_task"
                or getattr(profile, "success_evaluator_id", None) != "find_terminal"
                or type(binding) is not FindCommandBinding):
            return reject("find_identity_or_profile_invalid")
        expected = FindCommandBinding.from_command(getattr(descriptor, "command", None))
        if expected is None or (expected.target_kind, expected.target, expected.mode) != (binding.target_kind, binding.target, binding.mode):
            return reject("find_descriptor_command_mismatch")
        terminal = FindTerminalPayload.from_data(data)
        if terminal is None or (terminal.target_kind, terminal.mode) != (binding.target_kind, binding.mode):
            return reject("find_payload_invalid")
        fields = terminal.fields
        if binding.target_kind == "player":
            if fields["player_identity_digest"] != binding.player_identity_digest:
                return reject("find_player_binding_mismatch")
        else:
            if fields["canonical_target_id"] != binding.target:
                return reject("find_target_binding_mismatch")
            if binding.catalog_digest:
                if (fields["catalog_digest"] != binding.catalog_digest or fields["resource_generation"] != binding.resource_generation
                        or getattr(context, "session_id", None) != binding.session_id
                        or getattr(context, "generation", None) != binding.connection_generation):
                    return reject("find_catalog_binding_mismatch")
            else:
                snapshot = self._catalog_provider()
                if (type(snapshot) is not FindCatalogSnapshot
                        or snapshot.session_id != getattr(context, "session_id", None)
                        or snapshot.connection_generation != getattr(context, "generation", None)
                        or snapshot.catalog_digest != fields["catalog_digest"]
                        or snapshot.resource_generation != fields["resource_generation"]
                        or not any(record.target_kind == binding.target_kind and record.canonical_target_id == binding.target
                            and (record.target_kind != "entity" or record.eligibility != "non_mob")
                            for record in snapshot.records)):
                    return reject("find_raw_catalog_unverified")
        if terminal.find_result in FAILURES:
            if result.status is not CommandResultStatus.FAILED or result.ok is not False or result.error_code is not BridgeErrorCode.INTERNAL_ERROR:
                return reject("find_failure_status_conflict")
            self._log("failure_verified", terminal.find_result, result.request_id)
            return CommandTerminalEvidenceEvaluation(False, failure_projection=terminal, decision_reason="find_failure_verified")
        if result.status is not CommandResultStatus.COMPLETED or result.ok is not True or result.error_code is not None:
            return reject("find_query_status_conflict")
        if terminal.find_result in FOUND:
            self._log("found_verified", terminal.find_result, result.request_id)
            return CommandTerminalEvidenceEvaluation(True, terminal, decision_reason="find_found_verified")
        self._log("query_verified", terminal.find_result, result.request_id)
        return CommandTerminalEvidenceEvaluation(False, query_projection=terminal, decision_reason="find_query_miss_verified")

    def _reject(self, reason, request):
        self._log("rejected", reason, request)
        return CommandTerminalEvidenceEvaluation(False, decision_reason=reason)

    def _log(self, event, reason, request):
        try:
            # Request IDs are correlation only; omit target names and UUIDs.
            safe_request = str(request).replace("\n", "").replace("\r", "")[:128]
            self._diagnostics.info(f"FIND_EVIDENCE event={event} reason={reason} request={safe_request}")
        except Exception:
            pass
