#20260914_kpopmodder: Use strict production translators and complete typed terminal observations.
from types import SimpleNamespace
from uuid import UUID
from plugins.Minecraft.fabric.chatclef.intent.chatclef_natural_language_service import ChatClefNaturalLanguageService
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import CommandFeedbackDescriptorFactory


def descriptor(source="lavi_chat_ui", text="마을 주민 찾아줘"):
    event = SimpleNamespace(text=text, source=source, provider_id="VoiceInput" if source == "voice_input_final" else source,
                            event_kind="final_transcript" if source == "voice_input_final" else "chat_submit",
                            final=True, event_id="a" * 32)
    result = CommandFeedbackDescriptorFactory().from_trusted_translation(
        event=event, translation=ChatClefNaturalLanguageService().translate(text).to_dict())
    assert result is not None
    return result


def terminal_data(code="ARRIVED", query="마을 주민", kind="entity", requested_kind="auto", mode="approach"):
    success = code in {"ARRIVED", "FOUND"}
    unresolved = code in {"UNKNOWN_TARGET", "AMBIGUOUS_TARGET"}
    found = code not in {"UNKNOWN_TARGET", "AMBIGUOUS_TARGET", "NOT_FOUND", "SEARCH_LIMIT", "PLAYER_UNAVAILABLE"}
    payload = {"schema_version": 1, "operation_id": str(UUID(int=1)), "query": query,
               "requested_kind": requested_kind, "mode": mode, "code": code,
               "kind": requested_kind if unresolved else kind,
               "registry_id": "" if unresolved else ("minecraft:chest" if kind == "block" else "minecraft:villager"),
               "label": "" if unresolved else ("상자" if kind == "block" else "주민"),
               "dimension": "minecraft:overworld", "position": [10, 64, -20] if found else [],
               "radius": 32 if (requested_kind if unresolved else kind) == "block" else 64,
               "scanned": 0 if unresolved else 10,
               "scan_complete": not unresolved and code not in {"SEARCH_LIMIT", "PLAYER_UNAVAILABLE"},
               "observed": success, "arrived": code == "ARRIVED",
               "entity_uuid": str(UUID(int=2)) if found and kind != "block" else "",
               "language_warnings": 0,
               "suggestions": ["entity test:one", "block test:two"] if code == "AMBIGUOUS_TARGET" else [],
               "scope": "loaded_block_cube" if (requested_kind if unresolved else kind) == "block" else "loaded_entity_sphere"}
    return {"result_reason": "matching_task_finished", "result_fidelity": "callback_plus_matching_user_task_event", "find": payload}
