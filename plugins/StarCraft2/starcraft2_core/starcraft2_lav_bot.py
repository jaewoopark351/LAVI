#20260707_kpopmodder: Added lazy burnysc2 BotAI builder for the internal LAV StarCraft2 MVP bot.
from __future__ import annotations

import inspect
import time
from typing import Any, Dict

from core.logger import log_print


def build_lav_starcraft2_bot(sc2_modules, event_callback=None, stop_event=None, state=None, config=None):
    bot_ai_class = sc2_modules["BotAI"]
    unit_type_id = sc2_modules["UnitTypeId"]
    config = dict(config or {})

    class LAVStarCraft2Bot(bot_ai_class):
        #20260707_kpopmodder: Minimal Terran rule bot; LAV coaches while engine owns frame-level control.
        def __init__(self):
            super().__init__()
            self._lav_event_callback = event_callback
            self._lav_stop_event = stop_event
            self._lav_state = state
            self._lav_config = dict(config)
            self._lav_event_flags = set()
            self._lav_started_at = 0.0
            self._lav_last_worker_event = 0.0
            self._lav_last_marine_event = 0.0
            self._lav_last_attack_order = 0.0
            self._lav_last_enemy_seen_event = 0.0

        async def on_start(self):
            self._lav_started_at = time.monotonic()
            self._emit("game_started")

        async def on_step(self, iteration):
            if self._lav_stop_event is not None and self._lav_stop_event.is_set():
                await self._leave_game()
                return

            self._update_state()
            await self._train_workers()
            await self._build_supply_depot()
            await self._build_barracks()
            await self._train_marines()
            await self._attack_when_ready()

        async def on_enemy_unit_entered_vision(self, unit):
            now = time.monotonic()
            interval = self._lav_float("enemy_seen_event_interval_sec", 20.0, 0.0, 300.0)
            if now - self._lav_last_enemy_seen_event < interval:
                return
            self._lav_last_enemy_seen_event = now
            self._emit(
                "enemy_seen",
                {
                    "unit": str(getattr(unit, "type_id", "")),
                    "position": str(getattr(unit, "position", "")),
                },
            )

        async def on_end(self, result):
            details = self._build_end_details(result)
            log_print(
                "[StarCraft2:LAVBot] game_ended "
                f"result={details.get('result', '')} "
                f"elapsed_sec={details.get('elapsed_sec', 0.0)} "
                f"game_time={details.get('game_time', '')} "
                f"workers={details.get('workers', 0)} "
                f"army={details.get('army_count', 0)} "
                f"supply={details.get('supply_used', 0)}/{details.get('supply_cap', 0)} "
                f"map={self._lav_config.get('map_name', '')}"
            )
            self._emit("game_ended", details)

        async def _train_workers(self):
            scv = self._unit("SCV")
            if scv is None:
                return
            townhalls = getattr(self, "townhalls", None)
            if not townhalls:
                return
            workers = getattr(self, "workers", [])
            target_workers = max(
                12,
                min(
                    self._lav_int("target_workers", 18, 8, 80),
                    max(16, self._safe_len(townhalls) * 22),
                ),
            )
            if len(workers) >= target_workers:
                return
            for townhall in townhalls:
                if not await self._can_afford(scv):
                    continue
                if getattr(townhall, "is_idle", False):
                    townhall.train(scv)
                    self._emit_rate_limited("worker_training", 2.0)
                    return

        async def _build_supply_depot(self):
            depot = self._unit("SUPPLYDEPOT")
            scv = self._unit("SCV")
            if depot is None or scv is None:
                return
            supply_left = self._safe_int(getattr(self, "supply_left", 0))
            supply_used = self._safe_int(getattr(self, "supply_used", 0))
            supply_cap = self._safe_int(getattr(self, "supply_cap", 0))
            if supply_left > 6 and supply_cap >= 31 and supply_used < supply_cap - 6:
                return
            if await self._already_pending(depot) >= 2:
                return
            if not await self._can_afford(depot):
                return
            townhall = self._first(getattr(self, "townhalls", []))
            if townhall is None:
                return
            near = getattr(townhall, "position", None)
            if hasattr(near, "towards"):
                near = near.towards(getattr(self, "game_info", None).map_center, 8)
            await self._call(self.build(depot, near=near))
            self._emit_once("supply_depot_started")

        async def _build_barracks(self):
            barracks = self._unit("BARRACKS")
            depot = self._unit("SUPPLYDEPOT")
            if barracks is None or depot is None:
                return
            ready_barracks = self._count_ready(barracks)
            pending_barracks = await self._already_pending(barracks)
            target_barracks = self._target_barracks()
            if ready_barracks + pending_barracks >= target_barracks:
                return
            if self._count_ready(depot) < 1:
                return
            if not await self._can_afford(barracks):
                return
            townhall = self._first(getattr(self, "townhalls", []))
            if townhall is None:
                return
            near = getattr(townhall, "position", None)
            if hasattr(near, "towards"):
                distance = 10 + min(ready_barracks + pending_barracks, 6) * 2
                near = near.towards(getattr(self, "game_info", None).map_center, distance)
            await self._call(self.build(barracks, near=near))
            if ready_barracks + pending_barracks == 0:
                self._emit_once("barracks_started")

        async def _train_marines(self):
            barracks_type = self._unit("BARRACKS")
            marine = self._unit("MARINE")
            if barracks_type is None or marine is None:
                return
            barracks_units = getattr(self, "structures", None)
            if callable(barracks_units):
                barracks_units = barracks_units(barracks_type)
            else:
                barracks_units = []
            trained = False
            for barracks in barracks_units.ready.idle:
                if not await self._can_afford(marine):
                    continue
                barracks.train(marine)
                trained = True
            if trained:
                self._emit_rate_limited("marine_training", 2.0)

        async def _attack_when_ready(self):
            marine = self._unit("MARINE")
            if marine is None:
                return
            units = getattr(self, "units", None)
            if not callable(units):
                return
            marines = units(marine).ready
            attack_count = self._lav_int("attack_count", 8, 1, 80)
            army_count = self._safe_len(marines)
            if army_count < attack_count and "attack_started" not in self._lav_event_flags:
                return
            if army_count < 2:
                return
            now = time.monotonic()
            interval = self._lav_float("reinforce_interval_sec", 5.0, 1.0, 60.0)
            if "attack_started" in self._lav_event_flags and now - self._lav_last_attack_order < interval:
                return
            target = self._attack_target()
            if target is None:
                return
            ordered = 0
            for unit in marines:
                unit.attack(target)
                ordered += 1
            if ordered:
                self._lav_last_attack_order = now
                self._emit_once("attack_started", {"army_count": army_count})

        def _update_state(self):
            if self._lav_state is None:
                return
            try:
                self._lav_state.update_stats(
                    minerals=getattr(self, "minerals", 0),
                    vespene=getattr(self, "vespene", 0),
                    supply_used=getattr(self, "supply_used", 0),
                    supply_cap=getattr(self, "supply_cap", 0),
                    workers=len(getattr(self, "workers", [])),
                    army_count=getattr(self, "army_count", 0),
                )
            except Exception:
                pass

        def _build_end_details(self, result):
            elapsed_sec = 0.0
            if self._lav_started_at:
                elapsed_sec = round(max(0.0, time.monotonic() - self._lav_started_at), 3)
            return {
                "result": str(result),
                "elapsed_sec": elapsed_sec,
                "game_time": str(getattr(self, "time_formatted", "") or ""),
                "minerals": self._safe_int(getattr(self, "minerals", 0)),
                "vespene": self._safe_int(getattr(self, "vespene", 0)),
                "supply_used": self._safe_int(getattr(self, "supply_used", 0)),
                "supply_cap": self._safe_int(getattr(self, "supply_cap", 0)),
                "workers": self._safe_len(getattr(self, "workers", [])),
                "army_count": self._safe_int(getattr(self, "army_count", 0)),
                "townhalls": self._safe_len(getattr(self, "townhalls", [])),
            }

        async def _leave_game(self):
            client = getattr(self, "client", None)
            leave = getattr(client, "leave", None)
            if callable(leave):
                await self._call(leave())

        def _unit(self, name):
            return getattr(unit_type_id, name, None)

        async def _can_afford(self, unit):
            return bool(await self._call(self.can_afford(unit)))

        async def _already_pending(self, unit):
            value = await self._call(self.already_pending(unit))
            try:
                return int(value)
            except (TypeError, ValueError):
                return 0

        async def _call(self, value):
            if inspect.isawaitable(value):
                return await value
            return value

        def _count_ready(self, unit):
            structures = getattr(self, "structures", None)
            if not callable(structures):
                return 0
            try:
                return len(structures(unit).ready)
            except Exception:
                return 0

        def _first(self, units):
            try:
                if not units:
                    return None
                return units[0]
            except Exception:
                return None

        def _safe_int(self, value, default=0):
            try:
                return int(value)
            except (TypeError, ValueError):
                return int(default)

        def _safe_len(self, value):
            try:
                return len(value)
            except Exception:
                return 0

        def _lav_int(self, key, default, minimum=None, maximum=None):
            value = self._safe_int(self._lav_config.get(key, default), default)
            if minimum is not None:
                value = max(int(minimum), value)
            if maximum is not None:
                value = min(int(maximum), value)
            return value

        def _lav_float(self, key, default, minimum=None, maximum=None):
            try:
                value = float(self._lav_config.get(key, default))
            except (TypeError, ValueError):
                value = float(default)
            if minimum is not None:
                value = max(float(minimum), value)
            if maximum is not None:
                value = min(float(maximum), value)
            return value

        def _target_barracks(self):
            configured = self._lav_int("target_barracks", 4, 1, 12)
            minerals = self._safe_int(getattr(self, "minerals", 0))
            if minerals >= 1000:
                configured = max(configured, 6)
            return configured

        def _attack_target(self):
            for collection_name in ("known_enemy_structures", "enemy_structures", "known_enemy_units"):
                collection = getattr(self, collection_name, None)
                first = self._first(collection)
                if first is not None:
                    return getattr(first, "position", first)
            enemy_start_locations = getattr(self, "enemy_start_locations", [])
            target = self._first(enemy_start_locations)
            if target is not None:
                return target
            return getattr(getattr(self, "game_info", None), "map_center", None)

        def _emit_once(self, event_type, details=None):
            if event_type in self._lav_event_flags:
                return
            self._lav_event_flags.add(event_type)
            self._emit(event_type, details)

        def _emit_rate_limited(self, event_type, interval_sec):
            now = time.time()
            key = "_lav_last_" + event_type
            last = float(getattr(self, key, 0.0) or 0.0)
            if now - last < float(interval_sec):
                return
            setattr(self, key, now)
            self._emit(event_type)

        def _emit(self, event_type, details=None):
            if not callable(self._lav_event_callback):
                return
            event = {
                "source": "starcraft2",
                "engine": "internal_lav_bot",
                "event_type": event_type,
                "details": details or {},
                "map_name": self._lav_config.get("map_name", ""),
                "race": self._lav_config.get("race", "Terran"),
                "enemy_race": self._lav_config.get("enemy_race", "Zerg"),
                "enemy_difficulty": self._lav_config.get("enemy_difficulty", "Easy"),
                "time": time.time(),
            }
            try:
                self._lav_event_callback(event)
            except Exception as e:
                log_print(f"[StarCraft2:LAVBot] event callback failed: {e}")

    return LAVStarCraft2Bot
