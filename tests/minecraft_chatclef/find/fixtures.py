#20260914_kpopmodder: Build bounded runtime catalogs and production FIND descriptors for tests.
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.intent import ChatClefNaturalLanguageService
from plugins.Minecraft.fabric.chatclef.transport.find_catalog import FindCatalogRecord, FindCatalogSnapshot
from plugins.Minecraft.fabric.chatclef.transport.find_catalog.find_catalog_snapshot import catalog_digest
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import CommandFeedbackDescriptorFactory


def records():
    return (
        FindCatalogRecord("entity", "minecraft:villager", "entity.minecraft.villager", "주민", "Villager", "mob"),
        FindCatalogRecord("entity", "minecraft:zombie", "entity.minecraft.zombie", "좀비", "Zombie", "mob"),
        FindCatalogRecord("entity", "example:sky_beast", "entity.example.sky_beast", "하늘 짐승", "Sky Beast", "unknown"),
        FindCatalogRecord("entity", "minecraft:arrow", "entity.minecraft.arrow", "화살", "Arrow", "non_mob"),
        FindCatalogRecord("block", "minecraft:chest", "block.minecraft.chest", "상자", "Chest"),
        FindCatalogRecord("item", "minecraft:chest", "block.minecraft.chest", "상자", "Chest"),
        FindCatalogRecord("item", "minecraft:diamond", "item.minecraft.diamond", "다이아몬드", "Diamond"),
    )


def snapshot():
    current = records()
    return FindCatalogSnapshot("find-session", 1, 3, catalog_digest(current), current)


def translation(text="마을 주민 찾아줘"):
    return ChatClefNaturalLanguageService(find_catalog_provider=snapshot).translate(text).to_dict()


def descriptor(source="lavi_chat_ui", text="마을 주민 찾아줘"):
    return CommandFeedbackDescriptorFactory().from_trusted_translation(
        event=SimpleNamespace(source=source, final=True, event_id="a" * 32, provider_id=source,
                              event_kind="chat_submit" if source == "lavi_chat_ui" else "final_transcript"),
        translation=translation(text),
    )


def context(source="lavi_chat_ui", text="마을 주민 찾아줘"):
    return SimpleNamespace(descriptor=descriptor(source, text), request_id="find-request",
        command_message_id="find-message", session_id="find-session", generation=1)


def data(result="FOUND_AND_REPORTED", *, kind="entity", target="minecraft:villager", mode="report"):
    payload = dict(target_kind=kind, completion_mode="LOCATE_AND_REPORT" if mode == "report" else "LOCATE_AND_APPROACH",
        observation_scope={"entity": "loaded_entities", "block": "loaded_blocks", "item": "loaded_dropped_items", "player": "loaded_players"}[kind],
        find_result=result, find_satisfied=result in {"FOUND_AND_REPORTED", "FOUND_AND_IN_SAFE_RANGE", "ALREADY_IN_SAFE_RANGE"}, reason={
            "FOUND_AND_REPORTED": "complete_loaded_scope_candidate_revalidated",
            "FOUND_AND_IN_SAFE_RANGE": "same_target_safe_range_and_owned_cleanup",
            "ALREADY_IN_SAFE_RANGE": "same_target_safe_range_and_owned_cleanup",
            "NOT_OBSERVED_IN_LOADED_SCOPE": "complete_loaded_scope_no_match",
            "OBSERVATION_BOUNDS_EXHAUSTED": "elapsed_budget_exhausted",
            "TARGET_LOST": "selected_candidate_not_revalidated",
            "CANDIDATE_NOT_REVALIDATABLE": "selected_identity_unverifiable",
            "INVALID_TARGET": "invalid_target",
            "INTERNAL_ERROR": "observation_read_or_revalidation_failed",
            "INTERRUPTED": "catalog_resource_binding_changed",
            "UNREACHABLE": "exploration_route_unavailable",
            "TIMEOUT": "parent_deadline_exhausted",
        }.get(result, "unknown_fixture_outcome"))
    if kind == "player":
        import hashlib
        payload["player_identity_digest"] = hashlib.sha256(target.encode("utf-8")).hexdigest()
    else:
        current = snapshot()
        payload.update(canonical_target_id=target, catalog_digest=current.catalog_digest, resource_generation=current.resource_generation)
    if payload["find_satisfied"]:
        payload.update(candidate_identity_digest="b" * 64, dimension="minecraft:overworld", x=-2, y=64, z=5)
        if mode == "approach":
            payload["safe_distance_satisfied"] = True
    return dict(result_reason="matching_task_finished", result_fidelity="callback_plus_matching_user_task_event",
        effect_profile_id="fabric_chatclef_find_observation" if mode == "report" else "fabric_chatclef_find_approach",
        effect_profile_version=1, effect_kind="find_observation" if mode == "report" else "find_approach", effect_payload=payload)
