#20260707_kpopmodder: Added small rate-limit policy for StarCraft2 events before LAV/TTS reactions.
from __future__ import annotations

import time
from typing import Any, Dict

from .sc2_telemetry_registry import SC2_BUILDING_UNIT_TYPE_IDS


IMPORTANT_EVENT_TYPES = {
    "game_started",
    "first_supply",
    "supply_depot_started",
    "first_barracks",
    "barracks_started",
    "enemy_seen",
    "attack_started",
    "combat_started",
    "building_started",
    "unit_lost",
    "game_won",
    "game_lost",
    "game_ended",
    "engine_error",
    "error",
    "supply_blocked",
    "army_milestone",
    "unit_produced",
    "worker_produced",
    "situation_update",
    "upgrade",
    "upgrade_completed",
    "strategy",
}

#20260711_kpopmodder: Low-value lifecycle and coarse telemetry messages stay in
# diagnostics/raw-event storage but must not occupy the spoken TTS queue.
SILENT_TTS_EVENT_TYPES = {
    "game_started", "game_ended", "engine_error", "error", "situation_update",
    "army_milestone",
}

#20260711_kpopmodder: Zerg eggs and larvae are transient production state, so
# keep their telemetry in logs without announcing each one through TTS.
SILENT_TTS_TRANSIENT_ZERG_UNIT_IDS = {"103", "151"}

EVENT_ALIASES = {
    "supply_depot_started": "first_supply",
    "barracks_started": "first_barracks",
    "error": "engine_error",
    "attack_started": "combat_started",
    "attack": "combat_started",
    "rush": "combat_started",
    "enemy": "enemy_seen",
    "scout": "enemy_seen",
    "build": "building_started",
    "expand": "building_started",
    "train": "unit_produced",
    "observation": "situation_update",
    "result": "game_ended",
}


class StarCraft2ReactionPolicy:
    #20260707_kpopmodder: Avoid noisy frame-level chatter from SC2 bot events.
    def __init__(self, min_interval_sec: float = 8.0):
        self.min_interval_sec = max(float(min_interval_sec), 0.0)
        self._last_event_time_by_key = {}

    def should_emit(self, event: Dict[str, Any]) -> bool:
        event_type = self.normalized_event_type(event)
        details = event.get("details") if isinstance(event.get("details"), dict) else {}
        if details.get("speak") is False:
            return False
        if event_type in SILENT_TTS_EVENT_TYPES:
            return False
        if self._is_silent_transient_zerg_unit_event(event_type, event):
            return False
        if event_type not in IMPORTANT_EVENT_TYPES and event_type not in EVENT_ALIASES.values():
            return False

        key = self.event_key(event)
        now = time.time()
        last_time = float(self._last_event_time_by_key.get(key, 0.0) or 0.0)
        if now - last_time < self.min_interval_sec:
            return False
        self._last_event_time_by_key[key] = now
        return True

    def normalized_event_type(self, event: Dict[str, Any]) -> str:
        event_type = str((event or {}).get("event_type") or "").strip().lower()
        return EVENT_ALIASES.get(event_type, event_type)

    def event_key(self, event: Dict[str, Any]) -> str:
        event_type = self.normalized_event_type(event)
        details = event.get("details") if isinstance(event.get("details"), dict) else {}
        if event_type in {"building_started", "unit_produced", "worker_produced", "unit_lost"}:
            unit_type_id = str(details.get("unit_type_id") or "").strip()
            if not unit_type_id:
                allowed_ids = SC2_BUILDING_UNIT_TYPE_IDS if event_type == "building_started" else None
                unit_type_id = self._dominant_positive_unit_type_id(
                    event,
                    allowed_ids=allowed_ids,
                )
            if unit_type_id:
                return f"{event_type}:{unit_type_id}"
        if event_type in {"upgrade", "upgrade_completed"}:
            upgrade_key = str(
                details.get("upgrade_token")
                or details.get("upgrade_id")
                or details.get("message")
                or ""
            ).strip().lower()
            if upgrade_key:
                return f"{event_type}:{upgrade_key[:120]}"
        if event_type == "strategy":
            strategy_key = str(details.get("message") or details.get("raw_line") or "").strip().lower()
            if strategy_key:
                return f"{event_type}:{strategy_key[:160]}"
        if event_type in {"enemy_seen", "engine_error", "game_ended"}:
            detail = str((event or {}).get("details") or "")
            return f"{event_type}:{detail[:80]}"
        return event_type

    def _is_silent_transient_zerg_unit_event(self, event_type: str, event: Dict[str, Any]) -> bool:
        if event_type not in {"unit_produced", "unit_lost"}:
            return False
        details = event.get("details") if isinstance(event.get("details"), dict) else {}
        dominant_unit_type_id = str(details.get("unit_type_id") or "").strip()
        if not dominant_unit_type_id:
            dominant_unit_type_id = self._dominant_positive_unit_type_id(event)
        return dominant_unit_type_id in SILENT_TTS_TRANSIENT_ZERG_UNIT_IDS

    def _dominant_positive_unit_type_id(self, event: Dict[str, Any], allowed_ids=None) -> str:
        details = event.get("details") if isinstance(event.get("details"), dict) else {}
        changes = details.get("unit_changes") if isinstance(details.get("unit_changes"), dict) else {}
        candidates = []
        normalized_allowed_ids = (
            {str(value) for value in allowed_ids}
            if allowed_ids is not None
            else None
        )
        for unit_type_id, raw_delta in changes.items():
            normalized_unit_type_id = str(unit_type_id)
            if (
                normalized_allowed_ids is not None
                and normalized_unit_type_id not in normalized_allowed_ids
            ):
                continue
            try:
                delta = int(raw_delta or 0)
            except (TypeError, ValueError):
                continue
            if delta > 0:
                candidates.append((normalized_unit_type_id, delta))
        if not candidates:
            return ""
        return max(candidates, key=lambda item: item[1])[0]


def should_speak_starcraft2_event(event: Dict[str, Any], policy: StarCraft2ReactionPolicy | None = None) -> bool:
    return (policy or StarCraft2ReactionPolicy()).should_emit(event)
