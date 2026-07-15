#20260713_kpopmodder: Verify StarCraft2 UI callbacks stay behind the facade boundary.
import unittest
from unittest import mock

from app_core.extensions import (
    GameEventBus,
    GameStartResultDTO,
    GameStatusDTO,
    GameStopResultDTO,
)
from plugins.StarCraft2.starcraft2 import StarCraft2
from plugins.StarCraft2.starcraft2_core.starcraft2_dto import (
    StarCraft2CommandResult,
    StarCraft2LocalMatchCommand,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_contracts import (
    LadderProxyExitEventDTO,
    LadderProxyResultDTO,
    LadderProxyStatusDTO,
    LocalMatchLaunchConfigDTO,
    LocalMatchRuntimeStatusDTO,
    StarCraft2Event,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_event_bus import (
    StarCraft2EventBus,
    _StarCraft2EventBus,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_event_service import (
    StarCraft2EngineEventService,
    _StarCraft2EngineEventService,
    StarCraft2LadderProxyEventService,
    _StarCraft2LadderProxyEventService,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_facade_service import (
    StarCraft2FacadeService,
    _StarCraft2FacadeService,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_local_match_service import (
    StarCraft2LocalMatchService,
    _StarCraft2LocalMatchService,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_runtime_context import (
    SC2RuntimeContext,
)


class StarCraft2FacadeServiceTests(unittest.TestCase):
    def test_public_facade_name_keeps_legacy_alias(self):
        self.assertIs(StarCraft2FacadeService, _StarCraft2FacadeService)
        self.assertIs(StarCraft2EventBus, _StarCraft2EventBus)
        self.assertIs(StarCraft2LocalMatchService, _StarCraft2LocalMatchService)
        self.assertIs(StarCraft2EngineEventService, _StarCraft2EngineEventService)
        self.assertIs(
            StarCraft2LadderProxyEventService,
            _StarCraft2LadderProxyEventService,
        )

    def test_local_human_button_delegates_to_facade_service(self):
        plugin = StarCraft2()
        plugin._facade_service.on_local_human_vs_changeling_click = mock.Mock(
            return_value='{"ok": true}'
        )

        result = plugin.on_local_human_vs_changeling_click(
            "C:\\Tools\\LavHumanVsBot.exe",
            "C:\\Tools",
            "--bot changeling --race Protoss",
            "5677,5678",
            ai_race="Zerg",
        )

        self.assertEqual('{"ok": true}', result)
        plugin._facade_service.on_local_human_vs_changeling_click.assert_called_once_with(
            "C:\\Tools\\LavHumanVsBot.exe",
            "C:\\Tools",
            "--bot changeling --race Protoss",
            "5677,5678",
            ai_race="Zerg",
        )

    def test_event_bus_emits_stable_event_contract(self):
        events = []
        bus = StarCraft2EventBus()
        bus.subscribe(events.append)

        delivered = bus.emit({"event_type": "game_started", "details": {"source": "test"}})

        self.assertTrue(delivered)
        self.assertEqual("game_started", events[0]["event_type"])
        self.assertEqual({"source": "test"}, events[0]["details"])
        self.assertEqual("starcraft2", events[0]["source"])

    def test_event_bus_accepts_typed_event_contract(self):
        events = []
        bus = StarCraft2EventBus()
        bus.subscribe(events.append)

        delivered = bus.emit(
            StarCraft2Event(
                event_type="game_started",
                details={"source": "dto"},
            )
        )

        self.assertTrue(delivered)
        self.assertEqual("game_started", events[0]["event_type"])
        self.assertEqual({"source": "dto"}, events[0]["details"])

    def test_event_bus_mirrors_to_common_game_event_bus(self):
        events = []
        common_bus = GameEventBus()
        common_bus.subscribe(events.append)
        bus = StarCraft2EventBus(common_event_bus=common_bus)

        delivered = bus.emit(
            {"event_type": "game_started", "details": {"source": "sc2"}}
        )

        self.assertFalse(delivered)
        self.assertEqual("game_started", events[0]["event_type"])
        self.assertEqual("starcraft2", events[0]["game"])
        self.assertEqual("starcraft2", events[0]["source"])
        self.assertEqual("sc2", events[0]["details"]["source"])

    def test_common_event_subscription_uses_attached_game_bus(self):
        events = []
        common_bus = GameEventBus()
        bus = StarCraft2EventBus()
        bus.set_common_event_bus(common_bus)
        bus.subscribe_common_events(events.append)

        bus.emit({"event_type": "unit_produced", "details": {"unit_type_id": "104"}})

        self.assertEqual("unit_produced", events[0]["event_type"])
        self.assertEqual("starcraft2", events[0]["game"])

    def test_local_match_command_from_mapping_accepts_stable_ports_key(self):
        command = StarCraft2LocalMatchCommand.from_mapping(
            {
                "executable_path": "C:\\Tools\\LavHumanVsBot.exe",
                "working_directory": "C:\\Tools",
                "args": ["--race", "Protoss"],
                "ports": "5677,5678",
                "bot_name": "changeling",
            }
        )

        self.assertEqual([5677, 5678], command.proxy_ports)
        self.assertEqual("--race Protoss", command.args)
        self.assertEqual("changeling", command.bot_name)

    def test_facade_missing_local_match_returns_typed_status_contract(self):
        facade = StarCraft2FacadeService(
            config_manager=mock.Mock(),
            engine_registry=mock.Mock(),
            state=mock.Mock(),
            ladder_proxy=mock.Mock(),
            match_config_service=mock.Mock(),
            engine_event_service=None,
            local_match_service=None,
        )

        status = facade.start_local_match("", "", "", "")
        payload = status.to_dict()

        self.assertIsInstance(status, LocalMatchRuntimeStatusDTO)
        self.assertEqual("local_human_vs_changeling", payload["mode"])
        self.assertFalse(payload["result"]["ok"])
        self.assertEqual(
            "local_match_service_missing",
            payload["result"]["error"],
        )
        self.assertEqual("starcraft2", payload["game_status"]["name"])
        self.assertFalse(payload["game_result"]["ok"])

    def test_facade_start_stop_status_include_common_dtos(self):
        facade = self._build_facade()

        start_result = facade.start({})
        status = facade.get_status()
        stop_result = facade.stop()

        self.assertIsInstance(facade._last_game_start_result_dto, GameStartResultDTO)
        self.assertIsInstance(facade._last_game_status_dto, GameStatusDTO)
        self.assertIsInstance(facade._last_game_stop_result_dto, GameStopResultDTO)
        self.assertTrue(start_result["game_result"]["ok"])
        self.assertTrue(start_result["game_result"]["started"])
        self.assertEqual("starcraft2", status["game_status"]["name"])
        self.assertTrue(status["game_status"]["started"])
        self.assertTrue(stop_result["game_result"]["stopped"])

    #20260715_kpopmodder: Facade-owned runtime context must keep DTO errors.
    def test_facade_runtime_context_preserves_top_level_engine_error(self):
        context = SC2RuntimeContext()
        facade = self._build_facade(runtime_context=context)
        facade.current_engine = _FakeEngine()

        facade._sync_runtime_context(
            {
                "ok": False,
                "running": False,
                "status": {},
                "error": "engine_start_failed",
            }
        )

        snapshot = context.snapshot()
        self.assertEqual("engine_start_failed", snapshot["runtime_error"])
        self.assertEqual("engine_start_failed", snapshot["status"]["error"])

    def test_local_match_service_wraps_start_stop_status_in_common_dtos(self):
        service = self._build_local_match_service()
        command = StarCraft2LocalMatchCommand(
            executable_path="C:\\Tools\\LavHumanVsBot.exe",
            working_directory="C:\\Tools",
            args="--race Protoss",
            proxy_ports=[5677, 5678],
            bot_name="changeling",
            ai_race="Zerg",
            human_race="Protoss",
        )
        service._build_local_match_command = mock.Mock(return_value=command)
        service._start_local_human_vs_changeling = mock.Mock(
            return_value=StarCraft2CommandResult(
                ok=True,
                running=True,
                action="local_human_vs_changeling",
                status={"running": True},
            )
        )

        start_status = service.start_local_match("", "", "", "")
        current_status = service.get_local_match_status()
        stop_status = service.stop_local_match()

        self.assertTrue(start_status.to_dict()["game_result"]["started"])
        self.assertEqual("starcraft2", current_status.to_dict()["game_status"]["name"])
        self.assertEqual("stop", stop_status.to_dict()["game_result"]["action"])
        self.assertIsInstance(service._last_game_status_dto, GameStatusDTO)

    def test_facade_is_the_local_match_runtime_context_writer(self):
        facade = self._build_facade()
        facade.local_match_service = mock.Mock()
        facade.local_match_service.start_local_match.return_value = LocalMatchRuntimeStatusDTO()
        facade.match_config_service.local_match_config.return_value = {
            "ports": [5677, 5678],
            "check_hosts": ["127.0.0.1"],
        }
        facade.ladder_proxy.process = mock.Mock(pid=4321)
        facade.ladder_proxy.started_at = 123.0
        facade.ladder_proxy.get_status.return_value = LadderProxyStatusDTO(
            running=True,
            pid=4321,
            stdout_tail=["ready"],
            validation={"connect_timeout_sec": 2.5},
        )

        facade.start_local_match("proxy.exe", "runtime", "", "5677,5678")

        snapshot = facade.runtime_context.snapshot()
        self.assertEqual(4321, snapshot["process_pid"])
        self.assertEqual("local_match_proxy", snapshot["process_role"])
        self.assertEqual(["ready"], snapshot["stdout_tail"])
        self.assertEqual([5677, 5678], snapshot["ports"])
        self.assertEqual(2.5, snapshot["timeout_sec"])

    def test_local_match_service_emits_exit_without_writing_runtime_context(self):
        context = SC2RuntimeContext(status={"owner": "facade"}, process_pid=99)
        service = self._build_local_match_service()

        service._on_ladder_proxy_exit(
            LadderProxyExitEventDTO(pid=99, returncode=4)
        )

        snapshot = context.snapshot()
        self.assertEqual({"owner": "facade"}, snapshot["status"])
        self.assertEqual(99, snapshot["process_pid"])
        self.assertNotIn("runtime_context", service.__dict__)

    #20260715_kpopmodder: LocalMatch may read runtime snapshots but must not own context.
    def test_local_match_service_reads_runtime_snapshot_without_owning_context(self):
        context = SC2RuntimeContext(status={"owner": "facade"}, process_pid=77)
        service = self._build_local_match_service(
            runtime_snapshot_provider=context.snapshot
        )

        status = service.get_local_match_status()

        payload = status.to_dict()
        self.assertEqual(77, payload["game_status"]["runtime"]["process_pid"])
        self.assertNotIn("runtime_context", service.__dict__)

    #20260715_kpopmodder: Facade applies proxy_stopped through the typed EventBus path.
    def test_facade_handles_proxy_stopped_as_typed_runtime_event(self):
        event_bus = StarCraft2EventBus()
        context = SC2RuntimeContext()
        facade = self._build_facade(event_bus=event_bus, runtime_context=context)
        facade.ladder_proxy.get_status.return_value = LadderProxyStatusDTO(
            running=False,
            returncode=0,
        )

        delivered = event_bus.emit(
            StarCraft2Event("proxy_stopped", {"returncode": 0})
        )

        self.assertTrue(delivered)
        self.assertIsNone(context.runtime_error)
        self.assertIsNotNone(context.stopped_at)

    #20260715_kpopmodder: Shutdown must preserve proxy stop failures in Facade-owned state.
    def test_shutdown_syncs_ladder_proxy_stop_result_to_runtime_context(self):
        context = SC2RuntimeContext()
        facade = self._build_facade(runtime_context=context)
        facade.ladder_proxy.stop.return_value = LadderProxyResultDTO(
            ok=False,
            running=False,
            error="proxy_stop_failed",
            status=LadderProxyStatusDTO(running=False),
        )
        facade.ladder_proxy.get_status.return_value = LadderProxyStatusDTO(
            running=False,
        )

        facade.shutdown()

        self.assertEqual("proxy_stop_failed", context.runtime_error)
        facade.ladder_proxy.stop.assert_called_once()

    #20260715_kpopmodder: Status polling after proxy_stopped must not move the stop time.
    def test_local_match_status_does_not_overwrite_proxy_stopped_time(self):
        context = SC2RuntimeContext()
        facade = self._build_facade(runtime_context=context)
        facade.ladder_proxy.get_status.return_value = LadderProxyStatusDTO(
            running=False,
            returncode=0,
        )

        with mock.patch(
            "plugins.StarCraft2.starcraft2_core.starcraft2_facade_service.time.time",
            return_value=10.0,
        ):
            facade._sync_local_match_runtime_context(
                exit_details={"returncode": 0}
            )
        with mock.patch(
            "plugins.StarCraft2.starcraft2_core.starcraft2_facade_service.time.time",
            return_value=20.0,
        ):
            facade._sync_local_match_runtime_context()

        self.assertEqual(10.0, context.stopped_at)
        self.assertIsNone(context.runtime_error)

    def _build_facade(self, event_bus=None, runtime_context=None):
        config_manager = mock.Mock()
        config_manager.build_runtime_config.return_value = {
            "enabled": True,
            "engine": "fake",
        }
        config_manager.get_bool.return_value = True
        config_manager.get.return_value = "fake"
        config_manager.config_message.return_value = "ok"
        engine = _FakeEngine()
        engine_registry = mock.Mock()
        engine_registry.create.return_value = engine
        state = _FakeState()
        ladder_proxy = mock.Mock()
        ladder_proxy.get_status.return_value = LadderProxyStatusDTO(running=False)
        match_config_service = mock.Mock()
        match_config_service.ladder_proxy_config.return_value = {}
        return StarCraft2FacadeService(
            config_manager=config_manager,
            engine_registry=engine_registry,
            state=state,
            ladder_proxy=ladder_proxy,
            match_config_service=match_config_service,
            engine_event_service=None,
            event_bus=event_bus,
            runtime_context=runtime_context,
        )

    def _build_local_match_service(self, runtime_snapshot_provider=None):
        arg_utils = mock.Mock()
        config_service = mock.Mock()
        config_service.local_match_config.return_value = {}
        command_template = mock.Mock()
        ladder_proxy = mock.Mock()
        ladder_proxy.stop.return_value = LadderProxyResultDTO(
            ok=True,
            running=False,
            stopped=True,
            status=LadderProxyStatusDTO(running=False),
        )
        ladder_proxy.get_status.side_effect = lambda command=None: LadderProxyStatusDTO(
            running=False,
            validation=(
                {"executable_path": command.executable_path}
                if isinstance(command, LocalMatchLaunchConfigDTO)
                else {}
            ),
        )
        return StarCraft2LocalMatchService(
            arg_utils,
            config_service,
            command_template,
            ladder_proxy,
            runtime_snapshot_provider=runtime_snapshot_provider,
        )


class _FakeState:
    def __init__(self):
        self.running = False
        self.last_error = ""
        self.last_event = {}
        self.process_pid = None
        self.stdout_tail = []
        self.stderr_tail = []

    def to_dict(self):
        return {
            "running": self.running,
            "last_error": self.last_error,
            "last_event": dict(self.last_event),
        }

    def mark_stopped(self, reason):
        self.running = False
        self.last_error = str(reason or "")

    def mark_error(self, error):
        self.last_error = str(error)


class _FakeEngine:
    engine_name = "fake"

    def __init__(self):
        self.running = False

    def start(self, runtime_config, event_callback=None):
        self.running = True
        return {
            "ok": True,
            "running": True,
            "status": {"running": True},
            "details": {"engine": self.engine_name},
        }

    def stop(self):
        self.running = False
        return {
            "ok": True,
            "running": False,
            "stopped": True,
            "status": {"running": False},
        }

    def is_running(self):
        return self.running

    def get_status(self):
        return {"running": self.running}


if __name__ == "__main__":
    unittest.main()
