# import json
# import sys
# import tempfile
# import unittest
# from pathlib import Path
# from unittest import mock


# PROJECT_ROOT = Path(__file__).resolve().parents[1]
# if str(PROJECT_ROOT) not in sys.path:
#     sys.path.insert(0, str(PROJECT_ROOT))


# class StarCraftRemasteredPluginTests(unittest.TestCase):
#     def make_plugin_root(self):
#         temp_dir = tempfile.TemporaryDirectory()
#         root = Path(temp_dir.name) / "StarCraftRemastered"
#         (root / "config").mkdir(parents=True)
#         (root / "prompts").mkdir(parents=True)
#         (root / "prompts" / "starcraft_coach_prompt.txt").write_text(
#             "한국어로 짧게 훈수한다.",
#             encoding="utf-8",
#         )
#         self.addCleanup(temp_dir.cleanup)
#         return root

#     def write_config(self, plugin_root, config):
#         path = plugin_root / "config" / "starcraft_remastered_config.json"
#         path.write_text(
#             json.dumps(config, ensure_ascii=False),
#             encoding="utf-8",
#         )
#         return path

#     def test_modules_json_has_starcraft_toggle(self):
#         modules = json.loads((PROJECT_ROOT / "modules.json").read_text(
#             encoding="utf-8",
#         ))

#         self.assertIn("StarCraftRemastered", modules)
#         self.assertIsInstance(modules.get("StarCraftRemastered"), bool)

#     def test_missing_config_reports_friendly_message(self):
#         from plugins.StarCraftRemastered.starcraft_config import StarCraftConfig

#         plugin_root = self.make_plugin_root()
#         config = StarCraftConfig(str(plugin_root))

#         self.assertIn("StarCraft config missing", config.config_message())
#         validation = config.validate_paths()
#         self.assertFalse(validation.ok)
#         self.assertIn("Copy", validation.message())

#     def test_validate_paths_reports_missing_local_files(self):
#         from plugins.StarCraftRemastered.starcraft_config import StarCraftConfig

#         plugin_root = self.make_plugin_root()
#         self.write_config(
#             plugin_root,
#             {
#                 "enabled": True,
#                 "starcraft_x86_dir": "C:\\missing\\StarCraft\\x86",
#                 "samase_exe_path": "C:\\missing\\StarCraft\\x86\\samase.exe",
#                 "starcraft_exe_path": "C:\\missing\\StarCraft\\x86\\StarCraft.exe",
#                 "custom_scripts_dir": "C:\\missing\\StarCraft\\x86\\custom\\scripts",
#                 "aiscript_bin_path": "C:\\missing\\StarCraft\\x86\\custom\\scripts\\aiscript.bin",
#             },
#         )
#         config = StarCraftConfig(str(plugin_root))

#         validation = config.validate_paths()

#         self.assertFalse(validation.ok)
#         self.assertIn("Samase executable does not exist", validation.message())

#     def test_launcher_uses_shell_false_and_preserves_windows_paths(self):
#         from plugins.StarCraftRemastered.starcraft_config import StarCraftConfig
#         from plugins.StarCraftRemastered.starcraft_launcher import StarCraftLauncher

#         plugin_root = self.make_plugin_root()
#         x86_dir = plugin_root / "Star Craft x86"
#         scripts_dir = x86_dir / "custom" / "scripts"
#         scripts_dir.mkdir(parents=True)
#         samase_path = x86_dir / "samase.exe"
#         starcraft_path = x86_dir / "StarCraft.exe"
#         aiscript_path = scripts_dir / "aiscript.bin"
#         plugin_dll_path = plugin_root / "lav_samase_readonly_plugin.dll"
#         samase_path.write_text("", encoding="utf-8")
#         starcraft_path.write_text("", encoding="utf-8")
#         aiscript_path.write_text("", encoding="utf-8")
#         plugin_dll_path.write_text("", encoding="utf-8")
#         self.write_config(
#             plugin_root,
#             {
#                 "enabled": True,
#                 "profile": "bwmetaai",
#                 "starcraft_x86_dir": str(x86_dir),
#                 "samase_exe_path": str(samase_path),
#                 "starcraft_exe_path": str(starcraft_path),
#                 "mod_argument": "custom",
#                 "custom_scripts_dir": str(scripts_dir),
#                 "aiscript_bin_path": str(aiscript_path),
#                 "samase_readonly_plugin_dll_path": str(plugin_dll_path),
#             },
#         )
#         launcher = StarCraftLauncher(StarCraftConfig(str(plugin_root)))
#         fake_process = mock.Mock()
#         fake_process.pid = 1234

#         with mock.patch(
#             "plugins.StarCraftRemastered.starcraft_launcher.subprocess.Popen",
#             return_value=fake_process,
#         ) as popen_mock:
#             result = launcher.launch()

#         self.assertTrue(result.ok)
#         self.assertEqual(1234, result.process.pid)
#         popen_mock.assert_called_once()
#         args, kwargs = popen_mock.call_args
#         self.assertEqual([str(samase_path), "custom"], args[0])
#         self.assertEqual(str(x86_dir), kwargs["cwd"])
#         self.assertFalse(kwargs["shell"])
#         self.assertIn("LAV_SAMASE_STATE_PATH", kwargs["env"])
#         self.assertIn("LAV_BWAPI_RM_SNAPSHOT_PATH", kwargs["env"])
#         self.assertEqual("8", kwargs["env"]["LAV_SAMASE_STATE_EVERY_N_FRAMES"])
#         self.assertEqual("1000", kwargs["env"]["LAV_SAMASE_HEARTBEAT_INTERVAL_MS"])
#         self.assertEqual(str(plugin_dll_path), kwargs["env"]["SAMASE_MORE_DLLS"])

#     def test_launcher_requires_enabled_true(self):
#         from plugins.StarCraftRemastered.starcraft_config import StarCraftConfig
#         from plugins.StarCraftRemastered.starcraft_launcher import StarCraftLauncher

#         plugin_root = self.make_plugin_root()
#         x86_dir = plugin_root / "Star Craft x86"
#         scripts_dir = x86_dir / "custom" / "scripts"
#         scripts_dir.mkdir(parents=True)
#         samase_path = x86_dir / "samase.exe"
#         starcraft_path = x86_dir / "StarCraft.exe"
#         aiscript_path = scripts_dir / "aiscript.bin"
#         samase_path.write_text("", encoding="utf-8")
#         starcraft_path.write_text("", encoding="utf-8")
#         aiscript_path.write_text("", encoding="utf-8")
#         self.write_config(
#             plugin_root,
#             {
#                 "enabled": False,
#                 "starcraft_x86_dir": str(x86_dir),
#                 "samase_exe_path": str(samase_path),
#                 "starcraft_exe_path": str(starcraft_path),
#                 "custom_scripts_dir": str(scripts_dir),
#                 "aiscript_bin_path": str(aiscript_path),
#             },
#         )
#         launcher = StarCraftLauncher(StarCraftConfig(str(plugin_root)))

#         with mock.patch(
#             "plugins.StarCraftRemastered.starcraft_launcher.subprocess.Popen"
#         ) as popen_mock:
#             result = launcher.launch()

#         self.assertFalse(result.ok)
#         self.assertIn("enabled=false", result.message)
#         popen_mock.assert_not_called()

#     def test_coach_payload_is_sent_as_screen_observation(self):
#         from plugins.StarCraftRemastered.starcraft_remastered import (
#             StarCraftRemastered,
#         )

#         plugin_root = self.make_plugin_root()
#         plugin = StarCraftRemastered()
#         plugin.plugin_root = str(plugin_root)
#         plugin.prompt_path = str(
#             plugin_root / "prompts" / "starcraft_coach_prompt.txt"
#         )
#         plugin.config_manager.config["write_state_log"] = False
#         sent_payloads = []
#         plugin.add_output_event_listener(sent_payloads.append)
#         plugin.receive_observation("미니맵에 적 병력이 보인다.")

#         plugin.on_send_coach_click()

#         self.assertEqual(1, len(sent_payloads))
#         payload = sent_payloads[0]
#         self.assertEqual("screen_observation", payload["kind"])
#         self.assertEqual("StarCraftRemastered", payload["source"])
#         self.assertFalse(payload["remember_history"])
#         self.assertIn("미니맵에 적 병력이 보인다.", payload["text"])

#     def test_screen_input_provider_blocks_battlenet_commands(self):
#         from plugins.StarCraftRemastered.core.command import (
#             CommandType,
#             StarCraftCommand,
#         )
#         from plugins.StarCraftRemastered.lav_bridge.starcraft_log_router import (
#             StarCraftLogRouter,
#         )
#         from plugins.StarCraftRemastered.providers.screen_input_provider import (
#             ScreenInputProvider,
#         )

#         router = StarCraftLogRouter()
#         provider = ScreenInputProvider(
#             config={
#                 "mode": "single_player_only",
#                 "allow_battlenet": False,
#                 "allow_multiplayer": False,
#                 "auto_control": False,
#             },
#             log_router=router,
#         )
#         provider.update_screen_observation("Battle.net ladder screen")
#         command = StarCraftCommand(
#             command_type=CommandType.MOVE,
#             unit_ids=[1],
#             target_position=(10, 20),
#         )

#         self.assertFalse(provider.safety_check(provider.get_game_state()))
#         self.assertFalse(provider.send_command(command))
#         self.assertIn("Battle.net", provider.get_game_state().safety_reason)

#     def test_bwapi_compat_provider_maps_saida_style_commands(self):
#         from plugins.StarCraftRemastered.core.command import CommandType
#         from plugins.StarCraftRemastered.lav_bridge.starcraft_log_router import (
#             StarCraftLogRouter,
#         )
#         from plugins.StarCraftRemastered.providers.bwapi_compat_provider import (
#             BWAPICompatProvider,
#         )
#         from plugins.StarCraftRemastered.providers.screen_input_provider import (
#             ScreenInputProvider,
#         )

#         router = StarCraftLogRouter()
#         provider = ScreenInputProvider(
#             config={
#                 "mode": "single_player_only",
#                 "allow_battlenet": False,
#                 "allow_multiplayer": False,
#                 "auto_control": False,
#             },
#             log_router=router,
#         )
#         compat = BWAPICompatProvider(provider)

#         self.assertFalse(compat.move([1, 2], 64, 128))
#         self.assertFalse(compat.train("Terran Marine"))

#         commands = [
#             entry["command"]
#             for entry in router.get_recent_logs(limit=10)
#             if entry["kind"] == "command"
#         ]
#         self.assertEqual(CommandType.MOVE.value, commands[0]["command_type"])
#         self.assertEqual([1, 2], commands[0]["unit_ids"])
#         self.assertEqual((64, 128), commands[0]["target_position"])
#         self.assertEqual(CommandType.TRAIN.value, commands[1]["command_type"])
#         self.assertEqual("Terran Marine", commands[1]["unit_name"])

#     def test_plugin_exposes_bwapi_status_without_side_effect_control(self):
#         from plugins.StarCraftRemastered.starcraft_remastered import (
#             StarCraftRemastered,
#         )

#         plugin = StarCraftRemastered()
#         plugin.config_manager.config["write_state_log"] = False

#         status = plugin.get_status()

#         self.assertIn("game_state", status)
#         self.assertTrue(status["bwapi_compat_enabled"])
#         self.assertTrue(status["bwapi_shim_enabled"])
#         self.assertTrue(status["bwapi_shim"]["ready"])
#         self.assertTrue(status["bwapi_shim"]["source_level_compatibility"])
#         self.assertFalse(status["bwapi_shim"]["binary_abi_compatibility"])
#         self.assertFalse(status["bwapi_shim"]["native_injection"])
#         self.assertTrue(status["saida_compatibility_mode"])
#         self.assertFalse(status["allow_battlenet"])
#         self.assertFalse(status["allow_multiplayer"])
#         self.assertFalse(status["auto_control"])

#     def test_bwapi_shim_scaffold_pins_saida_source_level_contract(self):
#         plugin_root = PROJECT_ROOT / "plugins" / "StarCraftRemastered"
#         shim_root = PROJECT_ROOT / "plugins" / "StarCraftRemastered" / "bwapi_shim"
#         samase_plugin_cargo = plugin_root / "samase_readonly_plugin" / "Cargo.toml"
#         samase_plugin_source = plugin_root / "samase_readonly_plugin" / "src" / "lib.rs"
#         samase_plugin_readme = plugin_root / "samase_readonly_plugin" / "README.md"
#         bwapi_header = shim_root / "include" / "BWAPI.h"
#         bwapi_client_header = shim_root / "include" / "BWAPI" / "Client.h"
#         bridge_header = shim_root / "include" / "LAVBWAPIRM" / "Bridge.h"
#         runner_header = shim_root / "include" / "LAVBWAPIRM" / "CompatRunner.h"
#         file_bridge_header = shim_root / "include" / "LAVBWAPIRM" / "FileBridge.h"
#         provider_header = shim_root / "include" / "LAVBWAPIRM" / "GameStateProvider.h"
#         mock_provider_header = shim_root / "include" / "LAVBWAPIRM" / "MockGameStateProvider.h"
#         mock_bridge_header = shim_root / "include" / "LAVBWAPIRM" / "MockBridge.h"
#         example_bot = shim_root / "examples" / "minimal_saida_style_bot.cpp"
#         mock_runner = shim_root / "examples" / "mock_runtime_probe.cpp"
#         readonly_runner = shim_root / "examples" / "scr_readonly_runtime.cpp"
#         saida_runner = shim_root / "examples" / "saida_mock_runtime.cpp"
#         matrix = shim_root / "docs" / "saida_compatibility_matrix.md"

#         for path in (
#             bwapi_header,
#             bwapi_client_header,
#             bridge_header,
#             runner_header,
#             file_bridge_header,
#             provider_header,
#             mock_provider_header,
#             mock_bridge_header,
#             example_bot,
#             mock_runner,
#             readonly_runner,
#             saida_runner,
#             matrix,
#             samase_plugin_cargo,
#             samase_plugin_source,
#             samase_plugin_readme,
#         ):
#             self.assertTrue(path.is_file(), str(path))

#         bwapi_text = bwapi_header.read_text(encoding="utf-8")
#         bwapi_client_text = bwapi_client_header.read_text(encoding="utf-8")
#         bridge_text = bridge_header.read_text(encoding="utf-8")
#         runner_text = runner_header.read_text(encoding="utf-8")
#         file_bridge_text = file_bridge_header.read_text(encoding="utf-8")
#         provider_text = provider_header.read_text(encoding="utf-8")
#         mock_bridge_text = mock_bridge_header.read_text(encoding="utf-8")
#         example_text = example_bot.read_text(encoding="utf-8")
#         mock_runner_text = mock_runner.read_text(encoding="utf-8")
#         readonly_runner_text = readonly_runner.read_text(encoding="utf-8")
#         saida_runner_text = saida_runner.read_text(encoding="utf-8")
#         matrix_text = matrix.read_text(encoding="utf-8")
#         samase_plugin_text = samase_plugin_source.read_text(encoding="utf-8")

#         self.assertIn("extern GameHandle Broodwar", bwapi_text)
#         self.assertIn("class AIModule", bwapi_text)
#         self.assertIn("using Unit = UnitInterface*", bwapi_text)
#         self.assertIn("Player self()", bwapi_text)
#         self.assertIn("Unitset getMinerals()", bwapi_text)
#         self.assertIn("inline Client BWAPIClient", bwapi_client_text)
#         self.assertIn("class Bridge", bridge_text)
#         self.assertIn("virtual GameSnapshot snapshot()", bridge_text)
#         self.assertIn("class CompatRunner", runner_text)
#         self.assertIn("class FileBridge final", file_bridge_text)
#         self.assertIn("commandQueuePath", file_bridge_text)
#         self.assertIn("class GameStateProvider", provider_text)
#         self.assertIn("class MockBridge final", mock_bridge_text)
#         self.assertIn("newAIModule", example_text)
#         self.assertIn("MockSaidaOpeningBot", mock_runner_text)
#         self.assertIn("runner.run(bot, 5)", mock_runner_text)
#         self.assertIn("scr_readonly_runtime", (shim_root / "CMakeLists.txt").read_text(encoding="utf-8"))
#         self.assertIn("commands=disabled", readonly_runner_text)
#         self.assertIn("MyBot::MyBotModule bot", saida_runner_text)
#         self.assertIn("saida_mock_runtime", (shim_root / "CMakeLists.txt").read_text(encoding="utf-8"))
#         self.assertIn("BWAPI binary ABI | Not implemented", matrix_text)
#         self.assertIn("Battle.net / multiplayer automation | Out of scope", matrix_text)
#         self.assertIn("samase_plugin_init", samase_plugin_text)
#         self.assertIn("fn Initialize", samase_plugin_text)
#         self.assertIn("lav_samase_readonly_write_test_state", samase_plugin_text)
#         self.assertIn("samase_more_dll_thread", samase_plugin_text)
#         self.assertIn("samase_plugin_api_init", samase_plugin_text)
#         self.assertIn("samase_plugin_api", samase_plugin_text)
#         self.assertIn("LAV_SAMASE_STATE_PATH", samase_plugin_text)
#         self.assertIn("hook_game_loop_start", samase_plugin_text)
#         self.assertIn("lav_initialize_bridge_v3", samase_plugin_text)
#         self.assertIn("lav_initialize_bridge_v2", samase_plugin_text)
#         self.assertIn("lav_initialize_bridge_v1", samase_plugin_text)
#         self.assertIn("SAMASE_MORE_DLLS.Initialize", samase_plugin_text)
#         self.assertIn("CreateToolhelp32Snapshot", samase_plugin_text)
#         self.assertIn("Module32FirstW", samase_plugin_text)
#         self.assertIn("scr_version_snapshot", samase_plugin_text)
#         self.assertIn("compat_schema", samase_plugin_text)
#         self.assertIn("lav_in_game_detector_v1", samase_plugin_text)
#         self.assertIn("in_game_detector", samase_plugin_text)
#         self.assertIn("VirtualQuery", samase_plugin_text)
#         self.assertIn("LAV_SAMASE_INGAME_DETECTOR", samase_plugin_text)
#         self.assertIn("samase_1161_shim_offsets", samase_plugin_text)
#         self.assertIn("direct_primitive_reads", samase_plugin_text)
#         self.assertIn("pointer_dereferences", samase_plugin_text)
#         self.assertIn("candidate address is in an executable section", samase_plugin_text)
#         self.assertIn("lav_scr_offset_discovery_v1", samase_plugin_text)
#         self.assertIn("lav_scr_offset_discovery_v2", samase_plugin_text)
#         self.assertIn("lav_scr_offset_window_profiles_v1", samase_plugin_text)
#         self.assertIn("lav_scr_focused_window_drilldown_v1", samase_plugin_text)
#         self.assertIn("lav_scr_focused_window_drilldown_v2", samase_plugin_text)
#         self.assertIn("offset_discovery", samase_plugin_text)
#         self.assertIn("LAV_SAMASE_OFFSET_DISCOVERY", samase_plugin_text)
#         self.assertIn("focused_u32_watch", samase_plugin_text)
#         self.assertIn("lav_scr_focused_u32_watch_v1", samase_plugin_text)
#         self.assertIn("LAV_SAMASE_U32_WATCHLIST_PATH", samase_plugin_text)
#         self.assertIn("LAV_SAMASE_U32_WATCH", samase_plugin_text)
#         self.assertIn("direct_u32_le", samase_plugin_text)
#         self.assertIn("direct_read_only_no_pointer_deref", samase_plugin_text)
#         self.assertIn("load_u32_watch_candidates", samase_plugin_text)
#         self.assertIn("lav_in_game_detector_v2", samase_plugin_text)
#         self.assertIn("focused_u32_watch_signal_for_module", samase_plugin_text)
#         self.assertIn("LAV_SAMASE_U32_WATCH_ACTIVE_THRESHOLD", samase_plugin_text)
#         self.assertIn("LAV_SAMASE_RESOURCE_FOCUSED_SCAN", samase_plugin_text)
#         self.assertIn("LAV_SAMASE_RESOURCE_FOCUSED_START_WINDOW", samase_plugin_text)
#         self.assertIn("LAV_SAMASE_RESOURCE_FOCUSED_WINDOWS", samase_plugin_text)
#         self.assertIn("MAX_RESOURCE_FOCUSED_WINDOWS_PER_MODULE", samase_plugin_text)
#         self.assertIn("resolve_resource_focused_start_window", samase_plugin_text)
#         self.assertIn("nonzero_count", samase_plugin_text)
#         self.assertIn("bounded_readonly_scan", samase_plugin_text)
#         self.assertIn("window_profiles", samase_plugin_text)
#         self.assertIn("focused_window_drilldown", samase_plugin_text)
#         self.assertIn("resource_sweep", samase_plugin_text)
#         self.assertIn("focused_seed_window_rvas_for_role", samase_plugin_text)
#         self.assertIn("push_resource_focused_windows", samase_plugin_text)
#         self.assertIn("DISCOVERY_DRILLDOWN_CHUNK_BYTES", samase_plugin_text)
#         self.assertIn("bytes_hex", samase_plugin_text)
#         focused_chunk_source = samase_plugin_text[
#             samase_plugin_text.index("fn discovery_focused_chunk_json") :
#             samase_plugin_text.index("fn section_containing_rva")
#         ]
#         self.assertIn("chunk_rva_start", focused_chunk_source)
#         self.assertIn("bytes_hex", focused_chunk_source)
#         self.assertIn("hex_bytes(bytes)", focused_chunk_source)
#         self.assertIn("MAX_DISCOVERY_WINDOW_PROFILES_PER_MODULE", samase_plugin_text)
#         self.assertIn("small_u32_nonzero", samase_plugin_text)
#         self.assertIn("bool_u8_true", samase_plugin_text)
#         self.assertIn("readable_pointer_u32", samase_plugin_text)
#         self.assertIn("zero_suppressed", samase_plugin_text)
#         self.assertIn("MAX_DISCOVERY_RANGE_BYTES", samase_plugin_text)
#         self.assertIn("fingerprints", samase_plugin_text)
#         self.assertIn("samase_temp_module", samase_plugin_text)
#         self.assertIn("section_scan_preparation", samase_plugin_text)
#         self.assertIn("bounded_candidates_only", samase_plugin_text)
#         self.assertIn("scan_policy", samase_plugin_text)
#         self.assertIn("verified_scan_ranges", samase_plugin_text)
#         self.assertIn("within_section_bounds", samase_plugin_text)
#         self.assertIn("read_now", samase_plugin_text)
#         self.assertIn("validation_stages", samase_plugin_text)
#         self.assertIn("fnv1a64", samase_plugin_text)
#         self.assertIn("pe_time_date_stamp", samase_plugin_text)
#         self.assertIn("dereference_now", samase_plugin_text)
#         self.assertIn("read_only_memory_probe", samase_plugin_text)
#         self.assertIn("direct_memory_reads\\\": false", samase_plugin_text)

#     def test_samase_probe_summarizes_and_compares_offset_discovery(self):
#         from plugins.StarCraftRemastered.tools.samase_plugin_loader_probe import (
#             compare_offset_discovery_payloads,
#             compare_focused_chunks,
#             compare_offset_window_profiles,
#             build_resource_candidate_payload_from_snapshots,
#             focused_byte_probe_status,
#             focused_chunk_byte_diffs,
#             focused_chunks_have_bytes_hex,
#             focused_chunk_u32_diffs,
#             focused_u32_candidate_rows,
#             focused_u32_stability_report,
#             focused_u32_watchlist_payload,
#             focused_u32_watch_signal,
#             format_focused_u32_candidate,
#             format_focused_u32_stability_report,
#             format_focused_u32_watch_signal,
#             format_focused_u32_watch_transition,
#             format_focused_u32_watchlist_payload,
#             format_focused_byte_probe_status,
#             format_resource_candidate_payload,
#             format_state_metadata,
#             offset_comparison_warnings,
#             state_metadata,
#             summarize_focused_u32_watch_payload,
#             summarize_offset_discovery_payload,
#             window_change_score,
#         )

#         before = {
#             "written_at": 100,
#             "loader": "samase_more_dll_thread",
#             "game": {
#                 "in_game": False,
#                 "frame_count": 10,
#                 "map_name": "",
#             },
#             "units": {
#                 "my": [],
#                 "enemy": [],
#                 "neutral": [],
#             },
#             "bridge": {
#                 "loader": "samase_more_dll_thread",
#                 "process": {
#                     "pid": 1000,
#                 },
#                 "scr_version_snapshot": {
#                     "starcraft_module": {
#                         "base": "0x00400000",
#                     },
#                     "clientsdk_module": {
#                         "base": "0x588B0000",
#                     },
#                     "samase_temp_module": {
#                         "name": "samase_0831_probe.dll",
#                         "base": "0x5C580000",
#                     },
#                 },
#                 "offset_discovery": {
#                     "schema": "lav_scr_offset_discovery_v1",
#                     "mode": "bounded_readonly_scan",
#                     "enabled": True,
#                     "module_groups": [
#                         {
#                             "role": "starcraft_module",
#                             "status": "scanned",
#                             "ranges_scanned": 1,
#                             "bytes_scanned": 4096,
#                             "candidates": {
#                                 "small_u32_nonzero": [
#                                     {
#                                         "module_name": "starcraft.exe",
#                                         "section_name": ".data",
#                                         "rva": "0x00B01004",
#                                         "value": 4,
#                                         "hex_value": "0x00000004",
#                                     }
#                                 ],
#                                 "bool_u8_true": [],
#                                 "readable_pointer_u32": [],
#                             },
#                         }
#                     ],
#                 },
#                 "focused_u32_watch": {
#                     "schema": "lav_scr_focused_u32_watch_v1",
#                     "enabled": True,
#                     "status": "read",
#                     "source_path": "focused_u32_watchlist.json",
#                     "loaded_candidates": 1,
#                     "printed_candidates": 1,
#                     "read_count": 1,
#                     "candidates": [
#                         {
#                             "status": "read",
#                             "ok": True,
#                             "rva": "0x00B3CB40",
#                             "hex_value": "0x00000BA0",
#                             "confidence": "stable_scalar_u32",
#                         }
#                     ],
#                 },
#             }
#         }
#         before["bridge"]["offset_discovery"]["module_groups"][0]["window_profiles"] = [
#             {
#                 "module_name": "starcraft.exe",
#                 "section_name": ".data",
#                 "rva_start": "0x00B01000",
#                 "fnv1a64": "0x1111111111111111",
#                 "nonzero_bytes": 10,
#                 "small_u32_nonzero_count": 1,
#                 "bool_u8_true_count": 0,
#                 "readable_pointer_u32_count": 0,
#             },
#             {
#                 "module_name": "starcraft.exe",
#                 "section_name": ".data",
#                 "rva_start": "0x00B02000",
#                 "fnv1a64": "0x3333333333333333",
#                 "nonzero_bytes": 20,
#                 "small_u32_nonzero_count": 0,
#                 "bool_u8_true_count": 0,
#                 "readable_pointer_u32_count": 2,
#             }
#         ]
#         before["bridge"]["offset_discovery"]["module_groups"][0][
#             "focused_window_drilldown"
#         ] = [
#             {
#                 "module_name": "starcraft.exe",
#                 "section_name": ".data",
#                 "rva_start": "0x00B2A000",
#                 "chunks": [
#                     {
#                         "module_name": "starcraft.exe",
#                         "section_name": ".data",
#                         "window_rva_start": "0x00B2A000",
#                         "chunk_rva_start": "0x00B2A100",
#                         "fnv1a64": "0x4444444444444444",
#                         "nonzero_bytes": 30,
#                         "small_u32_nonzero_count": 2,
#                         "bool_u8_true_count": 1,
#                         "readable_pointer_u32_count": 4,
#                         "bytes_hex": "0102030400000000",
#                     }
#                 ],
#             }
#         ]
#         after = json.loads(json.dumps(before))
#         after["bridge"]["process"]["pid"] = 2000
#         after["bridge"]["offset_discovery"]["module_groups"][0]["window_profiles"][0][
#             "fnv1a64"
#         ] = "0x2222222222222222"
#         after["bridge"]["offset_discovery"]["module_groups"][0]["window_profiles"][0][
#             "nonzero_bytes"
#         ] = 12
#         after["bridge"]["offset_discovery"]["module_groups"][0]["window_profiles"][1][
#             "readable_pointer_u32_count"
#         ] = 3
#         after["bridge"]["offset_discovery"]["module_groups"][0][
#             "focused_window_drilldown"
#         ][0]["chunks"][0]["fnv1a64"] = "0x5555555555555555"
#         after["bridge"]["offset_discovery"]["module_groups"][0][
#             "focused_window_drilldown"
#         ][0]["chunks"][0]["nonzero_bytes"] = 33
#         after["bridge"]["offset_discovery"]["module_groups"][0][
#             "focused_window_drilldown"
#         ][0]["chunks"][0]["bytes_hex"] = "0102030505000000"
#         after["bridge"]["offset_discovery"]["module_groups"][0]["candidates"][
#             "small_u32_nonzero"
#         ][0]["value"] = 5
#         after["bridge"]["offset_discovery"]["module_groups"][0]["candidates"][
#             "small_u32_nonzero"
#         ][0]["hex_value"] = "0x00000005"
#         after["bridge"]["offset_discovery"]["module_groups"][0]["candidates"][
#             "bool_u8_true"
#         ].append(
#             {
#                 "module_name": "starcraft.exe",
#                 "section_name": ".data",
#                 "rva": "0x00B01008",
#                 "value": 1,
#                 "hex_value": "0x00000001",
#             }
#         )

#         summary = summarize_offset_discovery_payload(after)
#         watch_summary = summarize_focused_u32_watch_payload(after)
#         diff = compare_offset_discovery_payloads(before, after)
#         window_diff = compare_offset_window_profiles(before, after)
#         focused_diff = compare_focused_chunks(before, after)
#         meta = state_metadata(after)
#         meta_line = format_state_metadata("after", after)
#         warnings = offset_comparison_warnings(before, after)

#         self.assertIn("schema=lav_scr_offset_discovery_v1", summary[0])
#         self.assertIn("schema=lav_scr_focused_u32_watch_v1", watch_summary[0])
#         self.assertIn("read_count=1", watch_summary[0])
#         self.assertTrue(any("rva=0x00B3CB40" in line for line in watch_summary))
#         menu_watch = json.loads(json.dumps(after))
#         menu_watch["bridge"]["focused_u32_watch"]["candidates"] = [
#             {
#                 "status": "read",
#                 "ok": True,
#                 "rva": "0x00B3CB40",
#                 "value": 0,
#                 "hex_value": "0x00000000",
#             },
#             {
#                 "status": "read",
#                 "ok": True,
#                 "rva": "0x00B30930",
#                 "value": 0,
#                 "hex_value": "0x00000000",
#             },
#             {
#                 "status": "read",
#                 "ok": True,
#                 "rva": "0x00B30960",
#                 "value": 0,
#                 "hex_value": "0x00000000",
#             },
#             {
#                 "status": "read",
#                 "ok": True,
#                 "rva": "0x00B30970",
#                 "value": 0,
#                 "hex_value": "0x00000000",
#             },
#             {
#                 "status": "read",
#                 "ok": True,
#                 "rva": "0x00B3C9D8",
#                 "value": 0x750,
#                 "hex_value": "0x00000750",
#             },
#         ]
#         active_watch = json.loads(json.dumps(menu_watch))
#         active_watch["bridge"]["focused_u32_watch"]["candidates"][1]["value"] = 0x22A
#         active_watch["bridge"]["focused_u32_watch"]["candidates"][1]["hex_value"] = "0x0000022A"
#         active_watch["bridge"]["focused_u32_watch"]["candidates"][2]["value"] = 0x22C
#         active_watch["bridge"]["focused_u32_watch"]["candidates"][2]["hex_value"] = "0x0000022C"
#         active_watch["bridge"]["focused_u32_watch"]["candidates"][3]["value"] = 0x22A
#         active_watch["bridge"]["focused_u32_watch"]["candidates"][3]["hex_value"] = "0x0000022A"
#         active_watch["bridge"]["focused_u32_watch"]["candidates"][4]["value"] = 0x3661
#         active_watch["bridge"]["focused_u32_watch"]["candidates"][4]["hex_value"] = "0x00003661"
#         menu_signal = focused_u32_watch_signal(menu_watch)
#         active_signal = focused_u32_watch_signal(active_watch)
#         transition_line = format_focused_u32_watch_transition(
#             menu_signal,
#             active_signal,
#             "menu.json",
#             "ingame.json",
#         )
#         self.assertEqual("menu_like", menu_signal["status"])
#         self.assertEqual("active", active_signal["status"])
#         self.assertIn("status=active", format_focused_u32_watch_signal(active_signal))
#         self.assertIn("menu.json->ingame.json", transition_line)
#         self.assertIn("nonzero=1->4", transition_line)
#         self.assertIn("starcraft_module status=scanned", summary[1])
#         self.assertIn("windows=2", summary[1])
#         self.assertIn("small_u32_nonzero=1", summary[1])
#         self.assertIn("bool_u8_true=1", summary[1])
#         self.assertFalse(meta["in_game"])
#         self.assertEqual(2000, meta["process_pid"])
#         self.assertIn("pid=2000", meta_line)
#         self.assertIn("loader=samase_more_dll_thread", meta_line)
#         self.assertIn("in_game=False", meta_line)
#         self.assertTrue(any("in_game=false" in warning for warning in warnings))
#         self.assertTrue(any("different process ids" in warning for warning in warnings))
#         self.assertEqual(1, len(diff["changed"]))
#         self.assertEqual(1, len(diff["added"]))
#         self.assertEqual(0, len(diff["removed"]))
#         self.assertEqual(1, len(window_diff["changed"]))
#         self.assertEqual(3, window_change_score(*window_diff["changed"][0][1:]))
#         self.assertEqual(1, len(window_diff["pointer_plausibility_changed"]))
#         self.assertEqual(0, len(window_diff["added"]))
#         self.assertEqual(0, len(window_diff["removed"]))
#         self.assertEqual(1, len(focused_diff["changed"]))
#         self.assertEqual(4, window_change_score(*focused_diff["changed"][0][1:]))
#         self.assertEqual(
#             2,
#             focused_chunk_byte_diffs(*focused_diff["changed"][0][1:])["changed_count"],
#         )
#         self.assertTrue(focused_chunks_have_bytes_hex(after))
#         self.assertEqual(
#             2,
#             focused_chunk_u32_diffs(*focused_diff["changed"][0][1:])["changed_count"],
#         )
#         self.assertEqual(0, len(focused_diff["added"]))
#         self.assertEqual(0, len(focused_diff["removed"]))
#         ready_status = focused_byte_probe_status(before, after)
#         ready_lines = format_focused_byte_probe_status(ready_status)
#         self.assertEqual("ready_for_u32_samples", ready_status["status"])
#         self.assertEqual(1, ready_status["changed_with_bytes"])
#         self.assertEqual(2, ready_status["u32_changed"])
#         self.assertIn("status=ready_for_u32_samples", ready_lines[0])
#         self.assertTrue(any("focused u32 candidate" in line for line in ready_lines))
#         candidate_rows = focused_u32_candidate_rows(before, after)
#         candidate_line = format_focused_u32_candidate(candidate_rows[0], 1)
#         self.assertEqual(2, len(candidate_rows))
#         self.assertEqual(0x00B2A104, candidate_rows[0]["rva"])
#         self.assertEqual(5, candidate_rows[0]["after"])
#         self.assertEqual("small_u32", candidate_rows[0]["value_kind"])
#         self.assertIn("rva=0x00B2A104", candidate_line)
#         self.assertIn("after_dec=5", candidate_line)

#         stable_before = json.loads(json.dumps(before))
#         stable_mid = json.loads(json.dumps(after))
#         stable_later = json.loads(json.dumps(after))
#         for payload in (stable_before, stable_mid, stable_later):
#             payload["bridge"]["process"]["pid"] = 3000
#         stable_later["bridge"]["offset_discovery"]["module_groups"][0][
#             "focused_window_drilldown"
#         ][0]["chunks"][0]["fnv1a64"] = "0x6666666666666666"
#         stable_later["bridge"]["offset_discovery"]["module_groups"][0][
#             "focused_window_drilldown"
#         ][0]["chunks"][0]["nonzero_bytes"] = 34
#         stable_later["bridge"]["offset_discovery"]["module_groups"][0][
#             "focused_window_drilldown"
#         ][0]["chunks"][0]["bytes_hex"] = "0102030609000000"
#         stability = focused_u32_stability_report(
#             [stable_before, stable_mid, stable_later]
#         )
#         stability_lines = format_focused_u32_stability_report(
#             stability,
#             ["before.json", "mid.json", "later.json"],
#             5,
#         )
#         stable_rvas = {row["rva"] for row in stability["candidates"]}
#         self.assertEqual("ready", stability["status"])
#         self.assertTrue(stability["same_pid"])
#         self.assertIn(0x00B2A104, stable_rvas)
#         self.assertIn("status=ready", stability_lines[0])
#         self.assertTrue(any("before.json->mid.json" in line for line in stability_lines))
#         watchlist = focused_u32_watchlist_payload(
#             [stable_before, stable_mid, stable_later],
#             ["before.json", "mid.json", "later.json"],
#             4,
#         )
#         watchlist_lines = format_focused_u32_watchlist_payload(
#             watchlist,
#             "watchlist.json",
#         )
#         self.assertEqual("ready", watchlist["status"])
#         self.assertTrue(watchlist["same_pid"])
#         self.assertEqual(1, len(watchlist["candidates"]))
#         self.assertEqual("0x00B2A104", watchlist["candidates"][0]["rva"])
#         self.assertEqual("stable_small_u32", watchlist["candidates"][0]["confidence"])
#         self.assertIn("output=watchlist.json", watchlist_lines[0])

#         def resource_bytes(minerals, gas, supply_used, supply_total):
#             values = [0, minerals, supply_used, supply_total, gas]
#             return b"".join(
#                 int(value).to_bytes(4, "little")
#                 for value in values
#             ).hex()

#         resource_start = json.loads(json.dumps(stable_before))
#         resource_mid = json.loads(json.dumps(stable_before))
#         resource_later = json.loads(json.dumps(stable_before))
#         for index, payload in enumerate(
#             (resource_start, resource_mid, resource_later),
#             start=1,
#         ):
#             payload["bridge"]["process"]["pid"] = 4000
#             payload["game"]["in_game"] = True
#             payload["game"]["frame_count"] = index * 10
#         resource_start["bridge"]["offset_discovery"]["module_groups"][0][
#             "focused_window_drilldown"
#         ][0]["chunks"][0]["bytes_hex"] = resource_bytes(50, 0, 8, 20)
#         resource_mid["bridge"]["offset_discovery"]["module_groups"][0][
#             "focused_window_drilldown"
#         ][0]["chunks"][0]["bytes_hex"] = resource_bytes(60, 0, 8, 20)
#         resource_later["bridge"]["offset_discovery"]["module_groups"][0][
#             "focused_window_drilldown"
#         ][0]["chunks"][0]["bytes_hex"] = resource_bytes(40, 0, 10, 22)
#         resource_payload = build_resource_candidate_payload_from_snapshots(
#             [
#                 {
#                     "label": "start",
#                     "path": "start.json",
#                     "payload": resource_start,
#                     "observed": {
#                         "minerals": 50,
#                         "gas": 0,
#                         "supply_used": 8,
#                         "supply_total": 20,
#                     },
#                 },
#                 {
#                     "label": "mid",
#                     "path": "mid.json",
#                     "payload": resource_mid,
#                     "observed": {
#                         "minerals": 60,
#                         "gas": 0,
#                         "supply_used": 8,
#                         "supply_total": 20,
#                     },
#                 },
#                 {
#                     "label": "later",
#                     "path": "later.json",
#                     "payload": resource_later,
#                     "observed": {
#                         "minerals": 40,
#                         "gas": 0,
#                         "supply_used": 10,
#                         "supply_total": 22,
#                     },
#                 },
#             ],
#             5,
#         )
#         resource_lines = format_resource_candidate_payload(
#             resource_payload,
#             "resource_candidates.json",
#         )
#         self.assertEqual("lav_scr_resource_candidates_v1", resource_payload["schema"])
#         self.assertEqual("ready", resource_payload["status"])
#         self.assertTrue(resource_payload["same_pid"])
#         self.assertEqual(3, resource_payload["exact_changed_fields"])
#         self.assertTrue(resource_payload["required_exact_ready"])
#         self.assertIn("minerals", resource_payload["exact_changed_field_names"])
#         self.assertEqual(
#             "exact_changed",
#             resource_payload["fields"]["minerals"]["status"],
#         )
#         self.assertEqual(
#             "0x00B2A104",
#             resource_payload["fields"]["minerals"]["candidates"][0]["rva"],
#         )
#         self.assertEqual(
#             "exact_changed",
#             resource_payload["fields"]["minerals"]["candidates"][0]["confidence"],
#         )
#         self.assertEqual(
#             [50, 60, 40],
#             resource_payload["fields"]["minerals"]["candidates"][0]["actual_values"],
#         )
#         self.assertEqual(
#             "constant_zero_not_ranked",
#             resource_payload["fields"]["gas"]["status"],
#         )
#         self.assertEqual(
#             "exact_changed",
#             resource_payload["fields"]["supply_used"]["status"],
#         )
#         self.assertEqual(
#             "0x00B2A108",
#             resource_payload["fields"]["supply_used"]["candidates"][0]["rva"],
#         )
#         self.assertEqual(
#             "0x00B2A10C",
#             resource_payload["fields"]["supply_total"]["candidates"][0]["rva"],
#         )
#         self.assertIn("schema=lav_scr_resource_candidates_v1", resource_lines[0])
#         self.assertIn("exact_changed_fields=3", resource_lines[0])
#         self.assertIn("required_exact_ready=True", resource_lines[0])
#         self.assertTrue(any("field=minerals" in line for line in resource_lines))

#         support_only_payload = build_resource_candidate_payload_from_snapshots(
#             [
#                 {
#                     "label": "start",
#                     "path": "start.json",
#                     "payload": resource_start,
#                     "observed": {
#                         "minerals": 50,
#                         "supply_used": 8,
#                     },
#                 },
#                 {
#                     "label": "mid",
#                     "path": "mid.json",
#                     "payload": resource_mid,
#                     "observed": {
#                         "minerals": 66,
#                         "supply_used": 8,
#                     },
#                 },
#                 {
#                     "label": "later",
#                     "path": "later.json",
#                     "payload": resource_later,
#                     "observed": {
#                         "minerals": 16,
#                         "supply_used": 10,
#                     },
#                 },
#             ],
#             5,
#         )
#         support_only_lines = format_resource_candidate_payload(support_only_payload)
#         self.assertEqual("support_only_candidates", support_only_payload["status"])
#         self.assertEqual(["supply_used"], support_only_payload["exact_changed_field_names"])
#         self.assertFalse(support_only_payload["required_exact_ready"])
#         self.assertTrue(
#             any("support-field candidates" in line for line in support_only_lines)
#         )

#         partial_payload = build_resource_candidate_payload_from_snapshots(
#             [
#                 {
#                     "label": "start",
#                     "path": "start.json",
#                     "payload": resource_start,
#                     "observed": {
#                         "minerals": 50,
#                     },
#                 },
#                 {
#                     "label": "mid",
#                     "path": "mid.json",
#                     "payload": resource_mid,
#                     "observed": {
#                         "minerals": 66,
#                     },
#                 },
#                 {
#                     "label": "later",
#                     "path": "later.json",
#                     "payload": resource_later,
#                     "observed": {
#                         "minerals": 16,
#                     },
#                 },
#             ],
#             5,
#         )
#         partial_lines = format_resource_candidate_payload(partial_payload)
#         self.assertEqual("partial_candidates", partial_payload["status"])
#         self.assertEqual(0, partial_payload["exact_changed_fields"])
#         self.assertTrue(
#             any("do not promote yet" in line for line in partial_lines)
#         )

#         mismatched = focused_u32_stability_report([before, after, stable_later])
#         mismatch_lines = format_focused_u32_stability_report(
#             mismatched,
#             ["before.json", "after.json", "later.json"],
#             5,
#         )
#         self.assertEqual("pid_mismatch", mismatched["status"])
#         self.assertTrue(any("recapture" in line for line in mismatch_lines))
#         mismatch_watchlist = focused_u32_watchlist_payload(
#             [before, after, stable_later],
#             ["before.json", "after.json", "later.json"],
#             4,
#         )
#         self.assertEqual("pid_mismatch", mismatch_watchlist["status"])
#         self.assertEqual([], mismatch_watchlist["candidates"])

#         stale_before = json.loads(json.dumps(before))
#         stale_after = json.loads(json.dumps(after))
#         stale_before["bridge"]["offset_discovery"]["module_groups"][0][
#             "focused_window_drilldown"
#         ][0]["chunks"][0]["bytes_hex"] = ""
#         stale_after["bridge"]["offset_discovery"]["module_groups"][0][
#             "focused_window_drilldown"
#         ][0]["chunks"][0]["bytes_hex"] = ""
#         stale_status = focused_byte_probe_status(stale_before, stale_after)
#         stale_lines = format_focused_byte_probe_status(stale_status)
#         self.assertEqual("missing_focused_bytes", stale_status["status"])
#         self.assertEqual(0, stale_status["changed_with_bytes"])
#         self.assertTrue(any("run_control_more_dll.cmd" in line for line in stale_lines))

#     def test_screen_observation_parser_builds_bwapi_ready_game_state(self):
#         from plugins.StarCraftRemastered.core.observation_parser import (
#             StarCraftObservationParser,
#         )

#         parser = StarCraftObservationParser()
#         state = parser.parse(
#             "Brood War in-game Zerg base. minerals 50 gas 0 supply 4/9. "
#             "A Hatchery and 3 Drone units are visible."
#         )

#         self.assertTrue(state.is_connected)
#         self.assertTrue(state.is_in_game)
#         self.assertTrue(state.is_single_player)
#         self.assertFalse(state.is_battlenet_screen)
#         self.assertEqual("Zerg", state.player_race)
#         self.assertEqual(50, state.minerals)
#         self.assertEqual(0, state.gas)
#         self.assertEqual(4, state.supply_used)
#         self.assertEqual(9, state.supply_total)
#         self.assertIn("Zerg Hatchery", [unit.unit_type for unit in state.my_units])
#         self.assertEqual(
#             3,
#             len([unit for unit in state.my_units if unit.unit_type == "Zerg Drone"]),
#         )

#     def test_screen_observation_parser_rejects_codex_only_screen_as_in_game(self):
#         from plugins.StarCraftRemastered.core.observation_parser import (
#             StarCraftObservationParser,
#         )

#         parser = StarCraftObservationParser()
#         state = parser.parse(
#             "Visual Studio Code is open with main.py and a Chrome browser showing Codex."
#         )

#         self.assertTrue(state.is_connected)
#         self.assertFalse(state.is_in_game)
#         self.assertFalse(state.is_battlenet_screen)
#         self.assertFalse(state.is_multiplayer_screen)

#     def test_bwapi_runtime_bridge_writes_snapshot_and_reads_commands(self):
#         from plugins.StarCraftRemastered.core.observation_parser import (
#             StarCraftObservationParser,
#         )
#         from plugins.StarCraftRemastered.lav_bridge.bwapi_runtime_bridge import (
#             BWAPIRuntimeBridge,
#         )

#         temp_dir = tempfile.TemporaryDirectory()
#         self.addCleanup(temp_dir.cleanup)
#         snapshot_path = Path(temp_dir.name) / "snapshot.json"
#         command_path = Path(temp_dir.name) / "commands.jsonl"
#         bridge = BWAPIRuntimeBridge(str(snapshot_path), str(command_path))
#         state = StarCraftObservationParser().parse(
#             "Brood War Zerg in game. minerals 50 gas 0 supply 4/9 Hatchery Drone."
#         )

#         payload = bridge.write_snapshot(state)

#         self.assertTrue(snapshot_path.is_file())
#         self.assertEqual("lav_bwapi_rm_snapshot_v1", payload["schema"])
#         self.assertTrue(payload["game"]["in_game"])
#         self.assertEqual("Zerg", payload["game"]["self"]["race"])
#         self.assertEqual(50, payload["game"]["self"]["minerals"])

#         command_path.write_text(
#             json.dumps(
#                 {
#                     "schema": "lav_bwapi_rm_command_v1",
#                     "type": "MOVE",
#                     "unit_ids": [1, 2],
#                     "target_position": [64, 128],
#                 }
#             )
#             + "\n",
#             encoding="utf-8",
#         )

#         commands = bridge.read_pending_commands()

#         self.assertEqual(1, len(commands))
#         self.assertEqual("MOVE", commands[0].command_type.value)
#         self.assertEqual([1, 2], commands[0].unit_ids)
#         self.assertEqual((64, 128), commands[0].target_position)
#         self.assertEqual("", command_path.read_text(encoding="utf-8"))

#     def test_plugin_update_screen_observation_writes_bwapi_snapshot(self):
#         from plugins.StarCraftRemastered.lav_bridge.bwapi_runtime_bridge import (
#             BWAPIRuntimeBridge,
#         )
#         from plugins.StarCraftRemastered.starcraft_remastered import (
#             StarCraftRemastered,
#         )

#         temp_dir = tempfile.TemporaryDirectory()
#         self.addCleanup(temp_dir.cleanup)
#         snapshot_path = Path(temp_dir.name) / "snapshot.json"
#         command_path = Path(temp_dir.name) / "commands.jsonl"
#         plugin = StarCraftRemastered()
#         plugin.config_manager.config["write_state_log"] = False
#         plugin.runtime_bridge = BWAPIRuntimeBridge(
#             str(snapshot_path),
#             str(command_path),
#         )

#         state = plugin.update_screen_observation(
#             "Brood War Zerg in game. minerals 50 gas 0 supply 4/9 Hatchery Drone."
#         )

#         self.assertTrue(state.is_in_game)
#         self.assertTrue(snapshot_path.is_file())
#         payload = json.loads(snapshot_path.read_text(encoding="utf-8"))
#         self.assertEqual("lav_bwapi_rm_snapshot_v1", payload["schema"])
#         self.assertEqual("Zerg", payload["game"]["self"]["race"])
#         self.assertEqual(50, payload["game"]["self"]["minerals"])

#     def test_samase_provider_reads_readonly_state_file_without_control(self):
#         from plugins.StarCraftRemastered.core.command import (
#             CommandType,
#             StarCraftCommand,
#         )
#         from plugins.StarCraftRemastered.providers.samase_provider import (
#             SamaseProvider,
#         )

#         temp_dir = tempfile.TemporaryDirectory()
#         self.addCleanup(temp_dir.cleanup)
#         state_path = Path(temp_dir.name) / "samase_state.json"
#         state_path.write_text(
#             json.dumps(
#                 {
#                     "schema": "lav_bwapi_rm_snapshot_v1",
#                     "game": {
#                         "connected": True,
#                         "in_game": True,
#                         "single_player": True,
#                         "frame_count": 123,
#                         "map_name": "Readonly Bridge",
#                         "map_width": 64,
#                         "map_height": 64,
#                         "self": {
#                             "id": 1,
#                             "name": "SAIDA",
#                             "race": "Terran",
#                             "minerals": 125,
#                             "gas": 32,
#                             "supply_used": 8,
#                             "supply_total": 20,
#                             "start_location": [9, 15],
#                         },
#                         "enemy": {
#                             "id": 2,
#                             "name": "Enemy",
#                             "race": "Zerg",
#                             "start_location": [24, 7],
#                         },
#                     },
#                     "units": {
#                         "my": [
#                             {
#                                 "unit_id": 1,
#                                 "unit_type": "Terran Command Center",
#                                 "owner": "self",
#                                 "x": 288,
#                                 "y": 480,
#                                 "hp": 1500,
#                                 "is_completed": True,
#                             }
#                         ],
#                         "enemy": [],
#                         "neutral": [],
#                     },
#                 }
#             ),
#             encoding="utf-8",
#         )
#         provider = SamaseProvider(
#             config={
#                 "enabled": True,
#                 "auto_launch": False,
#                 "mode": "single_player_only",
#                 "allow_battlenet": False,
#                 "allow_multiplayer": False,
#                 "samase_state_path": str(state_path),
#             },
#         )

#         self.assertTrue(provider.connect())
#         state = provider.get_game_state()

#         self.assertTrue(state.is_connected)
#         self.assertTrue(state.is_in_game)
#         self.assertTrue(state.is_single_player)
#         self.assertEqual(123, state.frame_count)
#         self.assertEqual("Readonly Bridge", state.map_name)
#         self.assertEqual(64, state.map_width)
#         self.assertEqual("Terran", state.player_race)
#         self.assertEqual(125, state.minerals)
#         self.assertEqual((9, 15), state.my_start_location)
#         self.assertEqual(1, len(state.my_units))
#         self.assertEqual("Terran Command Center", state.my_units[0].unit_type)
#         self.assertFalse(
#             provider.send_command(
#                 StarCraftCommand(
#                     command_type=CommandType.MOVE,
#                     unit_ids=[1],
#                     target_position=(64, 128),
#                 )
#             )
#         )

#     def test_samase_readonly_state_writer_mirrors_bwapi_snapshot(self):
#         from plugins.StarCraftRemastered.core.game_state import (
#             StarCraftGameState,
#             StarCraftPlayer,
#         )
#         from plugins.StarCraftRemastered.core.unit import StarCraftUnit
#         from plugins.StarCraftRemastered.lav_bridge.samase_readonly_state_writer import (
#             SamaseReadonlyStateWriter,
#         )

#         temp_dir = tempfile.TemporaryDirectory()
#         self.addCleanup(temp_dir.cleanup)
#         state_path = Path(temp_dir.name) / "samase_state.json"
#         snapshot_path = Path(temp_dir.name) / "bwapi_snapshot.json"
#         state = StarCraftGameState(
#             is_connected=True,
#             is_in_game=True,
#             is_single_player=True,
#             player_race="Terran",
#             minerals=75,
#             gas=12,
#             supply_used=8,
#             supply_total=20,
#             frame_count=7,
#             map_name="Writer Probe",
#             map_width=64,
#             map_height=64,
#             my_start_location=(9, 15),
#         )
#         state.self_player = StarCraftPlayer(
#             player_id=1,
#             name="SAIDA",
#             race="Terran",
#             minerals=75,
#             gas=12,
#             supply_used=8,
#             supply_total=20,
#         )
#         state.my_units = [
#             StarCraftUnit(
#                 unit_id=1,
#                 unit_type="Terran Command Center",
#                 owner="self",
#                 owner_id=1,
#                 x=288,
#                 y=480,
#                 hp=1500,
#                 is_completed=True,
#             )
#         ]
#         writer = SamaseReadonlyStateWriter(
#             str(state_path),
#             bwapi_snapshot_path=str(snapshot_path),
#         )

#         payload = writer.write_state(state, source="unit_test")

#         self.assertTrue(state_path.is_file())
#         self.assertTrue(snapshot_path.is_file())
#         self.assertEqual("lav_samase_readonly_state_v1", payload["schema"])
#         self.assertEqual("unit_test", payload["source"])
#         self.assertEqual("Writer Probe", payload["game"]["map_name"])
#         self.assertEqual(1, len(payload["units"]["my"]))

#         snapshot = json.loads(snapshot_path.read_text(encoding="utf-8"))
#         self.assertEqual("lav_bwapi_rm_snapshot_v1", snapshot["schema"])
#         self.assertTrue(snapshot["game"]["in_game"])
#         self.assertEqual(75, snapshot["game"]["self"]["minerals"])
#         self.assertEqual(1, len(snapshot["units"]["my"]))

#     def test_samase_plugin_loader_probe_prepares_candidate_scripts(self):
#         from plugins.StarCraftRemastered.tools.samase_plugin_loader_probe import (
#             classify_state_payload,
#             prepare_probe_plan,
#         )

#         temp_dir = tempfile.TemporaryDirectory()
#         self.addCleanup(temp_dir.cleanup)
#         root = Path(temp_dir.name)
#         plugin_dll = root / "lav_samase_readonly_plugin.dll"
#         samase_exe = root / "StarCraft x86" / "samase-0.8.31.exe"
#         x86_dir = samase_exe.parent
#         out_dir = root / "probe"
#         x86_dir.mkdir(parents=True)
#         plugin_dll.write_bytes(b"fake dll")
#         samase_exe.write_text("", encoding="utf-8")

#         manifest = prepare_probe_plan(
#             plugin_dll_path=plugin_dll,
#             samase_exe_path=samase_exe,
#             starcraft_x86_dir=x86_dir,
#             out_dir=out_dir,
#             mod_args="custom",
#             resource_focused_start_window=64,
#             resource_focused_windows=32,
#         )

#         manifest_path = out_dir / "probe_manifest.json"
#         self.assertTrue(manifest_path.is_file())
#         candidate_ids = {
#             entry["candidate_id"]
#             for entry in manifest["entries"]
#         }
#         self.assertIn("control_more_dll", candidate_ids)
#         self.assertIn("samase_plugins_original_name", candidate_ids)
#         self.assertIn("special_files_samase_plugins_original_lines", candidate_ids)

#         api_candidate = next(
#             entry
#             for entry in manifest["entries"]
#             if entry["candidate_id"] == "samase_plugins_original_name"
#         )
#         self.assertTrue(Path(api_candidate["copied_dll_path"]).is_file())
#         run_script = Path(api_candidate["run_script"]).read_text(
#             encoding="utf-8",
#         )
#         self.assertIn("SAMASE_MORE_DLLS=", run_script)
#         self.assertIn("LAV_SAMASE_STATE_PATH", run_script)
#         self.assertIn("LAV_SAMASE_RESOURCE_FOCUSED_SCAN=1", run_script)
#         self.assertIn("LAV_SAMASE_RESOURCE_FOCUSED_START_WINDOW=64", run_script)
#         self.assertIn("LAV_SAMASE_RESOURCE_FOCUSED_WINDOWS=32", run_script)
#         self.assertIn("samase_plugin_api_init", manifest["summary"])
#         self.assertIn("samase_plugin_api", manifest["summary"])

#         special_candidate = next(
#             entry
#             for entry in manifest["entries"]
#             if entry["candidate_id"] == "special_files_samase_plugins_original_lines"
#         )
#         special_files_path = (
#             Path(special_candidate["mod_dir"])
#             / "samase"
#             / "special_files"
#         )
#         self.assertTrue(special_files_path.is_file())
#         self.assertIn(
#             "samase/plugins/lav_samase_readonly_plugin.dll",
#             special_files_path.read_text(encoding="utf-8"),
#         )
#         self.assertEqual(["samase/special_files"], special_candidate["text_files"])

#         self.assertEqual(
#             "api_loader_confirmed",
#             classify_state_payload({"loader": "samase_plugin_api"})["status"],
#         )
#         self.assertEqual(
#             "api_init_confirmed",
#             classify_state_payload({"loader": "samase_plugin_api_init"})[
#                 "status"
#             ],
#         )
#         self.assertEqual(
#             "initialize_only",
#             classify_state_payload({"loader": "samase_more_dll_thread"})[
#                 "status"
#             ],
#         )

#     def test_plugin_syncs_samase_state_file_to_bwapi_snapshot(self):
#         from plugins.StarCraftRemastered.core.game_state import (
#             StarCraftGameState,
#             StarCraftPlayer,
#         )
#         from plugins.StarCraftRemastered.lav_bridge.bwapi_runtime_bridge import (
#             BWAPIRuntimeBridge,
#         )
#         from plugins.StarCraftRemastered.lav_bridge.samase_readonly_state_writer import (
#             SamaseReadonlyStateWriter,
#         )
#         from plugins.StarCraftRemastered.providers.samase_provider import (
#             SamaseProvider,
#         )
#         from plugins.StarCraftRemastered.starcraft_remastered import (
#             StarCraftRemastered,
#         )

#         temp_dir = tempfile.TemporaryDirectory()
#         self.addCleanup(temp_dir.cleanup)
#         state_path = Path(temp_dir.name) / "samase_state.json"
#         snapshot_path = Path(temp_dir.name) / "bwapi_snapshot.json"
#         command_path = Path(temp_dir.name) / "commands.jsonl"
#         state = StarCraftGameState(
#             is_connected=True,
#             is_in_game=True,
#             is_single_player=True,
#             player_race="Terran",
#             minerals=99,
#             gas=4,
#             supply_used=8,
#             supply_total=20,
#             frame_count=11,
#             map_name="Relay Probe",
#             map_width=64,
#             map_height=64,
#         )
#         state.self_player = StarCraftPlayer(
#             player_id=1,
#             name="SAIDA",
#             race="Terran",
#             minerals=99,
#             gas=4,
#             supply_used=8,
#             supply_total=20,
#         )
#         SamaseReadonlyStateWriter(str(state_path)).write_state(
#             state,
#             source="unit_test",
#         )

#         plugin = StarCraftRemastered()
#         plugin.config_manager.config.update(
#             {
#                 "provider": "samase",
#                 "samase_state_path": str(state_path),
#                 "samase_state_bridge_enabled": True,
#                 "bwapi_snapshot_path": str(snapshot_path),
#                 "bwapi_command_queue_path": str(command_path),
#                 "write_state_log": False,
#             }
#         )
#         plugin.provider = SamaseProvider(
#             config=plugin.config_manager,
#             launcher=plugin.launcher,
#             log_router=plugin.log_router,
#         )
#         plugin.runtime_bridge = BWAPIRuntimeBridge(
#             str(snapshot_path),
#             str(command_path),
#         )

#         payload = plugin._sync_samase_state_once()

#         self.assertIsNotNone(payload)
#         self.assertTrue(snapshot_path.is_file())
#         snapshot = json.loads(snapshot_path.read_text(encoding="utf-8"))
#         self.assertTrue(snapshot["game"]["in_game"])
#         self.assertEqual("Relay Probe", snapshot["game"]["map_name"])
#         self.assertEqual(99, snapshot["game"]["self"]["minerals"])


# if __name__ == "__main__":
#     unittest.main()
