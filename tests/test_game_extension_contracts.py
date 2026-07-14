#20260715_kpopmodder: Verify shared GameExtension DTO/runtime/event contracts.
import unittest

from app_core.extensions import (
    GameCommandDTO,
    GameEventBus,
    GameEventDTO,
    GameEventMonitor,
    GameResultDTO,
    GameRuntimeContextRegistry,
    GameStatusDTO,
    GameStartResultDTO,
    GameStopResultDTO,
)
from app_core.extensions.chess_game_extension import ChessGameExtension
from app_core.extensions.game_extension_context import GameExtensionContext
from app_core.extensions.starcraft116_game_extension import StarCraft116GameExtension


class GameExtensionContractsTests(unittest.TestCase):
    def test_command_dto_preserves_legacy_dict_payload(self):
        command = GameCommandDTO.from_mapping(
            {
                "action": "Start",
                "profile_name": "default",
                "metadata": {"source": "test"},
            }
        )

        self.assertEqual("start", command.action)
        self.assertEqual("default", command.to_legacy_dict()["profile_name"])
        self.assertEqual({"source": "test"}, command.metadata)

    def test_result_and_status_dtos_accept_dict_payloads(self):
        result = GameResultDTO.from_mapping(
            {"ok": True, "status": {"running": True}},
            action="start",
        )
        status = GameStatusDTO.from_mapping(
            {
                "extension": {
                    "name": "starcraft2",
                    "initialized": True,
                    "started": True,
                },
                "worker": {"running": True},
            }
        )

        self.assertTrue(result.ok)
        self.assertEqual("start", result.action)
        self.assertEqual("starcraft2", status.name)
        self.assertTrue(status.initialized)
        self.assertTrue(status.worker["running"])

    def test_start_stop_and_status_dtos_preserve_legacy_fields(self):
        start = GameStartResultDTO.from_mapping(
            {"ok": True, "running": True, "pid": 1234},
            action="start",
        )
        stop = GameStopResultDTO.from_mapping(
            {"ok": True, "running": False, "stopped": True, "returncode": 0},
            action="stop",
        )
        status = GameStatusDTO.from_mapping(
            {
                "name": "starcraft2",
                "started": True,
                "engine": "internal_lav_bot",
            }
        )

        self.assertTrue(start.started)
        self.assertEqual(1234, start.details["pid"])
        self.assertTrue(stop.stopped)
        self.assertEqual(0, stop.details["returncode"])
        self.assertEqual("internal_lav_bot", status.details["engine"])
        self.assertEqual("starcraft2", status.to_legacy_dict()["name"])

    def test_runtime_context_registry_records_command_result_and_status(self):
        registry = GameRuntimeContextRegistry()
        context = registry.get("StarCraft2")

        context.mark_initialized(True)
        context.mark_started(True)
        context.set_command({"action": "status"})
        context.set_result({"ok": True, "status": {"running": True}}, action="status")
        context.set_status({"running": True})

        snapshot = registry.snapshot()["starcraft2"]
        self.assertTrue(snapshot["initialized"])
        self.assertTrue(snapshot["started"])
        self.assertEqual("status", snapshot["last_command"]["action"])
        self.assertTrue(snapshot["last_result"]["ok"])
        self.assertTrue(snapshot["status"]["running"])

    def test_game_event_bus_delivers_typed_event(self):
        events = []
        bus = GameEventBus()
        bus.subscribe(events.append)

        delivered = bus.emit(
            GameEventDTO(
                event_type="extension_started",
                game="starcraft2",
                details={"source": "test"},
            )
        )

        self.assertTrue(delivered)
        self.assertEqual("extension_started", events[0]["event_type"])
        self.assertEqual("starcraft2", events[0]["game"])

    def test_game_event_monitor_logs_successful_bus_delivery(self):
        messages = []
        bus = GameEventBus()
        monitor = GameEventMonitor(logger=messages.append)
        monitor.attach(bus)

        delivered = bus.emit(
            GameEventDTO(
                event_type="game_started",
                game="starcraft2",
                source="starcraft2",
            )
        )

        self.assertTrue(delivered)
        snapshot = monitor.snapshot()
        self.assertEqual(1, snapshot["total_events"])
        self.assertEqual("game_started", snapshot["recent_events"][0]["event_type"])
        self.assertEqual("starcraft2", snapshot["recent_events"][0]["game"])
        self.assertTrue(
            any(
                "[GameEventMonitor] received game=starcraft2" in item
                for item in messages
            )
        )

    def test_chess_and_starcraft116_status_include_common_status_shape(self):
        chess = ChessGameExtension(plugin=_FakeChessPlugin())
        chess._is_initialized = True
        chess_status = chess.get_status()

        starcraft116 = StarCraft116GameExtension(plugin=_FakeStarCraft116Plugin())
        starcraft116._is_initialized = True
        sc116_status = starcraft116.get_status()

        self.assertEqual("chess", chess_status["name"])
        self.assertTrue(chess_status["initialized"])
        self.assertEqual("chess", chess_status["game_status"]["name"])
        self.assertEqual("starcraft116", sc116_status["name"])
        self.assertTrue(sc116_status["initialized"])
        self.assertEqual("starcraft116", sc116_status["game_status"]["name"])

    def test_chess_handle_command_records_result_and_runtime_resources(self):
        registry = GameRuntimeContextRegistry()
        context = GameExtensionContext(runtime_contexts=registry, event_bus=GameEventBus())
        chess = ChessGameExtension(plugin=_FakeChessPlugin())
        chess.initialize(context)

        result = chess.handle_command({"action": "new_game"})

        snapshot = registry.snapshot()["chess"]
        self.assertTrue(result["ok"])
        self.assertEqual("new_game", result["action"])
        self.assertEqual({"fen": "startpos"}, result["state"])
        self.assertEqual({"fen": "startpos"}, result["details"]["state"])
        self.assertEqual("new_game", snapshot["last_command"]["action"])
        self.assertTrue(snapshot["last_result"]["ok"])
        self.assertEqual("new_game", snapshot["last_result"]["action"])
        self.assertEqual("_FakeChessPlugin", snapshot["resources"]["plugin"]["type"])
        self.assertEqual(
            "_FakeChessController",
            snapshot["resources"]["controller"]["type"],
        )
        self.assertEqual(
            "_FakeChessWebServer",
            snapshot["resources"]["web_server"]["type"],
        )

    def test_starcraft116_handle_command_records_result_contract(self):
        registry = GameRuntimeContextRegistry()
        context = GameExtensionContext(runtime_contexts=registry, event_bus=GameEventBus())
        starcraft116 = StarCraft116GameExtension(plugin=_FakeStarCraft116Plugin())
        starcraft116.initialize(context)
        self.addCleanup(starcraft116.shutdown)

        result = starcraft116.handle_command({"action": "status"})

        snapshot = registry.snapshot()["starcraft116"]
        self.assertTrue(result["ok"])
        self.assertEqual("status", result["action"])
        self.assertEqual("status", snapshot["last_command"]["action"])
        self.assertTrue(snapshot["last_result"]["ok"])
        self.assertEqual("status", snapshot["last_result"]["action"])
        self.assertEqual(
            "_FakeStarCraft116Plugin",
            snapshot["resources"]["plugin"]["type"],
        )
        self.assertEqual(
            "StarCraft116Bridge",
            snapshot["resources"]["bridge"]["type"],
        )
        self.assertEqual(
            "StarCraft116Worker",
            snapshot["resources"]["worker"]["type"],
        )
        self.assertEqual(
            "_FakeStarCraft116Config",
            snapshot["resources"]["config_manager"]["type"],
        )
        self.assertEqual(
            "_FakeStarCraft116Launcher",
            snapshot["resources"]["launcher"]["type"],
        )


class _FakeChessPlugin:
    server_url = "http://127.0.0.1:8765"
    server_message = "ready"
    web_server = None
    controller = None

    def __init__(self):
        self.controller = _FakeChessController()
        self.web_server = _FakeChessWebServer()


class _FakeChessController:
    def new_game(self):
        return {"fen": "startpos"}


class _FakeChessWebServer:
    url = "http://127.0.0.1:8765"


class _FakeStarCraft116Plugin:
    status_event_callback = None
    game_event_thread = None
    game_event_stop_event = None

    def __init__(self):
        self.config_manager = _FakeStarCraft116Config()
        self.launcher = _FakeStarCraft116Launcher()
        self.status_reader = _FakeStarCraft116StatusReader()
        self.state = _FakeStarCraft116RuntimeState()

    def get_status(self):
        return {"plugin": {"ready": True}}


class _FakeStarCraft116Config:
    pass


class _FakeStarCraft116Launcher:
    pass


class _FakeStarCraft116StatusReader:
    pass


class _FakeStarCraft116RuntimeState:
    pass


if __name__ == "__main__":
    unittest.main()
