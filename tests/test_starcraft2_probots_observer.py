#20260708_kpopmodder: Added passive ProBots observer tests without launching or controlling SC2.
import io
import os
import socket
import unittest
from unittest import mock

from app_core.extensions.game_extension_context import GameExtensionContext
from app_core.extensions.starcraft2_game_extension import (
    STARCRAFT2_LOG_EVENT_ORIGIN,
    STARCRAFT2_STATUS_EVENT_CALLBACK_RESOURCE,
    StarCraft2GameExtension,
)
from plugins.StarCraft2.starcraft2_core.probots_launcher import ProBotsLauncher
from plugins.StarCraft2.starcraft2_core.probots_log_watcher import ProBotsLogWatcher, WatchedLogState
from plugins.StarCraft2.starcraft2_core.sc2_event_parser import SC2EventParser
from plugins.StarCraft2.starcraft2_core.sc2_extension import StarCraft2Extension
from plugins.StarCraft2.starcraft2_core.sc2_ladder_proxy_launcher import SC2LadderProxyLauncher
from plugins.StarCraft2.starcraft2_core.sc2_tts_bridge import SC2TTSBridge
from plugins.StarCraft2.starcraft2 import StarCraft2
from plugins.StarCraft2.bot_launch_profiles import get_bot_launch_profile


# LAN_LOBBY_ARCHIVED_SKIP = "LAN Lobby archived/disabled; LAN modules are commented out."


class _FakeBinaryFile(io.BytesIO):
    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, traceback):
        self.close()


class StarCraft2ProBotsObserverTests(unittest.TestCase):
    def test_bot_launch_profiles_use_separate_runtime_types(self):
        self.assertEqual("Java", get_bot_launch_profile("BenBotBC").bot_type)
        self.assertEqual("BinaryCpp", get_bot_launch_profile("changeling").bot_type)
        self.assertEqual("dotnetcore", get_bot_launch_profile("sharkbot").bot_type)
        self.assertEqual("sharkbot.dll", get_bot_launch_profile("sharkbot").file_name)

    def test_sharkbot_profile_rejects_mixed_runtime_dlls(self):
        profile = get_bot_launch_profile("sharkbot")
        expected_hashes = dict(profile.runtime_sha256)

        def runtime_hash(path):
            file_name = path.rsplit("\\", 1)[-1].rsplit("/", 1)[-1]
            if file_name == "Sharky.dll":
                return "0" * 64
            return expected_hashes[file_name]

        with mock.patch(
            "plugins.StarCraft2.bot_launch_profiles.os.path.isfile",
            return_value=True,
        ):
            with mock.patch(
                "plugins.StarCraft2.bot_launch_profiles._sha256_file",
                side_effect=runtime_hash,
            ):
                result = profile.validate("C:\\SC2Runtime\\Bots")

        self.assertFalse(result["ok"])
        self.assertTrue(result["strict_runtime"])
        self.assertEqual("bot_runtime_checksum_mismatch", result["error"])
        failed_files = [item["file"] for item in result["runtime_checks"] if not item["ok"]]
        self.assertEqual(["Sharky.dll"], failed_files)

    def test_ladder_result_is_reported_from_ai_perspective(self):
        plugin = StarCraft2()
        callback = mock.Mock()
        plugin.status_event_callback = callback

        plugin._on_ladder_proxy_line(
            "stdout",
            "[LavHumanVsBot] Finished with result: Player1Win",
        )
        self.assertEqual("game_lost", callback.call_args.args[0]["event_type"])

        callback.reset_mock()
        plugin._on_ladder_proxy_line(
            "stdout",
            "[LavHumanVsBot] Finished with result: Player2Win",
        )
        self.assertEqual("game_won", callback.call_args.args[0]["event_type"])

    def test_ladder_initialization_error_is_not_reported_as_match_loss(self):
        plugin = StarCraft2()
        callback = mock.Mock()
        plugin.status_event_callback = callback

        plugin._on_ladder_proxy_line(
            "stdout",
            "[LavHumanVsBot] Finished with result: InitializationError",
        )

        self.assertEqual("engine_error", callback.call_args.args[0]["event_type"])

    def test_extension_ladder_proxy_line_keeps_log_only(self):
        status_callback = mock.Mock()
        tts = mock.Mock()
        context = GameExtensionContext(tts=tts)
        context.set_shared(
            STARCRAFT2_STATUS_EVENT_CALLBACK_RESOURCE,
            status_callback,
        )

        extension = StarCraft2Extension(
            plugin_root="C:\\fake\\StarCraft2",
            config_path="missing.json",
        )
        extension.initialize(context)
        extension.tts_bridge.speak = mock.Mock(return_value={"ok": True})

        extension._on_ladder_proxy_line("stdout", "[LavHumanVsBot] Building unit: PROBE")
        extension._on_ladder_proxy_line(
            "stderr",
            "[LavHumanVsBot] Finished with result: Player1Win",
        )

        status_callback.assert_not_called()
        extension.tts_bridge.speak.assert_not_called()
        tts.cancel_pending.assert_not_called()

    def test_parser_converts_known_events_and_dedupes_repeated_lines(self):
        parser = SC2EventParser(bot_name="Changeling")

        first = parser.parse_event("Changeling started attack at the natural expansion")
        second = parser.parse_event("Changeling started attack at the natural expansion")
        unknown = parser.parse_event("heartbeat frame=123")

        self.assertIsNotNone(first)
        self.assertEqual("attack", first.category)
        self.assertTrue(bool(str(first.message or "").strip()))
        self.assertNotIn("Changeling", first.message)
        self.assertIsNone(second)
        self.assertIsNone(unknown)

    def test_parser_recognizes_sc2aiapp_changeling_lifecycle_logs(self):
        parser = SC2EventParser(bot_name="Changeling")

        started = parser.parse_event("08-08-2025 13-51-00: Starting game vs human")
        ended = parser.parse_event("2025-08-08 14:02:34.774 | INFO | END GAME REPORT")

        self.assertIsNotNone(started)
        self.assertEqual("game_started", started.category)
        self.assertTrue(bool(str(started.message or "").strip()))
        self.assertNotIn("Changeling", started.message)
        self.assertIsNotNone(ended)
        self.assertEqual("result", ended.category)

    def test_parser_keeps_raw_engine_errors_out_of_tts_events(self):
        parser = SC2EventParser(bot_name="Changeling")

        raw_error_lines = [
            "Traceback (most recent call last):",
            "2026-07-12 22:37:01.234 | ERROR | sc2.main:initialize_first_step:144 - Caught unknown exception in AI on_start",
            "AssertionError: self._find_expansion_locations() has not been run yet",
            "AttributeError: 'Eris' object has no attribute 'manager_hub'",
            "[PYI-30628:ERROR] Failed to execute script 'run' due to unhandled exception!",
            "Resigning due to previous error",
            "ERROR: ladder bot failed before the first game step",
        ]

        for line in raw_error_lines:
            with self.subTest(line=line):
                self.assertIsNone(parser.parse_event(line))

    def test_parser_allows_same_event_again_after_new_game(self):
        parser = SC2EventParser(bot_name="Changeling")

        self.assertIsNotNone(parser.parse_event("Chosen opening: LING_BANE"))
        self.assertIsNotNone(parser.parse_event("Starting ladder game vs human"))
        self.assertIsNotNone(parser.parse_event("Chosen opening: LING_BANE"))

    def test_parser_recognizes_real_changeling_build_and_strategy_logs(self):
        parser = SC2EventParser(bot_name="Changeling")

        opening = parser.parse_event(
            "2026-07-08 21:55:34 | INFO | Chosen opening: BotMode.HatchPoolHatchGas"
        )
        hatchery = parser.parse_event(
            "2026-07-08 21:56:24 | INFO | 01:03 HATCHERY started"
        )
        queen = parser.parse_event(
            "2026-07-08 21:57:51 | INFO | bot.managers.build_runner:do_step - 17 02:32 QUEEN"
        )
        overlord = parser.parse_event(
            "2026-07-08 21:57:52 | INFO | bot.managers.build_runner:do_step - 13 00:11 OVERLORD"
        )
        transition = parser.parse_event(
            "2026-07-08 21:58:22 | INFO | 03:03: Transitioning from HatchPoolHatchGas to Adaptive because of OPENINGCOMPLETE"
        )
        army = parser.parse_event(
            "2026-07-08 21:58:23 | INFO | 03:04: Changed army composition to: LING_BANE"
        )

        self.assertEqual("strategy", opening.category)
        self.assertTrue(bool(str(opening.message or "").strip()))
        self.assertNotIn("HatchPoolHatchGas", opening.message)
        self.assertEqual("build", hatchery.category)
        self.assertIn("HATCHERY", str(hatchery.raw_line or ""))
        self.assertNotIn("HATCHERY", hatchery.message)
        self.assertEqual("train", queen.category)
        self.assertTrue(bool(str(queen.message or "").strip()))
        self.assertNotIn("QUEEN", queen.message)
        self.assertEqual("train", overlord.category)
        self.assertTrue(bool(str(overlord.message or "").strip()))
        self.assertNotIn("OVERLORD", overlord.message)
        self.assertEqual("strategy", transition.category)
        self.assertTrue(bool(str(transition.message or "").strip()))
        self.assertNotIn("Adaptive", transition.message)
        self.assertEqual("strategy", army.category)
        self.assertTrue(bool(str(army.message or "").strip()))
        self.assertNotIn("LING_BANE", army.message)

    def test_parser_localizes_hatch_gas_pool_extractor_trick(self):
        parser = SC2EventParser(bot_name="Changeling")
        spoken_name = "HatchGasPoolExtractorTrick"

        opening = parser.parse_event(
            "Chosen opening: BotMode.HatchGasPoolExtractorTrick"
        )
        transition = parser.parse_event(
            "03:12: Transitioning from HatchGasPoolExtractorTrick "
            "to Adaptive because of OPENINGCOMPLETE"
        )

        self.assertTrue(bool(str(opening.message or "").strip()))
        self.assertTrue(bool(str(transition.message or "").strip()))
        self.assertNotIn("HatchGasPoolExtractorTrick", opening.message)
        self.assertNotIn("HatchGasPoolExtractorTrick", transition.message)

    def test_parser_distinguishes_build_preparation_from_construction_start(self):
        parser = SC2EventParser(bot_name="Changeling")

        preparation = parser.parse_event(
            "2026-07-10 13:00:28 | INFO | "
            "00:31 UnitTypeId.HATCHERY added to building tracker"
        )
        started = parser.parse_event(
            "2026-07-10 13:00:44 | INFO | 00:47 HATCHERY started"
        )

        self.assertIsNotNone(preparation)
        self.assertEqual("build", preparation.category)
        self.assertIn("00:31", preparation.message)
        self.assertNotIn("異붿쟻", preparation.message)
        self.assertIsNotNone(started)
        self.assertEqual("build", started.category)
        self.assertIn("00:47", started.message)

    def test_parser_suppresses_repeated_changeling_queue_logs(self):
        parser = SC2EventParser(bot_name="Changeling")

        first = parser.parse_event(
            "2026-07-08 21:58:24 | INFO | 03:05 Structure queue: [UnitTypeId.BANELINGNEST]"
        )
        second = parser.parse_event(
            "2026-07-08 21:58:26 | INFO | 03:07 Structure queue: [UnitTypeId.BANELINGNEST]"
        )
        upgrade = parser.parse_event(
            "2026-07-08 21:58:27 | INFO | 03:09 Upgrade queue: [UpgradeId.ZERGLINGMOVEMENTSPEED]"
        )

        self.assertEqual("build", first.category)
        self.assertTrue(bool(str(first.message or "").strip()))
        self.assertNotIn("BANELINGNEST", first.message)
        self.assertIsNone(second)
        self.assertEqual("upgrade", upgrade.category)
        self.assertTrue(bool(str(upgrade.message or "").strip()))
        self.assertNotIn("ZERGLINGMOVEMENTSPEED", upgrade.message)

    def test_tts_bridge_uses_injected_callback(self):
        spoken = []
        bridge = SC2TTSBridge(speak_callback=spoken.append)

        result = bridge.speak("Changeling event")

        self.assertTrue(result["ok"])
        self.assertEqual("callback", result["method"])
        self.assertEqual(["Changeling event"], spoken)

    def test_tts_bridge_uses_narrow_pending_cancel_api(self):
        tts = mock.Mock()
        bridge = SC2TTSBridge(tts=tts)

        result = bridge.cancel_pending(reason="starcraft2_game_ended")

        self.assertTrue(result["ok"])
        self.assertEqual("cancel_pending", result["method"])
        tts.cancel_pending.assert_called_once_with(reason="starcraft2_game_ended")
        tts.handle_interrupt.assert_not_called()

    #20260711_kpopmodder: Keep the active repo runtime observer connected to
    # the main SC2 status-event path without duplicate unit/build commentary.
    def test_resolved_log_paths_include_repo_runtime_without_mutating_config(self):
        plugin_root = os.path.normpath("C:\\repo\\plugins\\StarCraft2")
        extension = StarCraft2Extension(
            plugin_root=plugin_root,
            config_path="missing.json",
        )
        runtime_stdout = os.path.join(
            plugin_root,
            "runtime",
            "Bots",
            "changeling",
            "data",
            "stdout.log",
        )
        configured_paths = ["C:\\custom\\probots.log", runtime_stdout]
        extension.config = {
            "preferred_bot": "changeling",
            "log_paths": list(configured_paths),
        }

        resolved = extension._resolved_log_paths()

        self.assertEqual(configured_paths, extension.config["log_paths"])
        self.assertIn(os.path.normpath("C:\\custom\\probots.log"), resolved)
        self.assertIn(os.path.normpath(runtime_stdout), resolved)
        self.assertIn(
            os.path.normpath(
                os.path.join(
                    plugin_root,
                    "runtime",
                    "Bots",
                    "changeling",
                    "data",
                    "stderr.log",
                )
            ),
            resolved,
        )
        self.assertEqual(3, len(resolved))

    def test_main_extension_publishes_status_callback_and_speaks_log_event(self):
        plugin = mock.Mock()
        tts = mock.Mock()
        context = GameExtensionContext(llm=mock.Mock(), tts=tts)
        reaction_callback = mock.Mock(return_value=False)
        extension = StarCraft2GameExtension(plugin=plugin)

        with mock.patch.object(
            extension,
            "_build_status_callback",
            return_value=reaction_callback,
        ):
            extension.initialize(context)

        callback = context.get_shared(STARCRAFT2_STATUS_EVENT_CALLBACK_RESOURCE)
        event = {
            "event_type": "upgrade",
            "details": {
                "origin": STARCRAFT2_LOG_EVENT_ORIGIN,
                "message": "?닿? ?湲留?諛쒖뾽???쒖옉?덉뼱??",
                "speak": True,
            },
        }
        callback(event)

        self.assertTrue(callable(callback))
        reaction_callback.assert_called_once_with(event)
        tts.receive_input.assert_not_called()

    def test_shared_callback_routes_upgrade_and_strategy_but_mutes_build_train(self):
        status_callback = mock.Mock()
        tts = mock.Mock()
        context = GameExtensionContext(tts=tts)
        context.set_shared(
            STARCRAFT2_STATUS_EVENT_CALLBACK_RESOURCE,
            status_callback,
        )
        extension = StarCraft2Extension(
            plugin_root="C:\\fake\\StarCraft2",
            config_path="missing.json",
        )
        extension.initialize(context)
        extension.tts_bridge.speak = mock.Mock(return_value={"ok": True})

        extension._on_log_line(
            "runtime.log",
            "03:09 Upgrade queue: [UpgradeId.ZERGLINGMOVEMENTSPEED]",
        )
        extension._on_log_line(
            "runtime.log",
            "03:04: Changed army composition to: LING_BANE",
        )
        extension._on_log_line("runtime.log", "01:03 HATCHERY started")
        extension._on_log_line(
            "runtime.log",
            "bot.managers.build_runner:do_step - 17 02:32 QUEEN",
        )

        self.assertEqual(2, status_callback.call_count)
        upgrade_event = status_callback.call_args_list[0].args[0]
        strategy_event = status_callback.call_args_list[1].args[0]
        self.assertEqual("upgrade", upgrade_event["event_type"])
        self.assertEqual("strategy", strategy_event["event_type"])
        self.assertEqual(
            STARCRAFT2_LOG_EVENT_ORIGIN,
            upgrade_event["details"]["origin"],
        )
        self.assertTrue(bool(str(upgrade_event["details"]["message"] or "").strip()))
        self.assertTrue(bool(str(strategy_event["details"]["message"] or "").strip()))
        extension.tts_bridge.speak.assert_not_called()

    def test_direct_tts_is_not_used_without_shared_status_callback(self):
        extension = StarCraft2Extension(
            plugin_root="C:\\fake\\StarCraft2",
            config_path="missing.json",
        )
        extension.initialize(GameExtensionContext(tts=mock.Mock()))
        extension.tts_bridge.speak = mock.Mock(return_value={"ok": True})

        extension._on_log_line(
            "runtime.log",
            "03:09 Upgrade queue: [UpgradeId.ZERGLINGMOVEMENTSPEED]",
        )

        extension.tts_bridge.speak.assert_not_called()

    def test_generic_end_report_is_log_only_without_stale_tts_cancel(self):
        tts = mock.Mock()
        extension = StarCraft2Extension(
            plugin_root="C:\\fake\\StarCraft2",
            config_path="missing.json",
        )
        extension.initialize(GameExtensionContext(tts=tts))
        extension.tts_bridge.speak = mock.Mock(return_value={"ok": True})

        extension._on_log_line("runtime.log", "END GAME REPORT")

        tts.cancel_pending.assert_not_called()
        extension.tts_bridge.speak.assert_not_called()

        extension._on_log_line(
            "runtime.log",
            "Result.Victory against opponent HUMAN",
        )
        extension.tts_bridge.speak.assert_not_called()

    def test_launcher_reports_missing_path_without_spawning_process(self):
        launcher = ProBotsLauncher()

        with mock.patch("plugins.StarCraft2.starcraft2_core.probots_launcher.subprocess.Popen") as popen:
            result = launcher.start(app_path="C:\\missing\\SC2AIApp.exe")

        self.assertFalse(result["ok"])
        self.assertEqual("probots_app_not_found", result["error"])
        popen.assert_not_called()

    def test_launcher_builds_sc2aiapp_env_with_full_exe_sc2path(self):
        launcher = ProBotsLauncher()
        config = self._sc2aiapp_config()

        with mock.patch.dict("os.environ", {"PATH": "C:\\Existing"}, clear=True):
            env = launcher.build_sc2aiapp_env(config)

        self.assertEqual(config["starcraft2_exe_path"], env["SC2PATH"])
        self.assertTrue(env["PATH"].startswith(config["starcraft2_support64_path"]))
        self.assertIn(config["starcraft2_base_path"], env["PATH"])
        self.assertTrue(env["PATH"].endswith("C:\\Existing"))

    def test_launcher_start_sc2aiapp_uses_cmd_success_conditions(self):
        launcher = ProBotsLauncher()
        config = self._sc2aiapp_config()
        config["kill_existing_processes_before_launch"] = True
        process = mock.Mock()
        process.pid = 1234
        process.poll.return_value = None
        process.stdout = None
        process.stderr = None

        with mock.patch("plugins.StarCraft2.starcraft2_core.probots_launcher.os.path.isfile", return_value=True):
            with mock.patch("plugins.StarCraft2.starcraft2_core.probots_launcher.os.path.isdir", return_value=True):
                with mock.patch("plugins.StarCraft2.starcraft2_core.probots_launcher.subprocess.run") as run:
                    with mock.patch(
                        "plugins.StarCraft2.starcraft2_core.probots_launcher.subprocess.Popen",
                        return_value=process,
                    ) as popen:
                        result = launcher.start_sc2aiapp(config, capture_output=False)

        self.assertTrue(result["ok"])
        self.assertEqual(2, run.call_count)
        popen.assert_called_once()
        _, kwargs = popen.call_args
        self.assertEqual(
            "C:\\Vtuber_Souorce_Code\\StarCraft2\\SC2AIApp_2025_S1\\SC2AIApp_2025_S1",
            kwargs["cwd"],
        )
        self.assertEqual(config["starcraft2_exe_path"], kwargs["env"]["SC2PATH"])
        self.assertTrue(kwargs["env"]["PATH"].startswith(config["starcraft2_support64_path"]))

    def test_launcher_start_sc2aiapp_does_not_taskkill_when_disabled(self):
        launcher = ProBotsLauncher()
        config = self._sc2aiapp_config()
        config["kill_existing_processes_before_launch"] = False
        process = mock.Mock()
        process.pid = 1234
        process.poll.return_value = None
        process.stdout = None
        process.stderr = None

        with mock.patch("plugins.StarCraft2.starcraft2_core.probots_launcher.os.path.isfile", return_value=True):
            with mock.patch("plugins.StarCraft2.starcraft2_core.probots_launcher.os.path.isdir", return_value=True):
                with mock.patch("plugins.StarCraft2.starcraft2_core.probots_launcher.subprocess.run") as run:
                    with mock.patch(
                        "plugins.StarCraft2.starcraft2_core.probots_launcher.subprocess.Popen",
                        return_value=process,
                    ):
                        result = launcher.start_sc2aiapp(config, capture_output=False)

        self.assertTrue(result["ok"])
        run.assert_not_called()

    def test_ladder_proxy_launcher_reports_missing_executable(self):
        launcher = SC2LadderProxyLauncher()

        with mock.patch("plugins.StarCraft2.starcraft2_core.sc2_ladder_proxy_launcher.subprocess.Popen") as popen:
            result = launcher.start({"executable_path": ""})

        self.assertFalse(result["ok"])
        self.assertEqual("ladder_proxy_executable_missing", result["error"])
        popen.assert_not_called()

    def test_ladder_proxy_launcher_starts_external_process_with_sc2_env(self):
        launcher = SC2LadderProxyLauncher()
        process = mock.Mock()
        process.pid = 4321
        process.poll.return_value = None
        process.stdout = None
        process.stderr = None
        config = {
            "executable_path": "C:\\Tools\\Sc2LadderServer.exe",
            "working_directory": "C:\\Tools",
            "args": "--local",
            "starcraft2_exe_path": "C:\\SC2\\Versions\\Base97425\\SC2_x64.exe",
            "starcraft2_support64_path": "C:\\SC2\\Support64",
            "starcraft2_base_path": "C:\\SC2\\Versions\\Base97425",
        }

        with mock.patch("plugins.StarCraft2.starcraft2_core.sc2_ladder_proxy_launcher.os.path.isfile", return_value=True):
            with mock.patch("plugins.StarCraft2.starcraft2_core.sc2_ladder_proxy_launcher.os.path.isdir", return_value=True):
                with mock.patch(
                    "plugins.StarCraft2.starcraft2_core.sc2_ladder_proxy_launcher.subprocess.Popen",
                    return_value=process,
                ) as popen:
                    result = launcher.start(config, capture_output=False)

        self.assertTrue(result["ok"])
        popen.assert_called_once()
        self.assertEqual(
            [
                "C:\\Tools\\Sc2LadderServer.exe",
                "--executable",
                "C:\\SC2\\Versions\\Base97425\\SC2_x64.exe",
                "--local",
            ],
            popen.call_args.args[0],
        )
        self.assertEqual("C:\\Tools", popen.call_args.kwargs["cwd"])
        self.assertEqual(config["starcraft2_exe_path"], popen.call_args.kwargs["env"]["SC2PATH"])
        self.assertTrue(
            popen.call_args.kwargs["env"]["PATH"].startswith(
                "C:\\Tools\\jre\\bin;C:\\SC2\\Support64"
            )
        )

    def test_ladder_proxy_launcher_splits_quoted_args(self):
        launcher = SC2LadderProxyLauncher()

        result = launcher._normalize_args(
            '--human-name "LAV Human" --bot changeling --bot-dir "C:\\SC2 Bots"'
        )

        self.assertEqual(
            [
                "--human-name",
                "LAV Human",
                "--bot",
                "changeling",
                "--bot-dir",
                "C:\\SC2 Bots",
            ],
            result,
        )

    def test_ladder_proxy_launcher_restarts_unhealthy_running_process_when_enabled(self):
        launcher = SC2LadderProxyLauncher()
        stale_process = mock.Mock()
        stale_process.pid = 1111
        stale_process.poll.return_value = None
        replacement_process = mock.Mock()
        replacement_process.pid = 2222
        replacement_process.poll.return_value = None
        replacement_process.stdout = None
        replacement_process.stderr = None
        launcher.process = stale_process
        launcher.started_at = 100.0
        launcher.stdout_tail.append("[LavHumanVsBot] Starting LAVHuman vs changeling")
        config = {
            "executable_path": "C:\\Tools\\Sc2LadderServer.exe",
            "working_directory": "C:\\Tools",
            "args": [],
            "restart_unhealthy": True,
            "restart_unhealthy_after_sec": 20.0,
        }

        with mock.patch("plugins.StarCraft2.starcraft2_core.sc2_ladder_proxy_launcher.time.time", return_value=130.0):
            with mock.patch("plugins.StarCraft2.starcraft2_core.sc2_ladder_proxy_launcher.os.path.isfile", return_value=True):
                with mock.patch("plugins.StarCraft2.starcraft2_core.sc2_ladder_proxy_launcher.os.path.isdir", return_value=True):
                    with mock.patch(
                        "plugins.StarCraft2.starcraft2_core.sc2_ladder_proxy_launcher.subprocess.Popen",
                        return_value=replacement_process,
                    ) as popen:
                        result = launcher.start(config, capture_output=False)

        self.assertTrue(result["ok"])
        stale_process.terminate.assert_called_once()
        popen.assert_called_once()

    def test_ladder_proxy_launcher_checks_local_and_lan_ports(self):
        launcher = SC2LadderProxyLauncher()
        connection = mock.MagicMock()

        with mock.patch(
            "plugins.StarCraft2.starcraft2_core.sc2_ladder_proxy_launcher.socket.create_connection",
            return_value=connection,
        ) as create_connection:
            result = launcher.check_ports(
                {
                    "proxy_host": "192.168.0.67",
                    "ports": "5677,5678",
                    "connect_timeout_sec": 0.1,
                }
            )

        self.assertTrue(result["ok"])
        self.assertEqual(["127.0.0.1", "192.168.0.67"], result["hosts"])
        self.assertEqual([5677, 5678], result["ports"])
        self.assertEqual(4, create_connection.call_count)

    def test_starcraft2_facade_preserves_ladder_proxy_args_without_override(self):
        facade = StarCraft2()
        facade.config_manager.config["ladder_proxy"] = {
            "executable_path": "C:\\Tools\\LavHumanVsBot.exe",
            "args": ["--bot", "changeling"],
            "ports": [5677, 5678],
        }
        result = facade._ladder_proxy_config()

        self.assertEqual(["--bot", "changeling"], result["args"])
        self.assertEqual([5677, 5678], result["ports"])

#     @unittest.skip(LAN_LOBBY_ARCHIVED_SKIP)
#     def test_starcraft2_facade_adds_remote_human_args_for_joined_lobby_player(self):
#         facade = StarCraft2()
#         facade.config_manager.config["ladder_proxy"] = {
#             "executable_path": "C:\\Tools\\LavHumanVsBot.exe",
#             "args": ["--bot", "changeling"],
#             "ports": [5677, 5678],
#             "remote_human_enabled": True,
#             "remote_human_client_port": 5679,
#         }
#         facade.config_manager.config["lan_lobby"] = {
#             "proxy_ports": [5677, 5678],
#             "human_client_port": 5679,
#         }
#         with facade.lan_discovery._joined_lock:
#             facade.lan_discovery._joined_players["client-1"] = {
#                 "client_id": "client-1",
#                 "player_name": "RemoteHuman",
#                 "remote_addr": "192.168.0.20",
#                 "last_seen": 200.0,
#             }

#         result = facade._ladder_proxy_config()
#         facade._apply_remote_human_args(result)

#         self.assertEqual(
#             [
#                 "--bot",
#                 "changeling",
#                 "--remote-human-host",
#                 "192.168.0.20",
#                 "--remote-human-client-port",
#                 "5679",
#                 "--lan-connect-mode",
#                 "relay",
#                 "--lan-port-layout",
#                 "s2client-api-shared",
#                 "--remote-human-join-mode",
#                 "remote-native",
#             ],
#             result["args"],
#         )
#         self.assertEqual("192.168.0.20", result["remote_human"]["host"])
#         self.assertEqual("relay", result["remote_human"]["lan_connect_mode"])
#         self.assertEqual("s2client-api-shared", result["remote_human"]["lan_port_layout"])

#     @unittest.skip(LAN_LOBBY_ARCHIVED_SKIP)
#     def test_starcraft2_facade_adds_lan_game_host_ip_from_selected_bind_host(self):
#         facade = StarCraft2()
#         facade.config_manager.config["ladder_proxy"] = {
#             "executable_path": "C:\\Tools\\LavLanLadderServer.exe",
#             "args": ["--bot", "changeling"],
#             "ports": [5677, 5678],
#             "remote_human_enabled": True,
#             "remote_human_client_port": 5679,
#         }
#         facade.config_manager.config["lan_lobby"] = {
#             "proxy_ports": [5677, 5678],
#             "human_client_port": 5679,
#         }
#         with facade.lan_discovery._joined_lock:
#             facade.lan_discovery._joined_players["client-1"] = {
#                 "client_id": "client-1",
#                 "player_name": "RemoteHuman",
#                 "remote_addr": "192.168.0.20",
#                 "last_seen": 200.0,
#             }

#         result = facade._ladder_proxy_config()
#         result["multiplayer_relay"] = {"selected_bind_host": "192.168.0.10"}
#         facade._apply_remote_human_args(result)

#         self.assertEqual(
#             [
#                 "--bot",
#                 "changeling",
#                 "--remote-human-host",
#                 "192.168.0.20",
#                 "--remote-human-client-port",
#                 "5679",
#                 "--lan-game-host-ip",
#                 "192.168.0.10",
#                 "--lan-connect-mode",
#                 "relay",
#                 "--lan-port-layout",
#                 "s2client-api-shared",
#                 "--remote-human-join-mode",
#                 "remote-native",
#             ],
#             result["args"],
#         )
#         self.assertEqual("192.168.0.10", result["remote_human"]["lan_game_host_ip"])

#     @unittest.skip(LAN_LOBBY_ARCHIVED_SKIP)
#     def test_starcraft2_facade_can_pass_direct_lan_swapped_port_layout(self):
#         facade = StarCraft2()
#         facade.config_manager.config["ladder_proxy"] = {
#             "executable_path": "C:\\Tools\\LavLanLadderServer.exe",
#             "args": ["--bot", "changeling"],
#             "ports": [5677, 5678],
#             "remote_human_enabled": True,
#             "remote_human_client_port": 5679,
#         }
#         facade.config_manager.config["lan_lobby"] = {
#             "proxy_host": "192.168.0.10",
#             "proxy_ports": [5677, 5678],
#             "human_client_port": 5679,
#             "lan_connect_mode": "direct",
#             "lan_port_layout": "swapped",
#             "multiplayer_relay_enabled": False,
#         }
#         with facade.lan_discovery._joined_lock:
#             facade.lan_discovery._joined_players["client-1"] = {
#                 "client_id": "client-1",
#                 "player_name": "RemoteHuman",
#                 "remote_addr": "192.168.0.20",
#                 "last_seen": 200.0,
#             }

#         result = facade._ladder_proxy_config()
#         result["multiplayer_relay"] = {"lan_connect_mode": "direct"}
#         facade._apply_remote_human_args(result)

#         self.assertIn("--lan-connect-mode", result["args"])
#         self.assertIn("direct", result["args"])
#         self.assertIn("--lan-port-layout", result["args"])
#         self.assertIn("swapped", result["args"])
#         self.assertEqual("192.168.0.10", result["remote_human"]["lan_game_host_ip"])
#         self.assertEqual("direct", result["remote_human"]["lan_connect_mode"])
#         self.assertEqual("swapped", result["remote_human"]["lan_port_layout"])

#     @unittest.skip(LAN_LOBBY_ARCHIVED_SKIP)
#     def test_lan_port_layout_helpers_match_native_contract(self):
#         self.assertEqual("swapped", normalize_lan_port_layout("swap"))
#         self.assertEqual(
#             "role-server-peer-client",
#             normalize_lan_port_layout("unknown"),
#         )
#         self.assertEqual([5692, 5693], derive_first_player_server_ports(5690))
#         self.assertEqual([5694, 5695], derive_first_player_client_ports(5690))
#         self.assertEqual([5694, 5695], derive_second_player_server_ports(5690))
#         self.assertEqual([5692, 5693], derive_second_player_client_ports(5690))
#         self.assertEqual(
#             [5694, 5695],
#             derive_first_player_server_ports(5690, "swapped"),
#         )
#         self.assertEqual(
#             [5692, 5693],
#             derive_first_player_client_ports(5690, "swapped"),
#         )
#         self.assertEqual(
#             [5692, 5693],
#             derive_second_player_server_ports(5690, "swapped"),
#         )
#         self.assertEqual(
#             [5694, 5695],
#             derive_second_player_client_ports(5690, "swapped"),
#         )
#         self.assertEqual(
#             "host-server-remote-client",
#             normalize_lan_port_layout("host-server"),
#         )
#         self.assertEqual(
#             [5694, 5695],
#             derive_first_player_server_ports(5690, "host-server-remote-client"),
#         )
#         self.assertEqual(
#             [5692, 5693],
#             derive_first_player_client_ports(5690, "host-server-remote-client"),
#         )
#         self.assertEqual(
#             [5694, 5695],
#             derive_second_player_server_ports(5690, "host-server-remote-client"),
#         )
#         self.assertEqual(
#             [5692, 5693],
#             derive_second_player_client_ports(5690, "host-server-remote-client"),
#         )
#         self.assertEqual(
#             "s2client-api-shared",
#             normalize_lan_port_layout("official"),
#         )
#         self.assertEqual(
#             [5692, 5693],
#             derive_first_player_server_ports(5690, "s2client-api-shared"),
#         )
#         self.assertEqual(
#             [5694, 5695],
#             derive_first_player_client_ports(5690, "s2client-api-shared"),
#         )
#         self.assertEqual(
#             [5692, 5693],
#             derive_second_player_server_ports(5690, "s2client-api-shared"),
#         )
#         self.assertEqual(
#             [5694, 5695],
#             derive_second_player_client_ports(5690, "s2client-api-shared"),
#         )

    def test_starcraft2_local_match_defaults_survive_archived_lan_proxy(self):
        facade = StarCraft2()

        local_config = facade._local_match_config()
        lan_config = facade._ladder_proxy_config()

        self.assertTrue(local_config["executable_path"].endswith("LavHumanVsBot.exe"))
        self.assertEqual("", lan_config["executable_path"])
        self.assertTrue(lan_config["archived"])
        self.assertFalse(local_config["remote_human_enabled"])
        self.assertFalse(lan_config["remote_human_enabled"])

    def test_starcraft2_local_match_config_strips_remote_human_args(self):
        facade = StarCraft2()
        facade.config_manager.config["local_match"] = {
            "executable_path": "C:\\Tools\\LavHumanVsBot.exe",
            "args": [
                "--bot",
                "changeling",
                "--remote-human-host",
                "192.168.0.20",
                "--remote-human-client-port=5679",
            ],
            "ports": [5677, 5678],
            "remote_human_enabled": True,
        }

        result = facade._local_match_config(
            proxy_ports="5677,5678",
        )

        self.assertEqual(
            [],
            result["args"],
        )
        self.assertFalse(result["remote_human_enabled"])
        self.assertEqual("", result["proxy_host"])
        self.assertEqual(["127.0.0.1"], result["check_hosts"])
        self.assertEqual("local_human_vs_changeling", result["mode"])

    def test_starcraft2_local_match_config_strips_existing_bot_race_arg(self):
        facade = StarCraft2()
        facade.config_manager.config["local_match"] = {
            "executable_path": "C:\\Tools\\LavHumanVsBot.exe",
            "args": ["--bot", "changeling", "--bot-race=Zerg"],
            "ports": [5677, 5678],
        }

        result = facade._local_match_config()

        self.assertEqual(
            [],
            result["args"],
        )

    def test_starcraft2_local_match_race_change_replaces_race_arg(self):
        facade = StarCraft2()
        cases = (
            ("--bot changeling --race Terran", "Protoss"),
            ("--bot changeling --race=Zerg", "Protoss"),
            ("--bot changeling", "Protoss"),
            (
                "--bot changeling --race Terran --bot-race Zerg "
                "--remote-human-host 192.168.0.20",
                "Protoss",
            ),
        )

        for args, race in cases:
            with self.subTest(args=args):
                result = facade.on_local_match_race_change(race, args)
                normalized = facade._normalize_ladder_args(result)

                self.assertEqual(
                    ["--race", "Protoss"],
                    normalized,
                )
                self.assertEqual(1, normalized.count("--race"))

    def test_starcraft2_local_match_race_change_preserves_quoted_args(self):
        facade = StarCraft2()

        result = facade.on_local_match_race_change(
            "Zerg",
            '--bot changeling --bot-dir "C:\\SC2 Bots" --race Terran',
        )

        self.assertEqual(
            [
                "--bot-dir",
                "C:\\SC2 Bots",
                "--race",
                "Zerg",
            ],
            facade._normalize_ladder_args(result),
        )

    def test_starcraft2_local_match_race_from_args_supports_both_forms(self):
        facade = StarCraft2()

        self.assertEqual(
            "Zerg",
            facade._local_match_race_from_args("--bot changeling --race Zerg"),
        )
        self.assertEqual(
            "Protoss",
            facade._local_match_race_from_args("--race=Protoss --bot changeling"),
        )
        self.assertEqual(
            "Terran",
            facade._local_match_race_from_args("--bot changeling"),
        )

    def test_starcraft2_local_match_ai_race_maps_to_bot(self):
        facade = StarCraft2()
        for race, _ in (("Terran", "BenBotBC"), ("Protoss", "sharkbot"), ("Zerg", "changeling")):
            with self.subTest(race=race):
                result = facade.on_local_match_ai_race_change(
                    race,
                    "--bot changeling --race Protoss",
                )
                self.assertEqual(
                    ["--race", "Protoss"],
                    facade._normalize_ladder_args(result),
                )

    def test_starcraft2_local_match_random_ai_is_rejected(self):
        facade = StarCraft2()
        with mock.patch.object(facade.ladder_proxy, "start") as start:
            result = facade.on_local_human_vs_changeling_click(
                "C:\\Tools\\LavHumanVsBot.exe",
                "C:\\Tools",
                "--bot changeling --race Protoss",
                "5677,5678",
                "Random",
            )
        start.assert_not_called()
        self.assertIn("local_match_random_ai_not_supported", result)

    def test_starcraft2_rejects_invalid_sharkbot_runtime_before_launch(self):
        facade = StarCraft2()
        with mock.patch.object(facade.ladder_proxy, "start") as start:
            result = facade.on_local_human_vs_changeling_click(
                "C:\\Tools\\LavHumanVsBot.exe",
                "C:\\MissingRuntime",
                "--bot changeling --race Terran",
                "5677,5678",
                "Protoss",
            )

        start.assert_not_called()
        self.assertIn("bot_runtime_missing", result)

    def test_starcraft2_local_human_button_starts_without_remote_human_args(self):
        facade = StarCraft2()

        with mock.patch(
            "plugins.StarCraft2.bot_launch_profiles.os.path.isfile",
            return_value=True,
        ):
            with mock.patch.object(
                facade.ladder_proxy,
                "start",
                return_value={"ok": True, "running": True},
            ) as start:
                facade.on_local_human_vs_changeling_click(
                    "C:\\Tools\\LavHumanVsBot.exe",
                    "C:\\Tools",
                    "--bot changeling --race Protoss --bot-race Terran",
                    "5677,5678",
                )

        started_config = start.call_args.args[0]
        self.assertEqual(
            ["--race", "Protoss"],
            started_config["args"],
        )
        self.assertNotIn("remote_human", started_config)
        self.assertFalse(started_config["remote_human_enabled"])

    def test_log_watcher_tails_new_lines(self):
        path = "C:\\logs\\probots.log"
        lines = []
        watcher = ProBotsLogWatcher(poll_interval_sec=0.05)
        watcher._callback = lambda log_path, line: lines.append((log_path, line))
        state = WatchedLogState(path=path, offset=0, exists=True)
        stream = _FakeBinaryFile(b"build barracks\n")

        with mock.patch("plugins.StarCraft2.starcraft2_core.probots_log_watcher.os.path.isfile", return_value=True):
            with mock.patch("plugins.StarCraft2.starcraft2_core.probots_log_watcher.os.path.getsize", return_value=15):
                with mock.patch("plugins.StarCraft2.starcraft2_core.probots_log_watcher.open", return_value=stream):
                    watcher._poll_state(state)

        self.assertEqual([(path, "build barracks")], lines)
        self.assertEqual(15, state.offset)
        self.assertEqual(1, state.lines_seen)

    def test_extension_mutes_start_notice_but_speaks_live_commentary(self):
        extension = StarCraft2Extension(plugin_root="C:\\fake\\StarCraft2", config_path="missing.json")
        spoken = []
        config = {
            "enabled": True,
            "probots_app_path": "",
            "starcraft2_install_path": "C:\\Program Files (x86)\\StarCraft II",
            "maps_path": "",
            "preferred_bot": "Changeling",
            "auto_launch_probots": False,
            "log_paths": ["probots.log"],
            "watch_stdout": True,
            "speak_events": True,
        }

        shared_status_callback = mock.Mock(side_effect=lambda event: spoken.append(event))
        context = GameExtensionContext(tts=mock.Mock())
        context.set_shared(
            STARCRAFT2_STATUS_EVENT_CALLBACK_RESOURCE,
            shared_status_callback,
        )
        extension.initialize(context)
        with mock.patch("plugins.StarCraft2.starcraft2_core.sc2_extension.log_print") as log_print:
            with mock.patch.object(extension, "_load_config", return_value=config):
                with mock.patch.object(extension.log_watcher, "start", return_value={"ok": True}) as watcher_start:
                    with mock.patch.object(extension.launcher, "start") as launcher_start:
                        extension.start()
                        extension._on_log_line("probots.log", "Starting game vs human")
                        extension._on_log_line("probots.log", "enemy rush detected")

            status = extension.get_status()
        logged = "\n".join(str(call.args[0]) for call in log_print.call_args_list)
        self.assertGreaterEqual(len(spoken), 2)
        event_types = [evt.get("event_type", "") for evt in spoken if isinstance(evt, dict)]
        self.assertIn("game_started", event_types)
        self.assertTrue(any(event_type != "game_started" for event_type in event_types))
        self.assertIn("[StarCraft2LogCommentary]", logged)
        self.assertNotIn("Changeling", logged)
        self.assertTrue(status["started"])
        watcher_start.assert_called_once()
        launcher_start.assert_not_called()

    def test_extension_can_launch_sc2aiapp_by_explicit_command(self):
        extension = StarCraft2Extension(plugin_root="C:\\fake\\StarCraft2", config_path="missing.json")
        config = self._sc2aiapp_config()
        config.update(
            {
                "enabled": True,
                "auto_launch_probots": False,
                "watch_stdout": True,
                "speak_events": True,
            }
        )

        with mock.patch.object(extension, "_load_config", return_value=config):
            with mock.patch.object(
                extension.launcher,
                "start_sc2aiapp",
                return_value={"ok": True, "running": True},
            ) as launcher_start:
                result = extension.handle_command({"action": "launch_sc2aiapp"})

        self.assertTrue(result["ok"])
        launcher_start.assert_called_once()

#     @unittest.skip(LAN_LOBBY_ARCHIVED_SKIP)
#     def test_lan_discovery_payload_roundtrip_discovers_room(self):
#         host = SC2LanDiscovery(now_func=lambda: 100.0)
#         client = SC2LanDiscovery(now_func=lambda: 101.0)

#         payload = host._build_payload(
#             {
#                 "room_name": "Test Room",
#                 "player_name": "Host",
#                 "preferred_bot": "Changeling",
#                 "preferred_map": "PersephoneLE.SC2Map",
#                 "map_file_name": "PersephoneLE.SC2Map",
#                 "map_size": 930000,
#                 "map_sha256": "abc123",
#                 "map_download_port": DEFAULT_MAP_DOWNLOAD_PORT,
#                 "map_download_path": "/map/PersephoneLE.SC2Map",
#                 "proxy_host": "192.168.0.10",
#                 "proxy_ports": "5677,5678",
#             }
#         )
#         data = json_bytes(payload)
#         room = client._parse_payload(data, ("192.168.0.10", 47624))
#         client._remember_room(room)

#         rooms = client.rooms()
#         self.assertEqual(1, len(rooms))
#         self.assertEqual("Test Room", rooms[0]["room_name"])
#         self.assertEqual("Changeling", rooms[0]["preferred_bot"])
#         self.assertEqual([5677, 5678], rooms[0]["proxy_ports"])
#         self.assertEqual(47625, rooms[0]["join_port"])
#         self.assertEqual(5679, rooms[0]["human_client_port"])
#         self.assertEqual(DEFAULT_REMOTE_START_PORT, rooms[0]["remote_start_port"])
#         self.assertEqual("PersephoneLE.SC2Map", rooms[0]["map_file_name"])
#         self.assertEqual(930000, rooms[0]["map_size"])
#         self.assertEqual("abc123", rooms[0]["map_sha256"])
#         self.assertEqual(DEFAULT_MAP_DOWNLOAD_PORT, rooms[0]["map_download_port"])
#         self.assertEqual("/map/PersephoneLE.SC2Map", rooms[0]["map_download_path"])

#     @unittest.skip(LAN_LOBBY_ARCHIVED_SKIP)
#     def test_lan_discovery_does_not_advertise_wildcard_or_loopback_proxy_host(self):
#         discovery = SC2LanDiscovery(now_func=lambda: 100.0)

#         with mock.patch.object(discovery, "_local_ip_hint", return_value="26.189.202.71"):
#             wildcard_room = discovery._normalize_room_info({"proxy_host": "0.0.0.0"})
#             loopback_room = discovery._normalize_room_info({"proxy_host": "127.0.0.1"})
#             stale_room = discovery._normalize_room_info({"proxy_host": "10.0.0.5"})

#         self.assertEqual("26.189.202.71", wildcard_room["proxy_host"])
#         self.assertEqual("26.189.202.71", loopback_room["proxy_host"])
#         self.assertEqual("26.189.202.71", stale_room["proxy_host"])
#         self.assertEqual("10.0.0.5", stale_room["configured_proxy_host"])

#     @unittest.skip(LAN_LOBBY_ARCHIVED_SKIP)
#     def test_starcraft2_lan_relay_bind_uses_joined_peer_route_not_stale_proxy_host(self):
#         facade = StarCraft2()
#         facade.config_manager.config["lan_lobby"] = {
#             "multiplayer_relay_enabled": True,
#             "multiplayer_relay_bind_host": "",
#             "start_port": 5690,
#         }
#         facade.lan_discovery._host_room = {
#             "proxy_host": "10.0.0.5",
#             "configured_proxy_host": "10.0.0.5",
#             "start_port": 5690,
#         }
#         with facade.lan_discovery._joined_lock:
#             facade.lan_discovery._joined_players["client-1"] = {
#                 "client_id": "client-1",
#                 "player_name": "RemoteHuman",
#                 "remote_addr": "192.168.0.20",
#                 "last_seen": 200.0,
#             }

#         with (
#             mock.patch(
#                 "plugins.StarCraft2.starcraft2.resolve_lan_bind_host",
#                 return_value="192.168.0.10",
#             ) as resolve_bind_host,
#             mock.patch.object(
#                 facade.lan_port_relay,
#                 "start",
#                 return_value={"ok": True, "running": True},
#             ),
#             mock.patch.object(
#                 facade.lan_loopback_relay,
#                 "start",
#                 return_value={"ok": True, "running": True},
#             ),
#             mock.patch.object(
#                 facade.lan_udp_pair_relay,
#                 "start",
#                 return_value={"ok": True, "running": True},
#             ),
#         ):
#             result = facade._start_lan_multiplayer_relay({"proxy_host": "10.0.0.5"})

#         resolve_bind_host.assert_called_once_with(
#             "",
#             peer_host="192.168.0.20",
#         )
#         self.assertEqual("192.168.0.20", result["selected_peer_host"])
#         self.assertEqual("192.168.0.10", result["selected_bind_host"])
#         self.assertEqual("10.0.0.5", result["configured_proxy_host"])

#     @unittest.skip(LAN_LOBBY_ARCHIVED_SKIP)
#     def test_starcraft2_lan_host_attaches_map_download_metadata(self):
#         facade = StarCraft2()
#         facade.config_manager.config["starcraft2_path"] = r"C:\Program Files (x86)\StarCraft II"
#         facade.config_manager.config["lan_lobby"] = {
#             "map_download_port": DEFAULT_MAP_DOWNLOAD_PORT,
#             "auto_serve_map": True,
#         }
#         room_info = {"preferred_map": "WrongFallback.SC2Map"}
#         map_result = {
#             "ok": True,
#             "map_file_name": "PersephoneLE.SC2Map",
#             "map_size": 930000,
#             "map_sha256": "abc123",
#             "map_download_port": DEFAULT_MAP_DOWNLOAD_PORT,
#             "map_download_path": "/map/PersephoneLE.SC2Map",
#         }

#         with mock.patch.object(facade.map_file_server, "start", return_value=map_result) as start:
#             result = facade._prepare_lan_map_download(
#                 room_info,
#                 {"args": ["--map", "PersephoneLE.SC2Map"]},
#             )

#         self.assertTrue(result["ok"])
#         start.assert_called_once_with(
#             r"C:\Program Files (x86)\StarCraft II\Maps\PersephoneLE.SC2Map",
#             DEFAULT_MAP_DOWNLOAD_PORT,
#         )
#         self.assertEqual("PersephoneLE.SC2Map", room_info["map_file_name"])
#         self.assertEqual("abc123", room_info["map_sha256"])

#     @unittest.skip(LAN_LOBBY_ARCHIVED_SKIP)
#     def test_lan_discovery_records_lobby_join_request(self):
#         discovery = SC2LanDiscovery(now_func=lambda: 200.0)
#         discovery._host_room = discovery._normalize_room_info(
#             {
#                 "room_id": "room-a",
#                 "room_name": "LAV Room",
#             }
#         )
#         join_payload = {
#             "protocol": LAN_JOIN_PROTOCOL,
#             "version": LAN_JOIN_VERSION,
#             "room_id": "room-a",
#             "client_id": "client-1",
#             "player_name": "Human",
#             "host_name": "JoinPC",
#             "human_client_port": 5679,
#             "remote_start_port": DEFAULT_REMOTE_START_PORT,
#             "timestamp": 199.5,
#         }

#         player, error = discovery._parse_join_payload(
#             json_bytes(join_payload),
#             ("192.168.0.20", 50000),
#         )
#         self.assertEqual("", error)
#         discovery._remember_joined_player(player)

#         status = discovery.get_status()
#         self.assertEqual(47625, status["join_port"])
#         self.assertEqual(1, len(status["joined_players"]))
#         self.assertEqual("Human", status["joined_players"][0]["player_name"])
#         self.assertEqual("192.168.0.20", status["joined_players"][0]["remote_addr"])
#         self.assertEqual(DEFAULT_REMOTE_START_PORT, status["joined_players"][0]["remote_start_port"])

#     @unittest.skip(LAN_LOBBY_ARCHIVED_SKIP)
#     def test_lan_discovery_accepts_tcp_lobby_join_request(self):
#         join_port = get_free_tcp_port()
#         discovery = SC2LanDiscovery(
#             discovery_port=get_free_udp_port(),
#             join_port=join_port,
#             broadcast_addresses=["127.0.0.1"],
#             now_func=lambda: 200.0,
#         )
#         discovery.start_host({"room_id": "room-a", "room_name": "LAV Room"})
#         self.addCleanup(discovery.stop_host)
#         join_payload = {
#             "protocol": LAN_JOIN_PROTOCOL,
#             "version": LAN_JOIN_VERSION,
#             "room_id": "room-a",
#             "client_id": "client-1",
#             "player_name": "Human",
#             "host_name": "JoinPC",
#             "human_client_port": 5679,
#             "remote_start_port": DEFAULT_REMOTE_START_PORT,
#             "timestamp": 199.5,
#         }

#         with socket.create_connection(("127.0.0.1", join_port), timeout=5.0) as sock:
#             sock.settimeout(5.0)
#             sock.sendall(json_bytes(join_payload))
#             sock.shutdown(socket.SHUT_WR)
#             response = sock.recv(8192)

#         ack = json_loads(response)
#         self.assertTrue(ack["ok"])
#         self.assertEqual("joined", ack["message"])
#         self.assertEqual("client-1", ack["client_id"])
#         status = discovery.get_status()
#         self.assertEqual(1, len(status["joined_players"]))
#         self.assertEqual("Human", status["joined_players"][0]["player_name"])
#         self.assertEqual("127.0.0.1", status["joined_players"][0]["remote_addr"])

#     @unittest.skip(LAN_LOBBY_ARCHIVED_SKIP)
#     def test_lan_discovery_rejects_invalid_tcp_lobby_join_with_ack(self):
#         join_port = get_free_tcp_port()
#         discovery = SC2LanDiscovery(
#             discovery_port=get_free_udp_port(),
#             join_port=join_port,
#             broadcast_addresses=["127.0.0.1"],
#             now_func=lambda: 200.0,
#         )
#         discovery.start_host({"room_id": "room-a", "room_name": "LAV Room"})
#         self.addCleanup(discovery.stop_host)

#         with socket.create_connection(("127.0.0.1", join_port), timeout=5.0) as sock:
#             sock.settimeout(5.0)
#             sock.sendall(json_bytes({"protocol": "not.lav.join", "version": LAN_JOIN_VERSION}))
#             sock.shutdown(socket.SHUT_WR)
#             response = sock.recv(8192)

#         ack = json_loads(response)
#         self.assertFalse(ack["ok"])
#         self.assertEqual("unsupported_protocol", ack["message"])
#         self.assertEqual([], discovery.get_status()["joined_players"])

#     @unittest.skip(LAN_LOBBY_ARCHIVED_SKIP)
#     def test_lan_discovery_accepts_udp_lobby_join_request(self):
#         join_port = get_free_udp_port()
#         discovery = SC2LanDiscovery(
#             discovery_port=get_free_udp_port(),
#             join_port=join_port,
#             broadcast_addresses=["127.0.0.1"],
#             now_func=lambda: 200.0,
#         )
#         discovery.start_host({"room_id": "room-a", "room_name": "LAV Room"})
#         self.addCleanup(discovery.stop_host)
#         join_payload = {
#             "protocol": LAN_JOIN_PROTOCOL,
#             "version": LAN_JOIN_VERSION,
#             "room_id": "room-a",
#             "client_id": "client-udp",
#             "player_name": "Human",
#             "host_name": "JoinPC",
#             "human_client_port": 5679,
#             "remote_start_port": DEFAULT_REMOTE_START_PORT,
#             "timestamp": 199.5,
#         }

#         with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
#             sock.settimeout(5.0)
#             sock.sendto(json_bytes(join_payload), ("127.0.0.1", join_port))
#             response, _ = sock.recvfrom(8192)

#         ack = json_loads(response)
#         self.assertTrue(ack["ok"])
#         self.assertEqual("joined", ack["message"])
#         self.assertEqual("client-udp", ack["client_id"])
#         status = discovery.get_status()
#         self.assertEqual(1, len(status["joined_players"]))
#         self.assertEqual("Human", status["joined_players"][0]["player_name"])
#         self.assertEqual("127.0.0.1", status["joined_players"][0]["remote_addr"])

#     @unittest.skip(LAN_LOBBY_ARCHIVED_SKIP)
#     def test_lan_discovery_replaces_duplicate_remote_human_join(self):
#         now = [200.0]
#         discovery = SC2LanDiscovery(now_func=lambda: now[0])
#         first = {
#             "client_id": "client-1",
#             "room_id": "room-a",
#             "player_name": "Human",
#             "remote_addr": "192.168.0.20",
#             "remote_port": 50000,
#             "human_client_port": 5679,
#             "remote_start_port": DEFAULT_REMOTE_START_PORT,
#             "joined_at": 199.0,
#         }
#         second = {
#             "client_id": "client-2",
#             "room_id": "room-a",
#             "player_name": "Human",
#             "remote_addr": "192.168.0.20",
#             "remote_port": 50001,
#             "human_client_port": 5679,
#             "remote_start_port": DEFAULT_REMOTE_START_PORT,
#             "joined_at": 200.0,
#         }

#         discovery._remember_joined_player(first)
#         now[0] = 201.0
#         discovery._remember_joined_player(second)

#         status = discovery.get_status()
#         self.assertEqual(1, len(status["joined_players"]))
#         self.assertEqual("client-2", status["joined_players"][0]["client_id"])
#         self.assertEqual(50001, status["joined_players"][0]["remote_port"])

#     @unittest.skip(LAN_LOBBY_ARCHIVED_SKIP)
#     def test_starcraft2_start_requests_remote_human_before_proxy_launch(self):
#         facade = StarCraft2()
#         facade.config_manager.config["ladder_proxy"] = {
#             "executable_path": "C:\\Tools\\LavHumanVsBot.exe",
#             "working_directory": "C:\\Tools",
#             "args": ["--bot", "changeling"],
#             "ports": [5677, 5678],
#             "remote_human_enabled": True,
#             "remote_human_client_port": 5679,
#         }
#         facade.config_manager.config["lan_lobby"] = {
#             "proxy_ports": [5677, 5678],
#             "human_client_port": 5679,
#             "remote_start_port": DEFAULT_REMOTE_START_PORT,
#         }
#         with facade.lan_discovery._joined_lock:
#             facade.lan_discovery._joined_players["client-1"] = {
#                 "client_id": "client-1",
#                 "player_name": "RemoteHuman",
#                 "remote_addr": "192.168.0.20",
#                 "remote_start_port": DEFAULT_REMOTE_START_PORT,
#                 "last_seen": 200.0,
#             }

#         with (
#             mock.patch.object(
#                 facade.lan_discovery,
#                 "request_remote_human_start",
#                 return_value={"ok": True, "pid": 1234},
#             ) as request_remote_start,
#             mock.patch.object(
#                 facade.ladder_proxy,
#                 "start",
#                 return_value={"ok": True, "running": True},
#             ) as start,
#             mock.patch.object(
#                 facade,
#                 "_start_lan_multiplayer_relay",
#                 return_value={"ok": True, "running": True},
#             ),
#         ):
#             facade.on_ladder_proxy_start_click(
#                 "C:\\Tools\\LavHumanVsBot.exe",
#                 "C:\\Tools",
#                 "--bot changeling",
#                 "",
#                 "5677,5678",
#             )

#         request_remote_start.assert_called_once()
#         started_config = start.call_args.args[0]
#         self.assertEqual("192.168.0.20", started_config["remote_human"]["host"])
#         self.assertEqual({"ok": True, "pid": 1234}, started_config["remote_human_start"])

#     @unittest.skip(LAN_LOBBY_ARCHIVED_SKIP)
#     def test_starcraft2_does_not_start_proxy_when_remote_human_start_fails(self):
#         facade = StarCraft2()
#         with facade.lan_discovery._joined_lock:
#             facade.lan_discovery._joined_players["client-1"] = {
#                 "client_id": "client-1",
#                 "player_name": "RemoteHuman",
#                 "remote_addr": "192.168.0.20",
#                 "remote_start_port": DEFAULT_REMOTE_START_PORT,
#                 "last_seen": 200.0,
#             }

#         with (
#             mock.patch.object(
#                 facade.lan_discovery,
#                 "request_remote_human_start",
#                 return_value={"ok": False, "error": "connection refused"},
#             ),
#             mock.patch.object(facade.ladder_proxy, "start") as start,
#         ):
#             _, status_json = facade.on_ladder_proxy_start_click(
#                 "C:\\Tools\\LavHumanVsBot.exe",
#                 "C:\\Tools",
#                 "--bot changeling",
#                 "",
#                 "5677,5678",
#             )

#         start.assert_not_called()
#         self.assertIn("connection refused", status_json)

#     @unittest.skip(LAN_LOBBY_ARCHIVED_SKIP)
#     def test_lan_discovery_prunes_stale_rooms(self):
#         now = [100.0]
#         discovery = SC2LanDiscovery(room_ttl_sec=5.0, now_func=lambda: now[0])
#         discovery._remember_room(
#             {
#                 "room_id": "room1",
#                 "room_name": "Old Room",
#                 "last_seen": 100.0,
#                 "expires_sec": 5.0,
#             }
#         )

#         now[0] = 106.0

#         self.assertEqual([], discovery.rooms())

#     @unittest.skip(LAN_LOBBY_ARCHIVED_SKIP)
#     def test_extension_lan_host_command_uses_changeling_metadata(self):
#         extension = StarCraft2Extension(plugin_root="C:\\fake\\StarCraft2", config_path="missing.json")
#         config = self._sc2aiapp_config()
#         config.update(
#             {
#                 "enabled": True,
#                 "preferred_bot": "Changeling",
#                 "preferred_map": "PersephoneLE.SC2Map",
#                 "lan_lobby": {
#                     "room_name": "LAV Room",
#                     "player_name": "Tester",
#                     "proxy_ports": [5677, 5678],
#                     "start_port": 5690,
#                 },
#             }
#         )

#         with mock.patch.object(extension, "_load_config", return_value=config):
#             with mock.patch.object(
#                 extension.lan_discovery,
#                 "start_host",
#                 return_value={"ok": True, "hosting": True},
#             ) as start_host:
#                 result = extension.handle_command({"action": "lan_host_room"})

#         self.assertTrue(result["ok"])
#         start_host.assert_called_once()
#         room_info = start_host.call_args.args[0]
#         self.assertEqual("LAV Room", room_info["room_name"])
#         self.assertEqual("Changeling", room_info["preferred_bot"])
#         self.assertEqual("PersephoneLE.SC2Map", room_info["preferred_map"])

#     def test_extension_reports_archived_ladder_proxy_status_command(self):
#         extension = StarCraft2Extension(plugin_root="C:\\fake\\StarCraft2", config_path="missing.json")
#         config = self._sc2aiapp_config()
#         config.update(
#             {
#                 "enabled": True,
#                 "ladder_proxy": {
#                     "executable_path": "C:\\Tools\\Sc2LadderServer.exe",
#                     "ports": [5677, 5678],
#                 },
#             }
#         )

#         with mock.patch.object(extension, "_load_config", return_value=config):
#             result = extension.handle_command({"action": "check_proxy_ports"})

#         self.assertFalse(result["ok"])
#         self.assertEqual("check_proxy_ports", result["action"])
#         self.assertEqual("lan_lobby_archived", result["error"])
#         self.assertEqual({"archived": True}, result["status"])

    def _sc2aiapp_config(self):
        return {
            "sc2aiapp_path": "C:\\Vtuber_Souorce_Code\\StarCraft2\\SC2AIApp_2025_S1\\SC2AIApp_2025_S1\\SC2AIApp.exe",
            "starcraft2_exe_path": "C:\\Program Files (x86)\\StarCraft II\\Versions\\Base97425\\SC2_x64.exe",
            "starcraft2_support64_path": "C:\\Program Files (x86)\\StarCraft II\\Support64",
            "starcraft2_base_path": "C:\\Program Files (x86)\\StarCraft II\\Versions\\Base97425",
            "maps_path": "C:\\Program Files (x86)\\StarCraft II\\Maps",
        }


def json_bytes(payload):
    import json

    return json.dumps(payload, ensure_ascii=False).encode("utf-8")


def json_loads(data):
    import json

    return json.loads(data.decode("utf-8"))


def get_free_tcp_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("127.0.0.1", 0))
        return int(sock.getsockname()[1])


def get_free_udp_port():
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
        sock.bind(("127.0.0.1", 0))
        return int(sock.getsockname()[1])


if __name__ == "__main__":
    unittest.main()
