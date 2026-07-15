#20260707_kpopmodder: StarCraft2 event callback builder for TTS reactions.
from __future__ import annotations
from typing import Any, Dict
from core.logger import log_print
from .sc2_telemetry_registry import (
    SC2_BUILDING_UNIT_TYPE_IDS,
    unit_speak_name,
    upgrade_speak_name,
)
from .starcraft2_reaction_io import (
    StarCraft2ReactionMemoryRecorder,
    StarCraft2ReactionTTSAdapter,
)
from .starcraft2_reaction_policy import StarCraft2ReactionPolicy
from .starcraft2_contracts import StarCraft2Event


class StarCraft2ReactionRuntime:
    #20260713_kpopmodder: Coordinate SC2 lifecycle and policy filtering while
    # side effects stay behind small reaction I/O adapters.
    TERMINAL_EVENT_TYPES = {
        "game_started",
        "game_ended",
        "game_won",
        "game_lost",
        "engine_error",
        "error",
    }

    def __init__(
        self,
        llm,
        tts,
        memory_store=None,
        policy: StarCraft2ReactionPolicy | None = None,
        post_game_suppression: bool = False,
        terminal_cancel_reason: str | None = None,
        memory_recorder: StarCraft2ReactionMemoryRecorder | None = None,
        tts_adapter: StarCraft2ReactionTTSAdapter | None = None,
    ):
        self.llm = llm
        self.tts = tts
        self.memory_store = memory_store
        self.policy = policy
        self.post_game_suppression = bool(post_game_suppression)
        self.terminal_cancel_reason = terminal_cancel_reason
        self.memory_recorder = memory_recorder or StarCraft2ReactionMemoryRecorder(
            memory_store
        )
        self.tts_adapter = tts_adapter or StarCraft2ReactionTTSAdapter(
            tts,
            terminal_cancel_reason=terminal_cancel_reason,
        )
        self._game_active = False
        self._game_end_cancelled = False
        self._suppress_post_game_tts = False

    def handle_status_event(self, event: Dict[str, Any]) -> bool:
        #20260715_kpopmodder: Preserve the legacy EventBus callback while the
        # reaction core uses the typed event contract.
        return self.handle_event(StarCraft2Event.from_mapping(event))

    def handle_event(self, event: StarCraft2Event) -> bool:
        normalized = StarCraft2Event.from_mapping(event)
        event_type = str(normalized.event_type or "").strip().lower()
        details = dict(normalized.details)

        log_print(f"[StarCraft2Reaction] event={event_type}")
        self.memory_recorder.store_event(normalized)
        self._update_game_state(event_type, details)

        if self.post_game_suppression and self._suppress_post_game_tts and (
            event_type not in self.TERMINAL_EVENT_TYPES
        ):
            suppressed_details = dict(details)
            suppressed_details["speak"] = False
            normalized = StarCraft2Event(
                event_type=normalized.event_type,
                details=suppressed_details,
                source=normalized.source,
                engine=normalized.engine,
                time=normalized.time,
            )
            log_print(
                "[StarCraft2ReactionRuntime] post-game TTS suppressed: "
                f"event={event_type}"
            )

        if self.policy is not None and not self.policy.should_emit(normalized):
            return False
        text = build_starcraft2_reaction_text(normalized)
        if not text:
            text = str(normalized.details.get("message") or "").strip()
        return self.tts_adapter.speak(text)

    def _update_game_state(self, event_type: str, event_details: Dict[str, Any]) -> None:
        if event_type == "game_started":
            self._game_active = True
            self._game_end_cancelled = False
            self._suppress_post_game_tts = False
            return
        if event_type in self.TERMINAL_EVENT_TYPES:
            self._game_active = False
            if self.post_game_suppression:
                self._suppress_post_game_tts = True
            if not self._game_end_cancelled:
                self.tts_adapter.cancel_pending(event_type, event_details)
                self._game_end_cancelled = True

    @staticmethod
    def _details(details: Any) -> Dict[str, Any]:
        return details if isinstance(details, dict) else {}


def build_starcraft2_status_event_callback(
    llm,
    tts,
    memory_store=None,
    terminal_cancel_reason: str | None = None,
):
    runtime = StarCraft2ReactionRuntime(
        llm,
        tts,
        memory_store=memory_store,
        policy=StarCraft2ReactionPolicy(),
        post_game_suppression=True,
        terminal_cancel_reason=terminal_cancel_reason,
    )
    return runtime.handle_status_event


def handle_starcraft2_status_event(llm, tts, memory_store, policy, event):
    runtime = StarCraft2ReactionRuntime(
        llm,
        tts,
        memory_store=memory_store,
        policy=policy,
        post_game_suppression=False,
    )
    return runtime.handle_event(StarCraft2Event.from_mapping(event))

def build_starcraft2_reaction_text(event: StarCraft2Event) -> str:
    normalized = StarCraft2Event.from_mapping(event)
    kind = str(normalized.event_type or "").lower()
    details = dict(normalized.details)
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
