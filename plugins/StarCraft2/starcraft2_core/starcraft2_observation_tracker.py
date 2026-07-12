#20260710_kpopmodder: Added a stateful AI telemetry tracker so SC2 TTS reacts
# only to meaningful ResponseObservation transitions, not every frame.
from __future__ import annotations

from typing import Any, Dict, Optional

from .sc2_telemetry_registry import (
    SC2_BUILDING_CATEGORY,
    SC2_UNIT_CATEGORY,
    canonical_unit_token,
    canonical_upgrade_token,
    unit_category,
)


class SC2ObservationTracker:
    def __init__(self):
        self.reset()

    def reset(self) -> None:
        self._last: Optional[Dict[str, int]] = None
        self._last_frame = -1

    def update(self, snapshot: Dict[str, Any]):
        if not isinstance(snapshot, dict):
            return []
        if int(snapshot.get("schema") or 0) != 1:
            return []
        if str(snapshot.get("role") or "").lower() != "ai":
            return []
        try:
            frame = int(snapshot.get("game_loop"))
        except (TypeError, ValueError):
            return []
        if frame <= self._last_frame:
            return []
        values = {
            key: self._int(snapshot.get(key))
            for key in (
                "minerals", "vespene", "food_used", "food_cap", "food_workers",
                "food_army", "army_count", "idle_workers", "self_units",
                "visible_enemy_units", "under_construction_units",
            )
        }
        raw_unit_types = snapshot.get("unit_type_counts")
        values["unit_type_counts"] = self._int_map(raw_unit_types)
        raw_under_construction_types = snapshot.get("under_construction_type_counts")
        has_typed_construction = isinstance(raw_under_construction_types, dict)
        values["under_construction_type_counts"] = self._int_map(
            raw_under_construction_types
        )
        values["upgrade_ids"] = sorted(self._id_set(snapshot.get("upgrade_ids")))
        previous = self._last
        self._last = values
        self._last_frame = frame
        if previous is None:
            return []

        unit_changes = self._count_changes(
            previous.get("unit_type_counts"),
            values.get("unit_type_counts"),
        )
        construction_changes = self._count_changes(
            previous.get("under_construction_type_counts"),
            values.get("under_construction_type_counts"),
        )
        events = []

        #20260711_kpopmodder: Emit every known type transition separately so
        # simultaneous units/buildings do not collapse into one generic event.
        started_building_ids = set()
        for unit_type_id, delta in self._positive_changes(construction_changes):
            if unit_category(unit_type_id) != SC2_BUILDING_CATEGORY:
                continue
            started_building_ids.add(unit_type_id)
            events.append(self._type_event(
                "building_started", snapshot, values, unit_type_id, delta,
                "AI가 새 건물 건설을 시작했어요.",
            ))
        for unit_type_id, delta in self._positive_changes(unit_changes):
            if unit_category(unit_type_id) != SC2_BUILDING_CATEGORY:
                continue
            if unit_type_id in started_building_ids:
                continue
            started_building_ids.add(unit_type_id)
            events.append(self._type_event(
                "building_started", snapshot, values, unit_type_id, delta,
                "AI가 새 건물 건설을 시작했어요.",
            ))

        if has_typed_construction:
            typed_building_ids = {
                str(unit_type_id)
                for unit_type_id, delta in self._positive_changes(construction_changes)
                if unit_category(unit_type_id) == SC2_BUILDING_CATEGORY
            }
            events = [
                event for event in events
                if event.get("event_type") != "building_started"
                or str(event.get("details", {}).get("unit_type_id", "")) in typed_building_ids
            ]

        if (
            not started_building_ids
            and not has_typed_construction
            and values["under_construction_units"] > previous["under_construction_units"]
        ):
            # Backward-compatible fallback for telemetry emitted by older binaries.
            events.append(self._event(
                "building_started", snapshot, values,
                message="AI가 새 건물 건설을 시작했어요.",
                unit_changes=unit_changes,
            ))

        for unit_type_id, delta in self._positive_changes(unit_changes):
            category = unit_category(unit_type_id)
            if category == SC2_BUILDING_CATEGORY:
                continue
            event_type = "unit_produced" if category == SC2_UNIT_CATEGORY else "unknown_unit_produced"
            events.append(self._type_event(
                event_type, snapshot, values, unit_type_id, delta,
                "AI가 유닛을 추가로 생산했어요.",
            ))

        for unit_type_id, delta in self._negative_changes(unit_changes):
            category = unit_category(unit_type_id)
            if category == SC2_BUILDING_CATEGORY:
                event_type = "building_lost"
            elif category == SC2_UNIT_CATEGORY:
                event_type = "unit_lost"
            else:
                event_type = "unknown_unit_lost"
            events.append(self._type_event(
                event_type, snapshot, values, unit_type_id, abs(delta),
                "AI 유닛 수가 감소했어요.",
            ))

        previous_upgrade_ids = set(previous.get("upgrade_ids") or [])
        for upgrade_id in values.get("upgrade_ids") or []:
            if upgrade_id in previous_upgrade_ids:
                continue
            upgrade_token = canonical_upgrade_token(upgrade_id)
            event_type = "upgrade_completed" if upgrade_token else "unknown_upgrade_completed"
            events.append(self._event(
                event_type, snapshot, values,
                message="AI가 업그레이드를 완료했어요.",
                upgrade_id=str(upgrade_id),
                upgrade_token=upgrade_token or "",
            ))

        if previous["visible_enemy_units"] == 0 and values["visible_enemy_units"] > 0:
            events.append(self._event(
                "enemy_seen", snapshot, values,
                message="AI가 시야에서 적 유닛을 발견했어요.",
            ))
        if previous["visible_enemy_units"] > 0 and values["visible_enemy_units"] == 0:
            events.append(self._event("enemy_destroyed", snapshot, values))
        if previous["visible_enemy_units"] == 0 and values["visible_enemy_units"] > 0 and values["army_count"] > 0:
            events.append(self._event("combat_started", snapshot, values))
        was_blocked = previous["food_cap"] > 0 and previous["food_used"] >= previous["food_cap"]
        is_blocked = values["food_cap"] > 0 and values["food_used"] >= values["food_cap"]
        if not was_blocked and is_blocked:
            events.append(self._event(
                "supply_blocked", snapshot, values,
                message="AI의 인구수가 막혔어요.",
            ))
        for threshold in (10, 20, 40):
            if previous["army_count"] < threshold <= values["army_count"]:
                events.append(self._event(
                    "army_milestone", snapshot, values,
                    message=f"AI 병력이 {threshold}기 규모에 도달했어요.",
                ))
                break
        if not events and any(
            values[key] != previous[key]
            for key in ("minerals", "vespene", "food_used", "food_cap",
                        "food_workers", "food_army", "army_count",
                        "idle_workers", "visible_enemy_units",
                        "under_construction_units")
        ):
            #20260710_kpopmodder: Forward compact state changes so TTS can
            # describe the current AI situation, not only milestone events.
            events.append(self._event("situation_update", snapshot, values))
        return events

    def _event(self, event_type, snapshot, values, message="", **details):
        return {
            "event_type": event_type,
            "details": {
                "message": message,
                "bot": snapshot.get("bot", ""),
                "game_loop": int(snapshot.get("game_loop") or 0),
                "snapshot": values,
                **details,
            },
        }

    def _type_event(self, event_type, snapshot, values, unit_type_id, count, message):
        token = canonical_unit_token(unit_type_id)
        return self._event(
            event_type, snapshot, values,
            message=message,
            unit_type_id=str(unit_type_id),
            unit_token=token or "",
            count=int(count),
            unit_changes={str(unit_type_id): int(count)},
        )

    @classmethod
    def _count_changes(cls, previous, current):
        previous = previous if isinstance(previous, dict) else {}
        current = current if isinstance(current, dict) else {}
        return {
            str(key): cls._int(current.get(key, 0)) - cls._int(previous.get(key, 0))
            for key in set(current) | set(previous)
            if cls._int(current.get(key, 0)) != cls._int(previous.get(key, 0))
        }

    @staticmethod
    def _positive_changes(changes):
        return sorted(
            ((str(key), int(value)) for key, value in changes.items() if int(value) > 0),
            key=lambda item: SC2ObservationTracker._type_sort_key(item[0]),
        )

    @staticmethod
    def _negative_changes(changes):
        return sorted(
            ((str(key), int(value)) for key, value in changes.items() if int(value) < 0),
            key=lambda item: SC2ObservationTracker._type_sort_key(item[0]),
        )

    @staticmethod
    def _type_sort_key(value):
        text = str(value)
        return (0, int(text)) if text.isdigit() else (1, text)

    @classmethod
    def _int_map(cls, value):
        return {
            str(key): cls._int(item)
            for key, item in value.items()
        } if isinstance(value, dict) else {}

    @classmethod
    def _id_set(cls, value):
        if not isinstance(value, (list, tuple, set)):
            return set()
        return {cls._int(item) for item in value if cls._int(item) > 0}

    @staticmethod
    def _int(value: Any) -> int:
        try:
            return int(value or 0)
        except (TypeError, ValueError):
            return 0
