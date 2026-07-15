#20260707_kpopmodder: Added lazy burnysc2 internal engine for StarCraft2 MVP matches.
from __future__ import annotations

from contextlib import contextmanager
import importlib
import os
from pathlib import Path
import signal
import threading
import time
from typing import Any, Dict, List

from core.logger import log_print

from .starcraft2_contracts import (
    EngineResultDTO,
    EngineStartCommandDTO,
    EngineStatusDTO,
)
from .starcraft2_engine_interface import StarCraft2EngineInterface
from .starcraft2_lav_bot import build_lav_starcraft2_bot
from .starcraft2_runner import StarCraft2ThreadedAsyncRunner
from .starcraft2_state import StarCraft2RuntimeState


class InternalLAVBotEngine(StarCraft2EngineInterface):
    #20260707_kpopmodder: Import burnysc2/sc2 only when this engine is actually started.
    engine_name = "internal_lav_bot"
    #20260715_kpopmodder: This is the first live SC2 engine using the DTO contract directly.
    uses_engine_dto_contract = True

    def __init__(self):
        self.state = StarCraft2RuntimeState(engine=self.engine_name)
        self.runner = StarCraft2ThreadedAsyncRunner("StarCraft2InternalLAVBot")
        self._stop_event = threading.Event()
        self._event_callback = None
        self._config: Dict[str, Any] = {}
        self._emitted_lifecycle_events = set()

    def start(
        self,
        command: EngineStartCommandDTO,
        event_callback=None,
    ) -> EngineResultDTO:
        if self.is_running():
            return self._result(True, status=self.get_status())

        command = EngineStartCommandDTO.from_mapping(command)
        self._config = command.to_dict()
        self._event_callback = event_callback
        self._stop_event.clear()
        self._emitted_lifecycle_events.clear()
        self.state.mark_started(self.engine_name, self._config)

        starcraft2_path = str(self._config.get("starcraft2_path") or "").strip()
        if starcraft2_path:
            os.environ["SC2PATH"] = starcraft2_path

        try:
            sc2_modules = self._load_sc2_modules()
        except Exception as e:
            self.state.mark_error(e)
            self._emit("error", {"error": str(e)})
            return self._result(
                False,
                running=False,
                status=self.get_status(),
                error=f"burnysc2_import_failed: {e}",
            )

        try:
            self.runner.start_sync(lambda: self._run_game_sync(sc2_modules))
        except Exception as e:
            self.state.mark_error(e)
            self._emit("error", {"error": str(e)})
            return self._result(
                False,
                running=False,
                status=self.get_status(),
                error=f"engine_start_failed: {e}",
            )
        return self._result(True, status=self.get_status())

    def stop(self) -> EngineResultDTO:
        self._stop_event.set()
        stopped = self.runner.join(timeout=3.0)
        if stopped:
            self.state.mark_stopped("stopped")
            return self._result(True, running=False, status=self.get_status())
        self.state.last_error = "stop_timeout"
        return self._result(False, running=True, status=self.get_status(), error="stop_timeout")

    def shutdown(self) -> EngineResultDTO:
        return self.stop()

    def get_status(self) -> EngineStatusDTO:
        status = self.state.to_dict()
        status["runner"] = self.runner.get_status()
        status["sc2path"] = os.environ.get("SC2PATH", "")
        return EngineStatusDTO.from_mapping(status, engine=self.engine_name)

    def is_running(self) -> bool:
        return self.runner.is_running()

    def _run_game_sync(self, sc2_modules) -> Any:
        started_at = time.monotonic()
        log_print(
            "[StarCraft2InternalLAVBot] run_game starting "
            f"map={self._config.get('map_name', '')} "
            f"race={self._config.get('race', 'Terran')} "
            f"enemy={self._config.get('enemy_race', 'Zerg')} "
            f"difficulty={self._config.get('enemy_difficulty', 'Easy')} "
            f"realtime={bool(self._config.get('realtime', False))}"
        )
        try:
            with self._burnysc2_worker_signal_shim():
                result = self._run_game(sc2_modules)
            self.state.mark_stopped(str(result or "completed"))
            details = self._build_game_end_details(result, started_at)
            if "game_ended" not in self._emitted_lifecycle_events:
                self._emit("game_ended", details)
            log_print(
                "[StarCraft2InternalLAVBot] run_game completed "
                f"result={details.get('result', '')} "
                f"elapsed_sec={details.get('elapsed_sec', 0.0)} "
                f"workers={details.get('workers', 0)} "
                f"army={details.get('army_count', 0)} "
                f"supply={details.get('supply_used', 0)}/{details.get('supply_cap', 0)}"
            )
            return result
        except Exception as e:
            self.state.mark_error(e)
            elapsed_sec = round(max(0.0, time.monotonic() - started_at), 3)
            log_print(
                "[StarCraft2InternalLAVBot] run_game failed "
                f"elapsed_sec={elapsed_sec} error={e}"
            )
            self._emit(
                "error",
                {
                    "error": str(e),
                    "elapsed_sec": elapsed_sec,
                    "map_name": self._config.get("map_name", ""),
                },
            )
            raise

    def _run_game(self, sc2_modules):
        bot_class = build_lav_starcraft2_bot(
            sc2_modules,
            event_callback=self._emit,
            stop_event=self._stop_event,
            state=self.state,
            config=self._config,
        )
        bot_instance = bot_class()
        race = self._enum_value(
            sc2_modules["Race"],
            self._config.get("race", "Terran"),
            "Terran",
        )
        enemy_race = self._enum_value(
            sc2_modules["Race"],
            self._config.get("enemy_race", "Zerg"),
            "Zerg",
        )
        difficulty = self._enum_value(
            sc2_modules["Difficulty"],
            self._config.get("enemy_difficulty", "Easy"),
            "Easy",
        )
        map_name = str(self._config.get("map_name") or "AbyssalReefLE")
        map_obj = self._resolve_map(sc2_modules, map_name)
        players = [
            sc2_modules["Bot"](race, bot_instance),
            sc2_modules["Computer"](enemy_race, difficulty),
        ]
        return sc2_modules["run_game"](
            map_obj,
            players,
            realtime=bool(self._config.get("realtime", False)),
        )

    def _load_sc2_modules(self) -> Dict[str, Any]:
        sc2 = importlib.import_module("sc2")
        maps = importlib.import_module("sc2.maps")
        bot_ai = importlib.import_module("sc2.bot_ai")
        data = importlib.import_module("sc2.data")
        ids = importlib.import_module("sc2.ids.unit_typeid")
        player = importlib.import_module("sc2.player")
        run_game = getattr(sc2, "run_game", None)
        if run_game is None:
            sc2_main = importlib.import_module("sc2.main")
            run_game = getattr(sc2_main, "run_game")
        return {
            "sc2": sc2,
            "maps": maps,
            "Map": maps.Map,
            "BotAI": bot_ai.BotAI,
            "Race": data.Race,
            "Difficulty": data.Difficulty,
            "UnitTypeId": ids.UnitTypeId,
            "Bot": player.Bot,
            "Computer": player.Computer,
            "run_game": run_game,
        }

    def _resolve_map(self, sc2_modules, map_name: str):
        local_map = self._find_plugin_map(map_name)
        if local_map is not None:
            return sc2_modules["Map"](local_map)
        try:
            return sc2_modules["maps"].get(map_name)
        except (FileNotFoundError, KeyError) as e:
            available = self._available_plugin_map_names()
            if available:
                raise type(e)(
                    f"{e}; plugin maps available: {', '.join(available)}. "
                    "Set StarCraft2 Map Name to one of these names."
                ) from e
            raise

    def _find_plugin_map(self, map_name: str) -> Path | None:
        target = Path(str(map_name or "").strip()).stem
        if not target:
            return None
        candidates = self._plugin_map_files()
        for path in candidates:
            if path.stem == target:
                return path
        target_lower = target.lower()
        for path in candidates:
            if path.stem.lower() == target_lower:
                return path
        return None

    def _available_plugin_map_names(self) -> List[str]:
        return [path.stem for path in self._plugin_map_files()]

    def _plugin_map_files(self) -> List[Path]:
        maps_dir = self._plugin_maps_dir()
        if not maps_dir.is_dir():
            return []
        return sorted(
            (
                path
                for path in maps_dir.rglob("*")
                if path.is_file() and path.suffix.lower() == ".sc2map"
            ),
            key=lambda path: path.stem.lower(),
        )

    def _plugin_maps_dir(self) -> Path:
        return Path(__file__).resolve().parents[1] / "maps"

    @contextmanager
    def _burnysc2_worker_signal_shim(self):
        if threading.current_thread() is threading.main_thread():
            yield
            return

        original_signal = signal.signal

        def safe_signal(sig, handler):
            try:
                return original_signal(sig, handler)
            except ValueError as e:
                if "main thread" in str(e):
                    return signal.getsignal(sig)
                raise

        signal.signal = safe_signal
        try:
            yield
        finally:
            signal.signal = original_signal

    def _enum_value(self, enum_class, configured_name: Any, default_name: str):
        configured = str(configured_name or default_name).strip()
        for candidate in (configured, configured.lower(), configured.upper(), configured.title()):
            if hasattr(enum_class, candidate):
                return getattr(enum_class, candidate)
        normalized = configured.lower()
        for name in dir(enum_class):
            if name.lower() == normalized:
                return getattr(enum_class, name)
        return getattr(enum_class, default_name)

    def _emit(self, event_type_or_event, details: Dict[str, Any] | None = None):
        if isinstance(event_type_or_event, dict):
            event = dict(event_type_or_event)
        else:
            event = {
                "source": "starcraft2",
                "engine": self.engine_name,
                "event_type": str(event_type_or_event),
                "details": details or {},
                "map_name": self._config.get("map_name", ""),
                "race": self._config.get("race", "Terran"),
                "enemy_race": self._config.get("enemy_race", "Zerg"),
                "enemy_difficulty": self._config.get("enemy_difficulty", "Easy"),
                "time": time.time(),
            }
        event.setdefault("source", "starcraft2")
        event.setdefault("engine", self.engine_name)
        event.setdefault("time", time.time())
        if self._is_duplicate_lifecycle_event(event):
            return False
        self.state.update_event(event)
        if not callable(self._event_callback):
            return True
        try:
            self._event_callback(event)
        except Exception as e:
            log_print(f"[StarCraft2:internal_lav_bot] event callback failed: {e}")
        return True

    def _is_duplicate_lifecycle_event(self, event: Dict[str, Any]) -> bool:
        event_type = str((event or {}).get("event_type") or "").strip().lower()
        if event_type not in {"game_started", "game_ended"}:
            return False
        if event_type in self._emitted_lifecycle_events:
            log_print(f"[StarCraft2InternalLAVBot] duplicate {event_type} event skipped")
            return True
        self._emitted_lifecycle_events.add(event_type)
        return False

    def _build_game_end_details(self, result: Any, started_at: float) -> Dict[str, Any]:
        elapsed_sec = round(max(0.0, time.monotonic() - started_at), 3)
        return {
            "result": str(result or "completed"),
            "elapsed_sec": elapsed_sec,
            "minerals": self.state.minerals,
            "vespene": self.state.vespene,
            "supply_used": self.state.supply_used,
            "supply_cap": self.state.supply_cap,
            "workers": self.state.workers,
            "army_count": self.state.army_count,
        }
