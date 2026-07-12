#20260707_kpopmodder: StarCraft2 event callback builder for TTS reactions.
from __future__ import annotations
import json
from typing import Any, Dict
from core.logger import log_print
from ..sc2_telemetry_registry import (
    SC2_BUILDING_UNIT_TYPE_IDS,
    unit_speak_name,
    upgrade_speak_name,
)
from .starcraft2_reaction_policy import StarCraft2ReactionPolicy

def build_starcraft2_status_event_callback(llm, tts, memory_store=None):
    policy = StarCraft2ReactionPolicy()
    return lambda event: handle_starcraft2_status_event(llm, tts, memory_store, policy, event)

def handle_starcraft2_status_event(llm, tts, memory_store, policy, event):
    event = dict(event or {})
    event_type = str(event.get("event_type") or "")
    log_print(f"[StarCraft2Reaction] event={event_type}")
    _store_starcraft2_raw_event(memory_store, event)
    if not policy.should_emit(event): return False
    text = build_starcraft2_reaction_text(event)
    receive_input = getattr(tts, "receive_input", None)
    if text and callable(receive_input): receive_input(text); return True
    return False

def build_starcraft2_reaction_text(event: Dict[str, Any]) -> str:
    kind = str(event.get("event_type") or "").lower()
    details = event.get("details") if isinstance(event.get("details"), dict) else {}
    snapshot = details.get("snapshot") if isinstance(details.get("snapshot"), dict) else {}
    changes = details.get("unit_changes") if isinstance(details.get("unit_changes"), dict) else {}
    provided_message = str(details.get("message") or "").strip()
    if kind == "game_started": return "내가 스타2 경기를 시작했어요. 초반 빌드부터 볼게요."
    if kind == "game_ended": return "스타2 경기가 끝났어요. 결과를 정리할게요."
    if kind == "building_started":
        name = _changed_unit_name(changes, building=True)
        count = _changed_count(details, changes)
        if count > 1:
            #20260711_kpopmodder: Use the generic counter for clearer plural TTS pronunciation.
            return f"내가 {name} {count}개 건설을 시작했어요."
        return f"내가 {name} 건설을 시작했어요."
    if kind == "enemy_seen": return "내가 적 유닛을 발견했어요."
    if kind == "enemy_destroyed": return "내가 교전 중 적 유닛을 모두 파괴했어요."
    if kind == "unit_lost":
        name = _changed_unit_name(changes)
        return f"내 {name} {max(_changed_count(details, changes), 1)}기를 잃었어요."
    if kind == "combat_started": return "내 병력이 적과 교전 중이에요."
    if kind == "supply_blocked": return "내 인구수가 막혔어요."
    if kind == "army_milestone": return f"내 병력이 {snapshot.get('army_count', 0)}기까지 늘었어요."
    if kind == "unit_produced": return _production_text(
        _changed_unit_name(changes),
        _changed_count(details, changes),
    )
    if kind == "worker_produced": return _production_text(
        _changed_unit_name(changes),
        _changed_count(details, changes),
    )
    if kind in {"upgrade", "strategy"}:
        return provided_message
    if kind == "upgrade_completed":
        upgrade_value = details.get("upgrade_token") or details.get("upgrade_id")
        name = upgrade_speak_name(upgrade_value, default="업그레이드")
        return f"내가 {name}{_object_particle(name)} 완료했어요."
    if kind == "situation_update": return _build_ai_situation_text(snapshot, "내 상황 업데이트입니다.")
    if kind == "game_won": return "내가 이번 경기를 이겼어요."
    if kind == "game_lost": return "내가 이번 경기를 졌어요."
    return ""

def _build_ai_situation_text(snapshot: Dict[str, Any], prefix: str) -> str:
    counts = snapshot.get("unit_type_counts") if isinstance(snapshot, dict) else {}
    parts = [
        f"{unit_speak_name(k, default='유닛')} {int(v)}기"
        for k, v in counts.items()
        if int(v or 0) > 0
    ] if isinstance(counts, dict) else []
    units = ", ".join(parts[:5]) if parts else f"전체 유닛 {snapshot.get('self_units', 0)}기"
    return (f"{prefix} 미네랄 {snapshot.get('minerals', 0)}, 가스 {snapshot.get('vespene', 0)}, "
            f"인구수 {snapshot.get('food_used', 0)} / {snapshot.get('food_cap', 0)}, 주요 전력은 {units}입니다.")

def _changed_unit_name(changes: Dict[str, Any], building: bool = False) -> str:
    candidates = [(str(k), int(v or 0)) for k, v in changes.items() if int(v or 0) > 0]
    if building:
        candidates = [
            item for item in candidates
            if item[0].isdigit() and int(item[0]) in SC2_BUILDING_UNIT_TYPE_IDS
        ]
    if not candidates:
        return "새 건물" if building else "유닛"
    unit_type_id = max(candidates, key=lambda item: item[1])[0]
    return unit_speak_name(unit_type_id, default="새 건물" if building else "유닛")

def _changed_count(details: Dict[str, Any], changes: Dict[str, Any]) -> int:
    try:
        explicit_count = int(details.get("count") or 0)
    except (TypeError, ValueError):
        explicit_count = 0
    positive_counts = []
    for value in changes.values():
        try:
            count = int(value or 0)
        except (TypeError, ValueError):
            continue
        if count > 0:
            positive_counts.append(count)
    return explicit_count or (max(positive_counts) if positive_counts else 1)

def _production_text(name: str, count: int = 1) -> str:
    if int(count or 0) > 1:
        #20260711_kpopmodder: Keep plural unit production natural for TTS with "개를".
        return f"내가 {name} {int(count)}개를 생산했어요."
    return f"내가 {name}{_object_particle(name)} 생산했어요."

def _object_particle(text: str) -> str:
    value = str(text or "").rstrip()
    if not value:
        return "을"
    last = ord(value[-1])
    if 0xAC00 <= last <= 0xD7A3:
        return "을" if (last - 0xAC00) % 28 else "를"
    return "을"

def _store_starcraft2_raw_event(memory_store, event: Dict[str, Any]) -> None:
    add_raw_event = getattr(memory_store, "add_raw_event", None) if memory_store else None
    if callable(add_raw_event):
        try: add_raw_event("starcraft2_game_event", json.dumps(event, ensure_ascii=False, default=str), source="starcraft2", metadata={"event_type": event.get("event_type", "")})
        except Exception as exc: log_print(f"[StarCraft2Reaction] raw event store failed: {exc}")
