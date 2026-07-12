#20260707_kpopmodder: Added internal StarCraft2 engine lazy-import tests without requiring burnysc2.
import asyncio
import unittest
from pathlib import Path
import signal
import threading
from types import SimpleNamespace
from unittest import mock

from plugins.StarCraft2.starcraft2_core.internal_lav_bot_engine import (
    InternalLAVBotEngine,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_lav_bot import (
    build_lav_starcraft2_bot,
)


class FakeEnum:
    Terran = "Terran"
    Zerg = "Zerg"
    Easy = "Easy"


class FakeMaps:
    @staticmethod
    def get(name):
        return f"map:{name}"


class FakeBotAI:
    pass


class FakeMap:
    def __init__(self, path):
        self.path = Path(path)


FakeMaps.Map = FakeMap


class FakeUnitTypeId:
    SCV = "SCV"
    SUPPLYDEPOT = "SUPPLYDEPOT"
    BARRACKS = "BARRACKS"
    MARINE = "MARINE"


class FakeUnitCollection:
    def __init__(self, units):
        self._units = list(units)

    @property
    def ready(self):
        return self

    @property
    def idle(self):
        return self

    def __iter__(self):
        return iter(self._units)

    def __len__(self):
        return len(self._units)

    def __bool__(self):
        return bool(self._units)

    def __getitem__(self, index):
        return self._units[index]


class FakeBarracks:
    def __init__(self):
        self.trained = []

    def train(self, unit):
        self.trained.append(unit)


class FakeCombatUnit:
    def __init__(self):
        self.attack_targets = []

    def attack(self, target):
        self.attack_targets.append(target)


class FakeRunner:
    def __init__(self):
        self.start_called = False
        self.start_sync_called = False
        self.function_factory = None

    def is_running(self):
        return False

    def start(self, coroutine_factory):
        self.start_called = True
        raise AssertionError("internal bot must not use async runner")

    def start_sync(self, function_factory):
        self.start_sync_called = True
        self.function_factory = function_factory
        return True

    def get_status(self):
        return {"running": False, "last_error": "", "last_result": None}


class StarCraft2InternalEngineTests(unittest.TestCase):
    def test_importing_engine_does_not_require_burnysc2(self):
        self.assertIsNotNone(InternalLAVBotEngine)

    def test_start_reports_lazy_import_failure_as_status_error(self):
        engine = InternalLAVBotEngine()

        with mock.patch(
            "plugins.StarCraft2.starcraft2_core.internal_lav_bot_engine.importlib.import_module",
            side_effect=ModuleNotFoundError("No module named 'sc2'"),
        ):
            result = engine.start({"map_name": "AbyssalReefLE"})

        self.assertFalse(result["ok"])
        self.assertIn("burnysc2_import_failed", result["error"])
        self.assertIn("No module named 'sc2'", result["status"]["last_error"])

    def test_start_uses_sync_runner_for_burnysc2_run_game(self):
        engine = InternalLAVBotEngine()
        fake_runner = FakeRunner()
        engine.runner = fake_runner
        fake_modules = {"run_game": lambda *args, **kwargs: "Victory"}

        with mock.patch.object(engine, "_load_sc2_modules", return_value=fake_modules):
            result = engine.start({"map_name": "001_wasteland", "enabled": True})

        self.assertTrue(result["ok"])
        self.assertFalse(fake_runner.start_called)
        self.assertTrue(fake_runner.start_sync_called)
        self.assertIsNotNone(fake_runner.function_factory)

    def test_run_game_sync_shims_burnysc2_signal_registration_in_worker_thread(self):
        engine = InternalLAVBotEngine()
        original_signal = signal.signal
        result = {}

        def fake_run_game(_sc2_modules):
            signal.signal(signal.SIGINT, signal.default_int_handler)
            signal.signal(signal.SIGINT, signal.SIG_DFL)
            return "Victory"

        with mock.patch.object(engine, "_run_game", side_effect=fake_run_game):
            thread = threading.Thread(
                target=lambda: result.setdefault("value", engine._run_game_sync({})),
            )
            thread.start()
            thread.join(timeout=3.0)

        self.assertFalse(thread.is_alive())
        self.assertEqual("Victory", result.get("value"))
        self.assertIs(original_signal, signal.signal)

    def test_run_game_sync_does_not_emit_started_before_map_resolution(self):
        engine = InternalLAVBotEngine()
        events = []
        engine._event_callback = events.append
        engine._config = {"map_name": "missing_map"}

        with mock.patch.object(engine, "_run_game", side_effect=FileNotFoundError("missing map")):
            with self.assertRaises(FileNotFoundError):
                engine._run_game_sync({})

        self.assertEqual(["error"], [event["event_type"] for event in events])
        self.assertEqual("missing_map", events[0]["details"]["map_name"])

    def test_run_game_sync_dedupes_bot_and_engine_lifecycle_events(self):
        engine = InternalLAVBotEngine()
        events = []
        engine._event_callback = events.append
        engine._config = {"map_name": "hunter"}
        engine.state.mark_started(engine.engine_name, engine._config)

        def fake_run_game(_sc2_modules):
            engine._emit("game_started")
            engine._emit("game_started")
            engine._emit("game_ended", {"result": "Defeat", "elapsed_sec": 1.0})
            return "Defeat"

        with mock.patch.object(engine, "_run_game", side_effect=fake_run_game):
            result = engine._run_game_sync({})

        self.assertEqual("Defeat", result)
        self.assertEqual(
            ["game_started", "game_ended"],
            [event["event_type"] for event in events],
        )

    def test_run_game_uses_mocked_sc2_modules(self):
        engine = InternalLAVBotEngine()
        engine._config = {
            "map_name": "AbyssalReefLE",
            "race": "Terran",
            "enemy_race": "Zerg",
            "enemy_difficulty": "Easy",
            "realtime": False,
        }
        calls = []

        def run_game(map_obj, players, realtime=False):
            calls.append((map_obj, players, realtime))
            return "Victory"

        fake_modules = {
            "maps": FakeMaps,
            "BotAI": FakeBotAI,
            "Race": FakeEnum,
            "Difficulty": FakeEnum,
            "UnitTypeId": FakeUnitTypeId,
            "Bot": lambda race, bot: ("bot", race, bot),
            "Computer": lambda race, difficulty: ("computer", race, difficulty),
            "run_game": run_game,
        }

        result = engine._run_game(fake_modules)

        self.assertEqual("Victory", result)
        self.assertEqual("map:AbyssalReefLE", calls[0][0])
        self.assertFalse(calls[0][2])
        self.assertEqual("bot", calls[0][1][0][0])
        self.assertEqual("computer", calls[0][1][1][0])

    def test_run_game_uses_plugin_maps_folder_before_sc2path_maps(self):
        engine = InternalLAVBotEngine()
        engine._config = {
            "map_name": "001_wasteland",
            "race": "Terran",
            "enemy_race": "Zerg",
            "enemy_difficulty": "Easy",
            "realtime": False,
        }
        calls = []

        def run_game(map_obj, players, realtime=False):
            calls.append((map_obj, players, realtime))
            return "Victory"

        engine._plugin_map_files = lambda: [
            Path("plugins/StarCraft2/maps/001_wasteland.SC2Map")
        ]
        fake_modules = {
            "maps": FakeMaps,
            "Map": FakeMap,
            "BotAI": FakeBotAI,
            "Race": FakeEnum,
            "Difficulty": FakeEnum,
            "UnitTypeId": FakeUnitTypeId,
            "Bot": lambda race, bot: ("bot", race, bot),
            "Computer": lambda race, difficulty: ("computer", race, difficulty),
            "run_game": run_game,
        }

        result = engine._run_game(fake_modules)

        self.assertEqual("Victory", result)
        self.assertEqual("001_wasteland.SC2Map", calls[0][0].path.name)

    def test_load_sc2_modules_uses_sc2_main_run_game_fallback(self):
        engine = InternalLAVBotEngine()
        expected_run_game = object()

        fake_modules = {
            "sc2": SimpleNamespace(),
            "sc2.maps": FakeMaps,
            "sc2.bot_ai": SimpleNamespace(BotAI=FakeBotAI),
            "sc2.data": SimpleNamespace(Race=FakeEnum, Difficulty=FakeEnum),
            "sc2.ids.unit_typeid": SimpleNamespace(UnitTypeId=FakeUnitTypeId),
            "sc2.player": SimpleNamespace(Bot=object, Computer=object),
            "sc2.main": SimpleNamespace(run_game=expected_run_game),
        }

        with mock.patch(
            "plugins.StarCraft2.starcraft2_core.internal_lav_bot_engine.importlib.import_module",
            side_effect=lambda name: fake_modules[name],
        ):
            modules = engine._load_sc2_modules()

        self.assertIs(expected_run_game, modules["run_game"])

    def test_lav_bot_trains_from_all_idle_barracks(self):
        bot_class = build_lav_starcraft2_bot(
            {
                "BotAI": FakeBotAI,
                "UnitTypeId": FakeUnitTypeId,
            }
        )
        bot = bot_class()
        barracks = [FakeBarracks(), FakeBarracks(), FakeBarracks()]
        bot.structures = lambda unit: FakeUnitCollection(barracks)
        bot.can_afford = lambda unit: True

        asyncio.run(bot._train_marines())

        self.assertEqual([["MARINE"], ["MARINE"], ["MARINE"]], [b.trained for b in barracks])

    def test_lav_bot_scales_barracks_target_on_mineral_rich_maps(self):
        bot_class = build_lav_starcraft2_bot(
            {
                "BotAI": FakeBotAI,
                "UnitTypeId": FakeUnitTypeId,
            },
            config={"target_barracks": 4},
        )
        bot = bot_class()

        bot.minerals = 50
        self.assertEqual(4, bot._target_barracks())

        bot.minerals = 1200
        self.assertEqual(6, bot._target_barracks())

    def test_lav_bot_reorders_attack_reinforcements(self):
        bot_class = build_lav_starcraft2_bot(
            {
                "BotAI": FakeBotAI,
                "UnitTypeId": FakeUnitTypeId,
            },
            config={"attack_count": 3, "reinforce_interval_sec": 1},
        )
        bot = bot_class()
        marines = [FakeCombatUnit(), FakeCombatUnit(), FakeCombatUnit()]
        bot.units = lambda unit: FakeUnitCollection(marines)
        bot.enemy_start_locations = ["enemy_base"]

        asyncio.run(bot._attack_when_ready())
        self.assertEqual(["enemy_base"], marines[0].attack_targets)

        bot._lav_last_attack_order -= 2
        asyncio.run(bot._attack_when_ready())
        self.assertEqual(["enemy_base", "enemy_base"], marines[0].attack_targets)


if __name__ == "__main__":
    unittest.main()
