#20260915_kpopmodder: Verify malformed, stale and contradictory equipment observations never become success.
from copy import deepcopy
from dataclasses import replace
import pytest

from plugins.Minecraft.fabric.chatclef.result.equip import EquipEffectPayloadDecoder
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.evidence import CommandTerminalEvidenceEvaluator
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.descriptor.command_feedback_target import CommandFeedbackTarget
from .fixtures import context, data, result


@pytest.mark.parametrize("outcome", ("satisfied", "already_satisfied", "not_satisfied"))
def test_natural_completion_and_slot_effect_are_distinct(outcome):
    ctx = context()
    observation = CommandTerminalEvidenceEvaluator().evaluate(result(ctx, data(ctx, outcome)), context=ctx)
    assert observation.verified == (outcome != "not_satisfied")
    projection = observation.projection or observation.failure_projection
    assert projection is not None and projection.outcome == outcome


@pytest.mark.parametrize("field,value", (
    ("command", "equip iron_leggings"), ("request_id", "another-request"), ("session_id", "another-session"),
    ("server_connection_generation", 2), ("java_socket_generation", True), ("task_identity", "foreign-root"),
    ("observation_source", "inventory"), ("quantity_semantics", "requested_count"), ("binding_valid", False),
    ("effect_observation_status", "already_satisfied"), ("effect_observation_reason", "no_targets_equipped"),
    ("before_satisfied", [True]), ("after_satisfied", [False]), ("after_satisfied", [1]),
))
def test_forged_or_inconsistent_payload_is_unverified(field, value):
    ctx = context()
    wire = data(ctx)
    wire["effect_payload"][field] = value
    evaluation = CommandTerminalEvidenceEvaluator().evaluate(result(ctx, wire), context=ctx)
    assert not evaluation.verified and evaluation.failure_projection is None


@pytest.mark.parametrize("mutation", ("missing", "extra", "version_bool", "callback_only", "different_task",
                                     "count", "native_target", "unavailable", "slot_count_bool", "air_with_count",
                                     "duplicate_slot", "reverse_time", "unknown_snapshot_reason", "too_many_targets"))
def test_shape_and_terminal_proof_fail_closed(mutation):
    ctx = context()
    wire = data(ctx)
    p = wire["effect_payload"]
    if mutation == "missing":
        del wire["effect_payload"]
    elif mutation == "extra":
        p["extra"] = True
    elif mutation == "version_bool":
        wire["effect_profile_version"] = True
    elif mutation == "callback_only":
        wire["result_fidelity"] = "callback_only"
    elif mutation == "different_task":
        wire["bound_root_task"]["identity"] = "other"
    elif mutation == "count":
        p["targets"][0]["requested_count"] = 2
    elif mutation == "native_target":
        p["targets"][0]["native_target"] = "iron_leggings"
    elif mutation == "unavailable":
        p["before"]["available"] = False
    elif mutation == "slot_count_bool":
        p["after"]["slots"][2]["count"] = True
    elif mutation == "air_with_count":
        p["after"]["slots"][2]["item_id"] = "minecraft:air"
    elif mutation == "duplicate_slot":
        p["after"]["slots"][0] = dict(p["after"]["slots"][2])
    elif mutation == "reverse_time":
        p["after"]["observed_at_ms"] = 99
    elif mutation == "unknown_snapshot_reason":
        p["after"]["reason"] = "unavailable"
    else:
        p["targets"] = p["targets"] * 33
    evaluation = CommandTerminalEvidenceEvaluator().evaluate(result(ctx, wire), context=ctx)
    assert not evaluation.verified and evaluation.failure_projection is None


def test_target_order_is_irrelevant_and_alias_candidates_are_or():
    ctx = context()
    ctx.descriptor = replace(ctx.descriptor, command="equip [helmet 1, diamond_leggings 1]", targets=(
        CommandFeedbackTarget("helmet", 1, "requested_count", "투구"),
        CommandFeedbackTarget("diamond_leggings", 1, "requested_count", "다이아 바지")))
    targets = [{"index": 0, "native_target": "diamond_leggings", "requested_count": 1,
                "matches": [{"item_id": "minecraft:diamond_leggings", "slot": "legs"}]},
               {"index": 1, "native_target": "helmet", "requested_count": 1,
                "matches": [{"item_id": "minecraft:iron_helmet", "slot": "head"},
                            {"item_id": "minecraft:diamond_helmet", "slot": "head"}]}]
    wire = data(ctx, targets=targets)
    evaluation = CommandTerminalEvidenceEvaluator().evaluate(result(ctx, wire), context=ctx)
    assert evaluation.verified
    wire = data(ctx, "partial", targets=targets)
    evaluation = CommandTerminalEvidenceEvaluator().evaluate(result(ctx, wire), context=ctx)
    assert not evaluation.verified and evaluation.failure_projection.outcome == "partial"


@pytest.mark.parametrize("uppercase", (False, True))
def test_material_set_expands_all_four_native_targets(uppercase):
    ctx = context(text="다이아 방어구 세트 장착해 줘")
    if uppercase:
        ctx.descriptor = replace(ctx.descriptor, command="equip DIAMOND")
    targets = [{"index": index, "native_target": f"minecraft:diamond_{part}", "requested_count": 1,
                "matches": [{"item_id": f"minecraft:diamond_{part}", "slot": slot}]}
               for index, (part, slot) in enumerate((("boots", "feet"), ("helmet", "head"),
                                                     ("leggings", "legs"), ("chestplate", "chest")))]
    evaluation = CommandTerminalEvidenceEvaluator().evaluate(result(ctx, data(ctx, targets=targets)), context=ctx)
    assert evaluation.verified


def test_payload_freezes_nested_wire_objects():
    ctx = context()
    wire = data(ctx)
    decoded = EquipEffectPayloadDecoder.decode(wire)
    assert decoded is not None
    original = deepcopy(decoded.after_slots)
    wire["effect_payload"]["after"]["slots"][2]["count"] = 0
    assert decoded.after_slots == original
    with pytest.raises(AttributeError):
        decoded.outcome = "other"


def test_duplicate_native_tokens_use_merged_count_without_claiming_two_worn_items():
    ctx = context()
    target = CommandFeedbackTarget("diamond_leggings", 1, "requested_count", "다이아 바지")
    ctx.descriptor = replace(ctx.descriptor, targets=(target, target), command="equip [diamond_leggings 1, diamond_leggings 1]")
    wire = data(ctx)
    wire["effect_payload"]["targets"][0]["requested_count"] = 2
    assert CommandTerminalEvidenceEvaluator().evaluate(result(ctx, wire), context=ctx).verified


def test_changed_snapshot_only_claims_current_equipment_even_if_targets_were_already_satisfied():
    ctx = context()
    wire = data(ctx, "already_satisfied")
    p = wire["effect_payload"]
    p["after"]["slots"][4].update(item_id="minecraft:shield", count=1)
    p.update(effect_observation_status="satisfied", effect_observation_reason="all_targets_equipped")
    evaluation = CommandTerminalEvidenceEvaluator().evaluate(result(ctx, wire), context=ctx)
    assert evaluation.verified and evaluation.projection.outcome == "satisfied"


@pytest.mark.parametrize("path,value", (
    (("dispatch_returned",), False), (("finish_callback_received",), False),
    (("task_finished_event_received",), False), (("failure_type",), "CommandException"),
    (("ownership", "detached"), True), (("ownership", "request_id"), "foreign-request"),
    (("ownership", "session_id"), "foreign-session"), (("ownership", "connection_generation"), True),
    (("bound_root_task", "available"), False), (("bound_root_task", "class_name"), "IdleTask"),
))
def test_inner_lifecycle_and_binding_cannot_contradict_outer_terminal(path, value):
    ctx = context()
    wire = data(ctx)
    container = wire
    for key in path[:-1]:
        container = container[key]
    container[path[-1]] = value
    evaluation = CommandTerminalEvidenceEvaluator().evaluate(result(ctx, wire), context=ctx)
    assert not evaluation.verified and evaluation.failure_projection is None
