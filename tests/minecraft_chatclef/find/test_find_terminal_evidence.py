#20260914_kpopmodder: FIND completion is typed, bound, and fail-closed rather than callback-only.
from copy import deepcopy
from types import SimpleNamespace
import pytest
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.fabric.chatclef.result.find import FindTerminalPayload
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle.terminal.find.korean_find_terminal_renderer import KoreanFindTerminalRenderer
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.evidence.command_terminal_evidence_evaluator import CommandTerminalEvidenceEvaluator
from .fixtures import descriptor, terminal_data


def evaluate(data, status="completed", source="lavi_chat_ui", request_id="request-find", context_id="request-find"):
    result = CommandResultDTO(request_id=request_id, status=status, ok=status == "completed",
                              error_code="internal_error" if status == "failed" else None, data=data)
    return CommandTerminalEvidenceEvaluator().evaluate(result, context=SimpleNamespace(descriptor=descriptor(source), request_id=context_id))

@pytest.mark.parametrize("source", ("lavi_chat_ui", "voice_input_final"))
def test_arrival_is_verified_for_both_sources(source):
    result = evaluate(terminal_data(), source=source)
    assert result.verified
    assert result.projection.code == "ARRIVED"
    assert KoreanFindTerminalRenderer().render(result.projection) == "주민 근처에 도착했어. 대상 좌표 10, 64, -20."

@pytest.mark.parametrize("code", sorted(FindTerminalPayload.FAILURE))
def test_failures_are_typed_and_not_success(code):
    result = evaluate(terminal_data(code), status="failed")
    assert not result.verified
    assert result.failure_projection is not None, (code, result)
    assert result.failure_projection.code == code
    assert KoreanFindTerminalRenderer().render(result.failure_projection)

@pytest.mark.parametrize("field, bad", (
    ("schema_version", True), ("schema_version", 2), ("operation_id", "fake"),
    ("mode", "report"), ("code", "completed"), ("kind", "auto"), ("registry_id", "villager"),
    ("position", [10,64]), ("position", [True,64,1]), ("position", [0,2**31,0]),
    ("radius", 65), ("radius", True), ("scanned", 4097), ("scanned", -1),
    ("scan_complete", False), ("observed", False), ("arrived", False),
    ("entity_uuid", ""), ("entity_uuid", "foreign"), ("label", ""),
    ("dimension", "overworld"), ("language_warnings", -1), ("scope", "global"),
    ("suggestions", ["entity mod:a"]), ("label", "주민\n도착했어"),
))
def test_payload_corruption_never_verifies(field, bad):
    data = terminal_data(); data["find"][field] = bad
    assert FindTerminalPayload.from_data(data) is None
    assert not evaluate(data).verified

@pytest.mark.parametrize("edit", (
    lambda d: d.pop("find"), lambda d: d["find"].update(extra=True),
    lambda d: d.update(result_reason="callback_completed_without_user_task"),
    lambda d: d.update(result_fidelity="dispatch_started_only"),
    lambda d: d.update(goto_terminal={}), lambda d: d.update(operation="attack"),
    lambda d: d["find"].update(query="좀비"),
))
def test_missing_conflicting_and_foreign_evidence_is_rejected(edit):
    data=terminal_data(); edit(data)
    result=evaluate(data)
    assert not result.verified
    assert result.failure_projection is None


def test_request_owner_mismatch_is_rejected():
    assert not evaluate(terminal_data(), context_id="other").verified


def test_failure_cannot_be_promoted_by_completed_status():
    result=evaluate(terminal_data("NOT_FOUND"))
    assert not result.verified
    assert result.failure_projection is None


def test_success_cannot_be_published_for_failed_status():
    assert not evaluate(terminal_data(), status="failed").verified


def test_report_evidence_and_block_outcome_decode_without_entity_uuid():
    value=FindTerminalPayload.from_data(terminal_data("FOUND", kind="block", requested_kind="block", query="minecraft:chest", mode="report"))
    assert value is not None
    assert "이동하지 않았어" in KoreanFindTerminalRenderer().render(value)


def test_ambiguous_target_requires_multiple_valid_id_candidates():
    data=terminal_data("AMBIGUOUS_TARGET"); data["find"]["suggestions"]=["entity mod:a"]
    assert FindTerminalPayload.from_data(data) is None


def test_freeform_message_is_not_completion_evidence():
    data={"message":"주민 근처에 도착했어", "result_reason":"matching_task_finished", "result_fidelity":"callback_plus_matching_user_task_event"}
    assert not evaluate(data).verified
