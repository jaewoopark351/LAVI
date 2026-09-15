#20260915_kpopmodder: Build bounded wire fixtures independently from production slot decoding.
from types import SimpleNamespace

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from tests.minecraft_chatclef.find.fixtures import descriptor


def context(source="lavi_chat_ui", text="다이아 바지 장착해 줘"):
    return SimpleNamespace(request_id="equip-request", session_id="equip-session", generation=1,
                           event_id="a" * 32, descriptor=descriptor(source, text))


def data(ctx, outcome="satisfied", *, targets=None):
    if targets is None:
        targets = [{"index": 0, "native_target": "diamond_leggings", "requested_count": 1,
                    "matches": [{"item_id": "minecraft:diamond_leggings", "slot": "legs"}]}]
    slots = ("head", "chest", "legs", "feet", "offhand")
    before = {slot: ("minecraft:air", 0) for slot in slots}
    after = dict(before)
    for index, target in enumerate(targets):
        item = target["matches"][-1]
        if outcome in {"satisfied", "already_satisfied"} or (outcome == "partial" and index == 0):
            after[item["slot"]] = (item["item_id"], 1)
        if outcome == "already_satisfied":
            before[item["slot"]] = (item["item_id"], 1)
    def observed(items, tick):
        return {"available": True, "reason": "available", "observed_at_ms": tick,
                "slots": [{"slot": slot, "item_id": items[slot][0], "count": items[slot][1]} for slot in slots]}
    def flags(items):
        return [any(items[match["slot"]][0] == match["item_id"] and items[match["slot"]][1] > 0
                    for match in target["matches"]) for target in targets]
    task = "01234"
    reasons = {"satisfied": "all_targets_equipped", "already_satisfied": "already_equipped",
               "partial": "some_targets_not_equipped", "not_satisfied": "no_targets_equipped"}
    return {"result_reason": "matching_task_finished", "result_fidelity": "callback_plus_matching_user_task_event",
            "evidence_sequence": 1, "dispatch_returned": True, "finish_callback_received": True,
            "task_finished_event_received": True, "failure_type": "",
            "bound_root_task": {"identity": task, "available": True, "class_name": "adris.altoclef.tasks.misc.EquipArmorTask"},
            "ownership": {"connection_generation": ctx.generation, "request_id": ctx.request_id,
                          "session_id": ctx.session_id, "detached": False},
            "effect_kind": "equip_slots", "effect_profile_id": "equip_armor_slots_v1", "effect_profile_version": 1,
            "effect_payload": {"command": ctx.descriptor.command.lstrip("@").strip(), "request_id": ctx.request_id,
                "session_id": ctx.session_id, "server_connection_generation": ctx.generation,
                "java_socket_generation": 1, "task_identity": task,
                "observation_source": "minecraft_client_equipment_slots",
                "quantity_semantics": "all_targets_any_match_slot_presence", "targets": targets,
                "before": observed(before, 100), "after": observed(after, 101), "binding_valid": True,
                "effect_observation_status": outcome, "effect_observation_reason": reasons[outcome],
                "before_satisfied": flags(before), "after_satisfied": flags(after)}}


def result(ctx, payload):
    return CommandResultDTO(request_id=ctx.request_id, ok=True, status="completed", data=payload)
