#20260702_kpopmodder: Added tests for the StarCraft 1.16 BWAPI launcher plugin.
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


class StarCraft116PluginTests(unittest.TestCase):
    def make_plugin_root(self):
        temp_dir = tempfile.TemporaryDirectory()
        root = Path(temp_dir.name) / "StarCraft116"
        (root / "config").mkdir(parents=True)
        self.addCleanup(temp_dir.cleanup)
        return root

    def write_config(self, plugin_root, config):
        path = plugin_root / "config" / "starcraft116_config.json"
        path.write_text(
            json.dumps(config, ensure_ascii=False),
            encoding="utf-8",
        )
        return path

    def test_modules_json_has_starcraft116_toggle(self):
        modules = json.loads((PROJECT_ROOT / "config" / "modules.example.json").read_text(
            encoding="utf-8",
        ))

        self.assertIn("StarCraft116", modules)
        self.assertIs(modules.get("StarCraftRemastered"), False)
        self.assertIsInstance(modules.get("StarCraft116"), bool)

    def test_missing_config_reports_friendly_message(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config

        plugin_root = self.make_plugin_root()
        config = StarCraft116Config(str(plugin_root))

        self.assertIn("StarCraft 1.16 config missing", config.config_message())
        validation = config.validate_paths()
        self.assertFalse(validation.ok)
        self.assertIn("Copy", validation.message())

    def test_validate_paths_reports_missing_selected_launcher(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config

        plugin_root = self.make_plugin_root()
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "saida",
                "profiles": {
                    "saida": {
                        "start_chaoslauncher": True,
                        "chaoslauncher_path": "C:\\missing\\Chaoslauncher.exe",
                    },
                },
            },
        )

        validation = StarCraft116Config(str(plugin_root)).validate_paths()

        self.assertFalse(validation.ok)
        self.assertIn("Chaoslauncher executable does not exist", validation.message())

    def test_discover_install_detects_bwapi_bot_files(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        ai_dir = game_dir / "bwapi-data" / "AI"
        ai_dir.mkdir(parents=True)
        (game_dir / "StarCraft.exe").write_text("", encoding="utf-8")
        (game_dir / "Chaoslauncher.exe").write_text("", encoding="utf-8")
        (ai_dir / "SAIDA.dll").write_text("", encoding="utf-8")
        (ai_dir / "Monster.dll").write_text("", encoding="utf-8")

        discovery = StarCraft116Config(str(plugin_root)).discover_install(str(game_dir))

        self.assertTrue(discovery.ok)
        self.assertEqual(str(game_dir / "StarCraft.exe"), discovery.starcraft_exe_path)
        self.assertEqual(
            str(game_dir / "Chaoslauncher.exe"),
            discovery.chaoslauncher_path,
        )
        self.assertEqual(str(game_dir / "bwapi-data"), discovery.bwapi_data_dir)
        self.assertEqual(2, len(discovery.bot_files))

    def test_write_config_from_install_creates_detected_profiles(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        ai_dir = game_dir / "bwapi-data" / "AI"
        ai_dir.mkdir(parents=True)
        (game_dir / "StarCraft.exe").write_text("", encoding="utf-8")
        (game_dir / "Chaoslauncher.exe").write_text("", encoding="utf-8")
        saida_path = ai_dir / "SAIDA.dll"
        monster_path = ai_dir / "Monster.dll"
        saida_path.write_text("", encoding="utf-8")
        monster_path.write_text("", encoding="utf-8")
        config = StarCraft116Config(str(plugin_root))

        discovery = config.write_config_from_install(str(game_dir))

        self.assertTrue(discovery.ok)
        self.assertTrue(config.get_bool("enabled", False))
        self.assertEqual("saida", config.get_active_profile_name())
        self.assertIn("saida", config.profile_names())
        self.assertIn("monster", config.profile_names())
        saida_profile = config.get_profile("saida")
        self.assertEqual(str(saida_path), saida_profile["bot_binary_path"])
        self.assertEqual(str(game_dir), saida_profile["chaoslauncher_working_dir"])
        self.assertTrue(saida_profile["chaoslauncher_run_as_admin"])
        self.assertTrue((plugin_root / "config" / "starcraft116_config.json").exists())

    def test_write_config_from_install_detects_supported_loose_bot_profiles(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        ai_dir = game_dir / "bwapi-data" / "AI"
        ai_dir.mkdir(parents=True)
        (game_dir / "StarCraft.exe").write_text("", encoding="utf-8")
        (game_dir / "Chaoslauncher.exe").write_text("", encoding="utf-8")
        stardust_path = ai_dir / "Stardust.dll"
        stardust_path.write_text("", encoding="utf-8")
        insanity_path = game_dir / "insanitybot" / "insanitybot.dll"
        halo_path = game_dir / "Hao Pan" / "Halo.dll"
        crona_path = game_dir / "Crona" / "BananaBrain.dll"
        terminus_path = game_dir / "Terminus" / "BananaBrain.dll"
        for path in (insanity_path, halo_path, crona_path, terminus_path):
            path.parent.mkdir(parents=True)
            path.write_text("", encoding="utf-8")
        config = StarCraft116Config(str(plugin_root))

        discovery = config.write_config_from_install(str(game_dir))

        self.assertTrue(discovery.ok)
        self.assertEqual("stardust", config.get_active_profile_name())
        self.assertIn("stardust", config.profile_names())
        self.assertIn("crona", config.profile_names())
        self.assertIn("terminus", config.profile_names())
        self.assertNotIn("insanitybot", config.profile_names())
        self.assertNotIn("hao_pan", config.profile_names())
        self.assertEqual(
            str(crona_path),
            config.get_profile("crona")["bot_binary_path"],
        )
        self.assertEqual(
            str(terminus_path),
            config.get_profile("terminus")["bot_binary_path"],
        )

    def test_profile_dropdown_choices_show_race_label_and_keep_profile_value(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config

        plugin_root = self.make_plugin_root()
        choices = StarCraft116Config(str(plugin_root)).profile_dropdown_choices()

        self.assertIn(("stardust_프로토스", "stardust"), choices)
        self.assertIn(("crona_저그", "crona"), choices)
        self.assertIn(("terminus_테란", "terminus"), choices)
        self.assertIn(("SAIDA", "saida"), choices)
        self.assertNotIn("hao_pan", [value for _label, value in choices])
        self.assertNotIn("insanitybot", [value for _label, value in choices])

    def test_write_config_from_install_supports_starcraft_only_install(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        game_dir.mkdir(parents=True)
        starcraft_path = game_dir / "StarCraft.exe"
        starcraft_path.write_text("", encoding="utf-8")
        config = StarCraft116Config(str(plugin_root))

        discovery = config.write_config_from_install(str(game_dir))

        self.assertFalse(discovery.ok)
        self.assertTrue(config.get_bool("enabled", False))
        self.assertEqual("starcraft", config.get_active_profile_name())
        self.assertEqual(["starcraft"], config.profile_names())
        profile = config.get_profile("starcraft")
        self.assertTrue(profile["start_starcraft"])
        self.assertFalse(profile["start_chaoslauncher"])
        self.assertEqual(str(starcraft_path), profile["starcraft_exe_path"])

    def test_launcher_uses_shell_false_and_profile_environment(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_launcher import StarCraft116Launcher

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        game_dir.mkdir(parents=True)
        launcher_path = game_dir / "Chaoslauncher.exe"
        bot_path = game_dir / "bwapi-data" / "AI" / "SAIDA.dll"
        bot_path.parent.mkdir(parents=True)
        launcher_path.write_text("", encoding="utf-8")
        bot_path.write_text("", encoding="utf-8")
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "saida",
                "profiles": {
                    "saida": {
                        "starcraft_116_dir": str(game_dir),
                        "bot_binary_path": str(bot_path),
                        "start_chaoslauncher": True,
                        "chaoslauncher_path": str(launcher_path),
                        "chaoslauncher_arguments": ["-run"],
                        "chaoslauncher_working_dir": str(game_dir),
                        "environment": {
                            "BWAPI_CONFIG_AI": "SAIDA",
                        },
                    },
                },
            },
        )
        launcher = StarCraft116Launcher(StarCraft116Config(str(plugin_root)))
        fake_process = mock.Mock()
        fake_process.pid = 116

        with mock.patch(
            "plugins.StarCraft116.starcraft116_core.starcraft116_launch_executor.launch_process",
            return_value=fake_process,
        ) as popen_mock:
            result = launcher.launch()

        self.assertTrue(result.ok)
        self.assertEqual(1, len(result.processes))
        popen_mock.assert_called_once()
        args, kwargs = popen_mock.call_args
        self.assertEqual([str(launcher_path), "-run"], args[0])
        self.assertEqual(str(game_dir), kwargs["cwd"])
        self.assertFalse(kwargs["shell"])
        self.assertEqual("saida", kwargs["env"]["LAV_STARCRAFT116_PROFILE"])
        self.assertEqual("SAIDA", kwargs["env"]["BWAPI_CONFIG_AI"])

    def test_launcher_adds_observer_process_only_when_profile_enables_it(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_launcher import StarCraft116Launcher

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        game_dir.mkdir(parents=True)
        launcher_path = game_dir / "Chaoslauncher.exe"
        observer_path = plugin_root / "observer" / "LAVBWAPIObserverClient.exe"
        launcher_path.write_text("", encoding="utf-8")
        observer_path.parent.mkdir(parents=True)
        observer_path.write_text("", encoding="utf-8")
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "monster",
                "profiles": {
                    "monster": {
                        "start_chaoslauncher": True,
                        "chaoslauncher_path": str(launcher_path),
                        "chaoslauncher_working_dir": str(game_dir),
                        "start_observer_process": True,
                        "observer_process_path": str(observer_path),
                        "observer_process_arguments": [
                            "--events-path",
                            str(plugin_root / "events.jsonl"),
                        ],
                        "observer_process_working_dir": str(observer_path.parent),
                    },
                    "stardust": {
                        "start_chaoslauncher": True,
                        "chaoslauncher_path": str(launcher_path),
                        "chaoslauncher_working_dir": str(game_dir),
                    },
                },
            },
        )
        config = StarCraft116Config(str(plugin_root))
        launcher = StarCraft116Launcher(config)

        monster_plan = launcher.build_launch_plan()
        config.set_active_profile("stardust")
        stardust_plan = launcher.build_launch_plan()

        self.assertEqual(["chaoslauncher", "observer"], [
            command.label for command in monster_plan
        ])
        self.assertEqual([
            str(observer_path),
            "--events-path",
            str(plugin_root / "events.jsonl"),
        ], monster_plan[-1].command)
        self.assertEqual(["chaoslauncher"], [
            command.label for command in stardust_plan
        ])

    def test_launcher_splits_string_arguments(self):#20260703_kpopmodder
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_launcher import StarCraft116Launcher

        launcher = StarCraft116Launcher(StarCraft116Config(str(self.make_plugin_root())))

        self.assertEqual(
            ["-window", "-skipupdate"],
            launcher._coerce_arguments("-window -skipupdate"),
        )

    def test_launcher_preserves_quoted_argument_paths(self):#20260703_kpopmodder
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_launcher import StarCraft116Launcher

        launcher = StarCraft116Launcher(StarCraft116Config(str(self.make_plugin_root())))

        self.assertEqual(
            [r"C:\Program Files\StarCraft Tools\helper.exe", "-window"],
            launcher._coerce_arguments(
                r'"C:\Program Files\StarCraft Tools\helper.exe" -window'
            ),
        )

    def test_launcher_can_elevate_chaoslauncher(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_launcher import StarCraft116Launcher

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        game_dir.mkdir(parents=True)
        launcher_path = game_dir / "Chaoslauncher.exe"
        bot_path = game_dir / "bwapi-data" / "AI" / "Stardust.dll"
        bot_path.parent.mkdir(parents=True)
        launcher_path.write_text("", encoding="utf-8")
        bot_path.write_text("", encoding="utf-8")
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "stardust",
                "profiles": {
                    "stardust": {
                        "starcraft_116_dir": str(game_dir),
                        "bot_binary_path": str(bot_path),
                        "start_chaoslauncher": True,
                        "chaoslauncher_path": str(launcher_path),
                        "chaoslauncher_working_dir": str(game_dir),
                        "chaoslauncher_run_as_admin": True,
                    },
                },
            },
        )
        launcher = StarCraft116Launcher(StarCraft116Config(str(plugin_root)))
        fake_process = mock.Mock()
        fake_process.pid = 1161

        with mock.patch(
            "plugins.StarCraft116.starcraft116_core.starcraft116_launch_executor.launch_process"
        ) as popen_mock, mock.patch.object(
            launcher,
            "_launch_elevated",
            return_value=fake_process,
        ) as elevated_mock:
            result = launcher.launch()

        self.assertTrue(result.ok)
        popen_mock.assert_not_called()
        elevated_mock.assert_called_once()
        launch_command = elevated_mock.call_args.args[0]
        self.assertEqual("chaoslauncher", launch_command.label)
        self.assertTrue(launch_command.run_as_admin)

    def test_launcher_requires_enabled_true(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_launcher import StarCraft116Launcher

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        game_dir.mkdir(parents=True)
        launcher_path = game_dir / "Chaoslauncher.exe"
        launcher_path.write_text("", encoding="utf-8")
        self.write_config(
            plugin_root,
            {
                "enabled": False,
                "profiles": {
                    "saida": {
                        "start_chaoslauncher": True,
                        "chaoslauncher_path": str(launcher_path),
                    },
                },
            },
        )
        launcher = StarCraft116Launcher(StarCraft116Config(str(plugin_root)))

        with mock.patch(
            "plugins.StarCraft116.starcraft116_core.starcraft116_launch_executor.launch_process"
        ) as popen_mock:
            result = launcher.launch()

        self.assertFalse(result.ok)
        self.assertIn("enabled=false", result.message)
        popen_mock.assert_not_called()

    def test_status_reader_reads_bwapi_ini_and_chaoslauncher_log(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_status import StarCraft116StatusReader

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        bwapi_dir = game_dir / "bwapi-data"
        ai_dir = bwapi_dir / "AI"
        chaos_dir = plugin_root / "Chaoslauncher"
        ai_dir.mkdir(parents=True)
        chaos_dir.mkdir(parents=True)
        bot_path = ai_dir / "Stardust.dll"
        bot_path.write_text("", encoding="utf-8")
        (bwapi_dir / "bwapi.ini").write_text(
            "\n".join([
                "ai     = bwapi-data/AI/Stardust.dll",
                "ai_dbg = bwapi-data/AI/Stardust.dll",
                "race = Protoss",
            ]),
            encoding="utf-8",
        )
        (chaos_dir / "Chaoslauncher.log").write_text(
            "\n".join([
                "Plugin loaded BWAPI 4.4.0 Injector [RELEASE]",
                "Plugin loaded W-MODE 1.02",
                "Obtained DebugPrivilege",
                "ApplyPatch for BWAPI 4.4.0 Injector [RELEASE]",
                "ApplyPatch for W-MODE 1.02",
                "Starting Starcraft completed",
            ]),
            encoding="utf-8",
        )
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "stardust",
                "profiles": {
                    "stardust": {
                        "starcraft_116_dir": str(game_dir),
                        "bwapi_data_dir": str(bwapi_dir),
                        "bot_binary_path": str(bot_path),
                        "start_chaoslauncher": True,
                        "chaoslauncher_path": str(chaos_dir / "Chaoslauncher.exe"),
                        "chaoslauncher_working_dir": str(chaos_dir),
                    },
                },
            },
        )
        reader = StarCraft116StatusReader(StarCraft116Config(str(plugin_root)))
        with mock.patch.object(
            reader,
            "process_snapshot",
            return_value={
                "supported": True,
                "matches": {
                    "StarCraft.exe": [{"image": "StarCraft.exe", "pid": 1161}],
                    "Chaoslauncher.exe": [],
                },
                "errors": [],
            },
        ):
            snapshot = reader.snapshot()

        self.assertEqual("Stardust.dll", snapshot["bwapi_ini"]["configured_ai_binary"])
        self.assertTrue(snapshot["bwapi_ini"]["expected_bot_matches_ini"])
        self.assertEqual("Protoss", snapshot["bwapi_ini"]["values"]["race"])
        self.assertTrue(
            snapshot["chaoslauncher_log"]["markers"][
                "debug_privilege_obtained"
            ]
        )
        self.assertTrue(snapshot["readiness"]["starcraft_process_running"])
        self.assertTrue(snapshot["readiness"]["bwapi_release_patch_applied"])
        self.assertTrue(snapshot["readiness"]["wmode_ready"])
        self.assertEqual("game_running", snapshot["summary"]["phase"])
        self.assertEqual("ok", snapshot["summary"]["severity"])

    def test_status_reader_accepts_bwapi_event_exporter_proxy(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_status import StarCraft116StatusReader

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        bwapi_dir = game_dir / "bwapi-data"
        ai_dir = bwapi_dir / "AI"
        chaos_dir = plugin_root / "Chaoslauncher"
        ai_dir.mkdir(parents=True)
        chaos_dir.mkdir(parents=True)
        bot_path = ai_dir / "Stardust.dll"
        exporter_path = ai_dir / "LAVEventExporter.dll"
        bot_path.write_text("", encoding="utf-8")
        exporter_path.write_text("", encoding="utf-8")
        (ai_dir / "LAVEventExporter.ini").write_text(
            "\n".join([
                "wrapped_ai=Stardust.dll",
                f"events_path={plugin_root / 'events.jsonl'}",
            ]),
            encoding="utf-8",
        )
        (bwapi_dir / "bwapi.ini").write_text(
            "\n".join([
                "ai     = bwapi-data/AI/LAVEventExporter.dll",
                "ai_dbg = bwapi-data/AI/LAVEventExporter.dll",
                "race = Protoss",
            ]),
            encoding="utf-8",
        )
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "stardust",
                "bwapi_event_exporter_enabled": True,
                "profiles": {
                    "stardust": {
                        "starcraft_116_dir": str(game_dir),
                        "bwapi_data_dir": str(bwapi_dir),
                        "bot_binary_path": str(bot_path),
                        "start_chaoslauncher": True,
                        "chaoslauncher_path": str(chaos_dir / "Chaoslauncher.exe"),
                        "chaoslauncher_working_dir": str(chaos_dir),
                    },
                },
            },
        )
        reader = StarCraft116StatusReader(StarCraft116Config(str(plugin_root)))
        with mock.patch.object(
            reader,
            "process_snapshot",
            return_value={
                "supported": True,
                "matches": {
                    "StarCraft.exe": [],
                    "Chaoslauncher.exe": [],
                },
                "errors": [],
            },
        ):
            snapshot = reader.snapshot()

        self.assertEqual(
            "LAVEventExporter.dll",
            snapshot["bwapi_ini"]["configured_ai_binary"],
        )
        self.assertTrue(snapshot["bwapi_ini"]["configured_ai_is_exporter"])
        self.assertTrue(snapshot["bwapi_ini"]["expected_bot_matches_ini"])
        self.assertTrue(
            snapshot["bwapi_event_exporter"]["wrapped_ai_matches_profile"]
        )
        self.assertIn(
            "wrapping Stardust.dll",
            "\n".join(snapshot["summary"]["messages"]),
        )

    def test_status_reader_accepts_recent_exporter_events_as_runtime_evidence(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_status import StarCraft116StatusReader

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        bwapi_dir = game_dir / "bwapi-data"
        ai_dir = bwapi_dir / "AI"
        chaos_dir = plugin_root / "Chaoslauncher"
        events_path = plugin_root / "events.jsonl"
        ai_dir.mkdir(parents=True)
        chaos_dir.mkdir(parents=True)
        bot_path = ai_dir / "Stardust.dll"
        bot_path.write_text("", encoding="utf-8")
        (ai_dir / "LAVEventExporter.dll").write_text("", encoding="utf-8")
        (ai_dir / "LAVEventExporter.ini").write_text(
            "\n".join([
                "wrapped_ai=Stardust.dll",
                f"events_path={events_path}",
            ]),
            encoding="utf-8",
        )
        (bwapi_dir / "bwapi.ini").write_text(
            "\n".join([
                "ai     = bwapi-data/AI/LAVEventExporter.dll",
                "ai_dbg = bwapi-data/AI/LAVEventExporter.dll",
            ]),
            encoding="utf-8",
        )
        (chaos_dir / "Chaoslauncher.log").write_text(
            "Plugin loaded BWAPI 4.4.0 Injector [RELEASE]\n",
            encoding="utf-8",
        )
        events_path.write_text(
            json.dumps({
                "source": "LAVEventExporter",
                "event_type": "state_snapshot",
                "summary": "Economy snapshot: 50 minerals.",
                "frame": 144,
            })
            + "\n",
            encoding="utf-8",
        )
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "stardust",
                "bwapi_event_exporter_enabled": True,
                "profiles": {
                    "stardust": {
                        "starcraft_116_dir": str(game_dir),
                        "bwapi_data_dir": str(bwapi_dir),
                        "bot_binary_path": str(bot_path),
                        "start_chaoslauncher": True,
                        "chaoslauncher_path": str(chaos_dir / "Chaoslauncher.exe"),
                        "chaoslauncher_working_dir": str(chaos_dir),
                    },
                },
            },
        )
        reader = StarCraft116StatusReader(StarCraft116Config(str(plugin_root)))
        with mock.patch.object(
            reader,
            "process_snapshot",
            return_value={
                "supported": True,
                "matches": {
                    "StarCraft.exe": [{"image": "StarCraft.exe", "pid": 1161}],
                    "Chaoslauncher.exe": [],
                },
                "errors": [],
            },
        ):
            snapshot = reader.snapshot()

        self.assertTrue(snapshot["readiness"]["bwapi_runtime_event_seen"])
        self.assertEqual("game_running", snapshot["summary"]["phase"])
        self.assertEqual("ok", snapshot["summary"]["severity"])
        self.assertIn(
            "BWAPI runtime event evidence",
            "\n".join(snapshot["summary"]["messages"]),
        )

    def test_exporter_manager_builds_sidecar_config_text(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_exporter import (
            StarCraft116ExporterManager,
        )

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        bwapi_dir = game_dir / "bwapi-data"
        ai_dir = bwapi_dir / "AI"
        ai_dir.mkdir(parents=True)
        bot_path = ai_dir / "Stardust.dll"
        bot_path.write_text("", encoding="utf-8")
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "stardust",
                "game_events_path": str(plugin_root / "events.jsonl"),
                "profiles": {
                    "stardust": {
                        "bwapi_data_dir": str(bwapi_dir),
                        "bot_binary_path": str(bot_path),
                    },
                },
            },
        )
        manager = StarCraft116ExporterManager(StarCraft116Config(str(plugin_root)))

        text = manager.build_ini_text("stardust")

        self.assertIn("wrapped_ai=Stardust.dll", text)
        self.assertIn(f"events_path={plugin_root / 'events.jsonl'}", text)
        self.assertIn("snapshot_interval_frames=144", text)

    def test_exporter_manager_uses_absolute_wrapped_ai_for_external_bot(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_exporter import (
            StarCraft116ExporterManager,
        )

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        bwapi_dir = game_dir / "bwapi-data"
        ai_dir = bwapi_dir / "AI"
        external_dir = game_dir / "Crona"
        ai_dir.mkdir(parents=True)
        external_dir.mkdir(parents=True)
        bot_path = external_dir / "BananaBrain.dll"
        bot_path.write_text("", encoding="utf-8")
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "crona",
                "profiles": {
                    "crona": {
                        "bwapi_data_dir": str(bwapi_dir),
                        "bot_binary_path": str(bot_path),
                    },
                },
            },
        )
        manager = StarCraft116ExporterManager(StarCraft116Config(str(plugin_root)))

        text = manager.build_ini_text("crona")

        self.assertIn(f"wrapped_ai={bot_path}", text)
        self.assertNotIn("wrapped_ai=BananaBrain.dll", text)

    def test_exporter_manager_writes_sidecar_config_for_profile(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_exporter import (
            StarCraft116ExporterManager,
        )

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        bwapi_dir = game_dir / "bwapi-data"
        ai_dir = bwapi_dir / "AI"
        external_dir = game_dir / "Crona"
        ai_dir.mkdir(parents=True)
        external_dir.mkdir(parents=True)
        bot_path = external_dir / "BananaBrain.dll"
        bot_path.write_text("", encoding="utf-8")
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "crona",
                "game_events_path": str(plugin_root / "events.jsonl"),
                "profiles": {
                    "crona": {
                        "bwapi_data_dir": str(bwapi_dir),
                        "bot_binary_path": str(bot_path),
                    },
                },
            },
        )
        manager = StarCraft116ExporterManager(StarCraft116Config(str(plugin_root)))

        ok, message = manager.write_ini("crona")

        self.assertTrue(ok)
        self.assertIn("crona", message)
        ini_text = (ai_dir / "LAVEventExporter.ini").read_text(encoding="utf-8")
        self.assertIn(f"wrapped_ai={bot_path}", ini_text)
        self.assertIn(f"events_path={plugin_root / 'events.jsonl'}", ini_text)

    def test_launch_config_sync_skips_monster_profile(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_launch_config_sync import (
            StarCraft116LaunchConfigSync,
        )

        class Config:
            def get_bool(self, key, default=False):
                return True

        class Exporter:
            def write_ini(self, profile_name):
                raise AssertionError("write_ini should not be called")

            def write_bwapi_ini_ai(self, profile_name, use_exporter=None):
                raise AssertionError("write_bwapi_ini_ai should not be called")

        sync = StarCraft116LaunchConfigSync(
            Config(),
            Exporter(),
            is_monster_profile=lambda profile_name: True,
        )

        ok, message = sync.sync("monster")

        self.assertTrue(ok)
        self.assertIn("standalone BWAPI observer", message)

    def test_launch_config_sync_falls_back_when_exporter_ini_is_locked(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_launch_config_sync import (
            StarCraft116LaunchConfigSync,
        )

        calls = []

        class Config:
            def get_bool(self, key, default=False):
                return key == "bwapi_event_exporter_enabled"

        class Exporter:
            def write_ini(self, profile_name):
                calls.append(("write_ini", profile_name))
                raise PermissionError("locked")

            def write_bwapi_ini_ai(self, profile_name, use_exporter=None):
                calls.append(("write_bwapi_ini_ai", profile_name, use_exporter))
                return True, "bwapi updated"

        sync = StarCraft116LaunchConfigSync(
            Config(),
            Exporter(),
            is_monster_profile=lambda profile_name: False,
        )

        ok, message = sync.sync("stardust")

        self.assertTrue(ok)
        self.assertEqual(
            [
                ("write_ini", "stardust"),
                ("write_bwapi_ini_ai", "stardust", False),
            ],
            calls,
        )
        self.assertIn("Skipped LAVEventExporter.ini update", message)
        self.assertIn("bwapi updated", message)

    def test_launch_config_sync_returns_bwapi_ini_failure(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_launch_config_sync import (
            StarCraft116LaunchConfigSync,
        )

        class Config:
            def get_bool(self, key, default=False):
                return False

        class Exporter:
            def write_bwapi_ini_ai(self, profile_name, use_exporter=None):
                return False, "bwapi failed"

        sync = StarCraft116LaunchConfigSync(
            Config(),
            Exporter(),
            is_monster_profile=lambda profile_name: False,
        )

        ok, message = sync.sync("stardust")

        self.assertFalse(ok)
        self.assertEqual("bwapi failed", message)

    def test_status_reader_parses_tasklist_csv(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_status import StarCraft116StatusReader

        rows = StarCraft116StatusReader._parse_tasklist_output(
            '"StarCraft.exe","1161","Console","1","42,000 K"\n'
            'INFO: No tasks are running which match the specified criteria.\n'
        )

        self.assertEqual(1, len(rows))
        self.assertEqual("StarCraft.exe", rows[0]["image"])
        self.assertEqual(1161, rows[0]["pid"])
        self.assertEqual("42,000 K", rows[0]["memory"])

    def test_status_reader_reports_launcher_waiting_for_start(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_status import StarCraft116StatusReader

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        bwapi_dir = game_dir / "bwapi-data"
        ai_dir = bwapi_dir / "AI"
        chaos_dir = plugin_root / "Chaoslauncher"
        ai_dir.mkdir(parents=True)
        chaos_dir.mkdir(parents=True)
        bot_path = ai_dir / "Stardust.dll"
        bot_path.write_text("", encoding="utf-8")
        (bwapi_dir / "bwapi.ini").write_text(
            "ai = bwapi-data/AI/Stardust.dll\n",
            encoding="utf-8",
        )
        (chaos_dir / "Chaoslauncher.log").write_text(
            "\n".join([
                "Plugin loaded BWAPI 4.4.0 Injector [RELEASE]",
                "Plugin loaded W-MODE 1.02",
            ]),
            encoding="utf-8",
        )
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "stardust",
                "profiles": {
                    "stardust": {
                        "bwapi_data_dir": str(bwapi_dir),
                        "bot_binary_path": str(bot_path),
                        "start_chaoslauncher": True,
                        "chaoslauncher_path": str(chaos_dir / "Chaoslauncher.exe"),
                        "chaoslauncher_working_dir": str(chaos_dir),
                    },
                },
            },
        )
        reader = StarCraft116StatusReader(StarCraft116Config(str(plugin_root)))
        with mock.patch.object(
            reader,
            "process_snapshot",
            return_value={
                "supported": True,
                "matches": {
                    "StarCraft.exe": [],
                    "Chaoslauncher.exe": [
                        {"image": "Chaoslauncher.exe", "pid": 31892},
                    ],
                },
                "errors": [],
            },
        ):
            snapshot = reader.snapshot()

        self.assertEqual("launcher_waiting_for_start", snapshot["summary"]["phase"])
        self.assertIn("waiting for Start", snapshot["summary"]["message"])
        self.assertTrue(snapshot["readiness"]["chaoslauncher_process_running"])

    def test_status_reader_reports_log_only_without_process_as_warning(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_status import StarCraft116StatusReader

        plugin_root = self.make_plugin_root()
        bwapi_dir = plugin_root / "Star Craft 116" / "bwapi-data"
        ai_dir = bwapi_dir / "AI"
        chaos_dir = plugin_root / "Chaoslauncher"
        ai_dir.mkdir(parents=True)
        chaos_dir.mkdir(parents=True)
        bot_path = ai_dir / "Stardust.dll"
        bot_path.write_text("", encoding="utf-8")
        (bwapi_dir / "bwapi.ini").write_text(
            "ai = bwapi-data/AI/Stardust.dll\n",
            encoding="utf-8",
        )
        (chaos_dir / "Chaoslauncher.log").write_text(
            "Plugin loaded BWAPI 4.4.0 Injector [RELEASE]\n",
            encoding="utf-8",
        )
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "stardust",
                "profiles": {
                    "stardust": {
                        "bwapi_data_dir": str(bwapi_dir),
                        "bot_binary_path": str(bot_path),
                        "start_chaoslauncher": True,
                        "chaoslauncher_path": str(chaos_dir / "Chaoslauncher.exe"),
                        "chaoslauncher_working_dir": str(chaos_dir),
                    },
                },
            },
        )
        reader = StarCraft116StatusReader(StarCraft116Config(str(plugin_root)))
        with mock.patch.object(
            reader,
            "process_snapshot",
            return_value={
                "supported": True,
                "matches": {
                    "StarCraft.exe": [],
                    "Chaoslauncher.exe": [],
                },
                "errors": [],
            },
        ):
            snapshot = reader.snapshot()

        self.assertEqual("last_launcher_log_only", snapshot["summary"]["phase"])
        self.assertEqual("warning", snapshot["summary"]["severity"])
        self.assertIn("no current process", snapshot["summary"]["message"])
        self.assertIn("Launch BWAPI Profile", snapshot["summary"]["next_actions"][0])

    def test_runtime_state_syncs_external_starcraft_process(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_state import StarCraft116RuntimeState

        state = StarCraft116RuntimeState(profile="stardust")

        state.update_from_external_status({
            "processes": {
                "matches": {
                    "StarCraft.exe": [
                        {"image": "StarCraft.exe", "pid": 1161},
                    ],
                    "Chaoslauncher.exe": [],
                },
            },
            "summary": {
                "message": "StarCraft is running with BWAPI release patch evidence.",
            },
        })

        self.assertTrue(state.running)
        self.assertEqual(1, len(state.processes))
        self.assertEqual("external_starcraft", state.processes[0]["label"])
        self.assertEqual(1161, state.processes[0]["pid"])
        self.assertIn("StarCraft is running", state.last_message)

    def test_status_reader_resolves_management_paths(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_status import StarCraft116StatusReader

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        bwapi_dir = game_dir / "bwapi-data"
        chaos_dir = plugin_root / "Chaoslauncher"
        bwapi_dir.mkdir(parents=True)
        chaos_dir.mkdir(parents=True)
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "stardust",
                "profiles": {
                    "stardust": {
                        "starcraft_116_dir": str(game_dir),
                        "bwapi_data_dir": str(bwapi_dir),
                        "chaoslauncher_path": str(chaos_dir / "Chaoslauncher.exe"),
                        "chaoslauncher_working_dir": str(chaos_dir),
                    },
                },
            },
        )

        paths = StarCraft116StatusReader(
            StarCraft116Config(str(plugin_root))
        ).management_paths()

        self.assertEqual(str(bwapi_dir / "bwapi.ini"), paths["bwapi_ini"])
        self.assertEqual(str(chaos_dir), paths["chaoslauncher_folder"])
        self.assertEqual(str(game_dir), paths["starcraft_folder"])

    def test_management_open_button_opens_existing_target(self):
        from plugins.StarCraft116.starcraft116 import StarCraft116
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_status import StarCraft116StatusReader

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        bwapi_dir = game_dir / "bwapi-data"
        bwapi_dir.mkdir(parents=True)
        ini_path = bwapi_dir / "bwapi.ini"
        ini_path.write_text("ai = bwapi-data/AI/Stardust.dll\n", encoding="utf-8")
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "stardust",
                "profiles": {
                    "stardust": {
                        "starcraft_116_dir": str(game_dir),
                        "bwapi_data_dir": str(bwapi_dir),
                    },
                },
            },
        )
        config = StarCraft116Config(str(plugin_root))
        plugin = StarCraft116.__new__(StarCraft116)
        plugin.config_manager = config
        plugin.status_reader = StarCraft116StatusReader(config)

        with mock.patch(
            "plugins.StarCraft116.starcraft116.os.startfile",
            create=True,
        ) as startfile_mock:
            message = plugin._open_management_target("bwapi_ini", "file")

        startfile_mock.assert_called_once_with(str(ini_path))
        self.assertIn("Opened bwapi.ini", message)

    def test_management_open_button_reports_missing_target(self):
        from plugins.StarCraft116.starcraft116 import StarCraft116
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_status import StarCraft116StatusReader

        plugin_root = self.make_plugin_root()
        bwapi_dir = plugin_root / "Star Craft 116" / "bwapi-data"
        bwapi_dir.mkdir(parents=True)
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "stardust",
                "profiles": {
                    "stardust": {
                        "bwapi_data_dir": str(bwapi_dir),
                    },
                },
            },
        )
        config = StarCraft116Config(str(plugin_root))
        plugin = StarCraft116.__new__(StarCraft116)
        plugin.config_manager = config
        plugin.status_reader = StarCraft116StatusReader(config)

        with mock.patch(
            "plugins.StarCraft116.starcraft116.os.startfile",
            create=True,
        ) as startfile_mock:
            message = plugin._open_management_target("bwapi_ini", "file")

        startfile_mock.assert_not_called()
        self.assertIn("does not exist", message)

    def test_status_reaction_policy_builds_concise_event(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_reaction_policy import (
            build_starcraft116_reaction_user_message,
            build_starcraft116_status_event,
            build_starcraft116_status_event_key,
        )

        external_status = {
            "profile": "stardust",
            "generated_at": 123.456,
            "bwapi_ini": {
                "configured_ai_binary": "Stardust.dll",
                "expected_bot_matches_ini": True,
            },
            "chaoslauncher_log": {
                "recent_relevant_lines": [
                    "Plugin loaded BWAPI 4.4.0 Injector [RELEASE]",
                    "ApplyPatch for BWAPI 4.4.0 Injector [RELEASE]",
                ],
            },
            "readiness": {
                "starcraft_process_running": True,
                "chaoslauncher_process_running": True,
                "bwapi_release_patch_applied": True,
                "wmode_ready": True,
                "debug_privilege_obtained": True,
                "starcraft_start_completed": True,
            },
            "summary": {
                "phase": "game_running",
                "severity": "ok",
                "message": "StarCraft is running with BWAPI release patch evidence.",
                "messages": [
                    "StarCraft is running with BWAPI release patch evidence.",
                    "BWAPI ai is configured as Stardust.dll.",
                ],
                "next_actions": [],
            },
        }

        event = build_starcraft116_status_event(
            external_status,
            source="refresh",
        )

        self.assertEqual("game_running", event["phase"])
        self.assertEqual("stardust", event["profile"])
        self.assertEqual("Stardust.dll", event["configured_ai_binary"])
        self.assertTrue(event["readiness"]["bwapi_release_patch_applied"])
        self.assertIn("game_running", build_starcraft116_status_event_key(event))
        user_message = build_starcraft116_reaction_user_message(event)
        self.assertIn("Stardust.dll", user_message)
        self.assertIn("summary_messages:", user_message)

    def test_status_reaction_policy_speak_decision_matches_existing_rules(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_reaction_policy import (
            is_log_only_starcraft116_game_event,
            is_log_only_starcraft116_status_event,
            should_speak_starcraft116_event,
        )

        proxy_loaded = {
            "source": "game_event",
            "event_type": "bwapi_proxy_loaded",
        }
        morphed = {
            "source": "game_event",
            "event_type": "unit_morphed",
        }
        launcher_waiting = {
            "phase": "launcher_waiting_for_start",
        }
        config_missing = {
            "phase": "config_missing",
        }

        self.assertTrue(is_log_only_starcraft116_game_event(proxy_loaded))
        self.assertFalse(should_speak_starcraft116_event(proxy_loaded))
        self.assertFalse(is_log_only_starcraft116_game_event(morphed))
        self.assertTrue(should_speak_starcraft116_event(morphed))
        self.assertTrue(is_log_only_starcraft116_status_event(launcher_waiting))
        self.assertFalse(should_speak_starcraft116_event(launcher_waiting))
        self.assertFalse(is_log_only_starcraft116_status_event(config_missing))
        self.assertTrue(should_speak_starcraft116_event(config_missing))

    def test_status_event_callback_emits_once_per_status_key(self):
        from plugins.StarCraft116.starcraft116 import StarCraft116

        plugin = StarCraft116.__new__(StarCraft116)
        plugin.config_manager = mock.Mock()
        plugin.config_manager.get_bool.return_value = True
        plugin.status_event_callback = mock.Mock()
        plugin._last_status_event_key = ""
        external_status = {
            "profile": "stardust",
            "generated_at": 1.0,
            "bwapi_ini": {
                "configured_ai_binary": "Stardust.dll",
                "expected_bot_matches_ini": True,
            },
            "chaoslauncher_log": {
                "recent_relevant_lines": [],
            },
            "readiness": {
                "starcraft_process_running": True,
                "chaoslauncher_process_running": True,
                "bwapi_release_patch_applied": True,
                "wmode_ready": True,
                "debug_privilege_obtained": True,
                "starcraft_start_completed": True,
            },
            "summary": {
                "phase": "game_running",
                "severity": "ok",
                "message": "StarCraft is running with BWAPI release patch evidence.",
                "messages": [
                    "StarCraft is running with BWAPI release patch evidence.",
                ],
                "next_actions": [],
            },
        }

        self.assertTrue(plugin._maybe_emit_status_event(
            external_status,
            "refresh",
        ))
        self.assertFalse(plugin._maybe_emit_status_event(
            external_status,
            "refresh",
        ))
        plugin.status_event_callback.assert_called_once()

        changed_status = dict(external_status)
        changed_status["summary"] = dict(external_status["summary"])
        changed_status["summary"]["phase"] = "last_run_completed_or_exited"
        changed_status["summary"]["severity"] = "warning"
        changed_status["readiness"] = dict(external_status["readiness"])
        changed_status["readiness"]["starcraft_process_running"] = False

        self.assertTrue(plugin._maybe_emit_status_event(
            changed_status,
            "refresh",
        ))
        self.assertEqual(2, plugin.status_event_callback.call_count)

    def test_status_event_callback_respects_reaction_toggle(self):
        from plugins.StarCraft116.starcraft116 import StarCraft116

        plugin = StarCraft116.__new__(StarCraft116)
        plugin.config_manager = mock.Mock()
        plugin.config_manager.get_bool.return_value = False
        plugin.status_event_callback = mock.Mock()
        plugin._last_status_event_key = ""

        emitted = plugin._maybe_emit_status_event({
            "summary": {
                "phase": "game_running",
                "severity": "ok",
                "messages": [],
                "next_actions": [],
            },
        }, "refresh")

        self.assertFalse(emitted)
        plugin.status_event_callback.assert_not_called()

    def test_starcraft116_reaction_runtime_uses_openai_text(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_reaction_runtime import (
            run_starcraft116_status_reaction,
        )

        llm = mock.Mock()
        llm.generate_text_only.return_value = "스타 떴어, Stardust도 제대로 물렸네."
        tts = mock.Mock()
        event = {
            "phase": "game_running",
            "severity": "ok",
            "configured_ai_binary": "Stardust.dll",
            "readiness": {},
            "messages": [],
            "next_actions": [],
            "recent_relevant_lines": [],
        }

        run_starcraft116_status_reaction(llm, tts, event)

        llm.generate_text_only.assert_called_once()
        tts.receive_input.assert_called_once_with(
            "스타 떴어, Stardust도 제대로 물렸네."
        )

    def test_starcraft116_reaction_runtime_applies_polite_speech_style(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_reaction_runtime import (
            run_starcraft116_status_reaction,
        )

        llm = mock.Mock()
        llm.speech_style_mode = "polite"
        llm.generate_text_only.return_value = "스타 떴어, Stardust도 제대로 물렸네."
        tts = mock.Mock()
        event = {
            "phase": "game_running",
            "severity": "ok",
            "configured_ai_binary": "Stardust.dll",
            "readiness": {},
            "messages": [],
            "next_actions": [],
            "recent_relevant_lines": [],
        }

        run_starcraft116_status_reaction(llm, tts, event)

        system_prompt = llm.generate_text_only.call_args.args[1]
        self.assertIn("존댓말", system_prompt)
        tts.receive_input.assert_called_once_with(
            "스타 떴어요, Stardust도 제대로 물렸네요."
        )

    def test_starcraft116_reaction_runtime_keeps_active_warning_idempotent(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_reaction_runtime import (
            run_starcraft116_status_reaction,
        )

        llm = mock.Mock()
        llm.generate_text_only.return_value = "적의 SCV를 발견했어! 조심해야겠다."
        tts = mock.Mock()
        event = {
            "source": "game_event",
            "event_type": "enemy_spotted",
            "phase": "game_event",
            "severity": "info",
            "summary": "Terran_SCV spotted.",
            "details": {},
            "unit_mentions": [{
                "role": "unit",
                "raw_type": "Terran_SCV",
                "speak_name": "SCV",
                "owner": "enemy",
            }],
        }

        run_starcraft116_status_reaction(llm, tts, event)

        tts.receive_input.assert_called_once_with(
            "적의 SCV를 발견했어! 조심해야겠다."
        )

    def test_starcraft116_reaction_runtime_falls_back_to_tts(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_reaction_runtime import (
            run_starcraft116_status_reaction,
        )

        llm = mock.Mock()
        llm.generate_text_only.side_effect = RuntimeError("openai unavailable")
        tts = mock.Mock()
        event = {
            "phase": "config_missing",
            "severity": "warning",
            "configured_ai_binary": "Stardust.dll",
            "readiness": {},
            "messages": [],
            "next_actions": [],
            "recent_relevant_lines": [],
        }

        run_starcraft116_status_reaction(llm, tts, event)

        tts.receive_input.assert_called_once()
        self.assertIn("BWAPI 설정 파일", tts.receive_input.call_args[0][0])

    def test_starcraft116_reaction_callback_factory_preserves_log_only_skip(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_reaction_runtime import (
            build_starcraft116_status_event_callback,
        )

        llm = mock.Mock()
        tts = mock.Mock()
        callback = build_starcraft116_status_event_callback(llm, tts)

        callback({
            "phase": "launcher_waiting_for_start",
            "severity": "ok",
            "configured_ai_binary": "Stardust.dll",
            "readiness": {},
            "messages": [],
            "next_actions": [],
            "recent_relevant_lines": [],
        })

        llm.generate_text_only.assert_not_called()
        tts.receive_input.assert_not_called()

    def test_starcraft116_reaction_runtime_skips_launcher_waiting_tts(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_reaction_runtime import (
            run_starcraft116_status_reaction,
        )

        llm = mock.Mock()
        tts = mock.Mock()
        event = {
            "phase": "launcher_waiting_for_start",
            "severity": "ok",
            "configured_ai_binary": "Stardust.dll",
            "readiness": {},
            "messages": [],
            "next_actions": [
                "In Chaoslauncher, check BWAPI 4.4.0 Injector [RELEASE] and W-MODE 1.02, then press Start.",
            ],
            "recent_relevant_lines": [],
        }

        run_starcraft116_status_reaction(llm, tts, event)

        llm.generate_text_only.assert_not_called()
        tts.receive_input.assert_not_called()

    def test_game_event_tailer_reads_complete_jsonl_only(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_game_events import (
            StarCraft116GameEventTailer,
        )

        plugin_root = self.make_plugin_root()
        event_path = plugin_root / "events.jsonl"
        event_path.write_text(
            json.dumps({"event_type": "enemy_spotted", "summary": "Zergling spotted"})
            + "\n"
            + "{\"event_type\": \"combat_",
            encoding="utf-8",
        )
        tailer = StarCraft116GameEventTailer(start_at_end=False)

        first = tailer.read_new_events(str(event_path), max_events=10)

        self.assertEqual(1, len(first.events))
        self.assertEqual("enemy_spotted", first.events[0]["event_type"])
        self.assertEqual([], first.errors)

        with event_path.open("a", encoding="utf-8") as file:
            file.write("started\", \"summary\": \"Fight started\"}\n")

        second = tailer.read_new_events(str(event_path), max_events=10)

        self.assertEqual(1, len(second.events))
        self.assertEqual("combat_started", second.events[0]["event_type"])

    def test_game_event_tailer_start_at_end_skips_existing_events(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_game_events import (
            StarCraft116GameEventTailer,
        )

        plugin_root = self.make_plugin_root()
        event_path = plugin_root / "events.jsonl"
        event_path.write_text(
            json.dumps({"event_type": "old_event"}) + "\n",
            encoding="utf-8",
        )
        tailer = StarCraft116GameEventTailer(start_at_end=True)

        first = tailer.read_new_events(str(event_path), max_events=10)
        with event_path.open("a", encoding="utf-8") as file:
            file.write(json.dumps({"event_type": "new_event"}) + "\n")
        second = tailer.read_new_events(str(event_path), max_events=10)

        self.assertEqual([], first.events)
        self.assertEqual(["new_event"], [
            event["event_type"]
            for event in second.events
        ])

    def test_game_event_tailer_accepts_utf8_bom(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_game_events import (
            StarCraft116GameEventTailer,
        )

        plugin_root = self.make_plugin_root()
        event_path = plugin_root / "events.jsonl"
        event_path.write_bytes(
            b"\xef\xbb\xbf"
            + json.dumps({
                "event_type": "enemy_spotted",
                "summary": "Enemy Zergling spotted",
            }).encode("utf-8")
            + b"\n"
        )
        tailer = StarCraft116GameEventTailer(start_at_end=False)

        result = tailer.read_new_events(str(event_path), max_events=10)

        self.assertEqual([], result.errors)
        self.assertEqual(1, len(result.events))
        self.assertEqual("enemy_spotted", result.events[0]["event_type"])

    def test_monster_log_tailer_builds_events_from_plain_text(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_monster_log_events import (
            StarCraft116MonsterLogTailer,
        )

        plugin_root = self.make_plugin_root()
        log_path = plugin_root / "monster_log.txt"
        log_path.write_text(
            "\n".join([
                "Waiting to connect...",
                "Connected to BWAPI.",
                "Joined a game.",
                "sc.dat is missing. Aborting.Disconnected",
                "ExitCode: 1",
            ])
            + "\n",
            encoding="utf-8",
        )
        tailer = StarCraft116MonsterLogTailer(start_at_end=False)

        result = tailer.read_new_events(str(log_path), max_events=10)

        self.assertEqual([], result.errors)
        self.assertEqual([
            "monster_connection_successful",
            "monster_joined_game",
            "monster_missing_sc_dat",
            "monster_exit_code",
        ], [event["event_type"] for event in result.events])
        self.assertEqual("error", result.events[-1]["severity"])
        self.assertFalse(any(
            event.get("tts_eligible", True)
            for event in result.events
        ))

    def test_monster_log_tailer_builds_events_from_exit_code_variants(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_monster_log_events import (
            StarCraft116MonsterLogTailer,
        )

        plugin_root = self.make_plugin_root()
        log_path = plugin_root / "monster_log.txt"
        log_path.write_text(
            "\n".join([
                "Connected to BWAPI.",
                "Exit code = 1",
                "ExitCode 0",
            ])
            + "\n",
            encoding="utf-8",
        )
        tailer = StarCraft116MonsterLogTailer(start_at_end=False)

        result = tailer.read_new_events(str(log_path), max_events=10)

        self.assertEqual([], result.errors)
        self.assertEqual([
            "monster_connection_successful",
            "monster_exit_code",
            "monster_exit_code",
        ], [event["event_type"] for event in result.events])
        self.assertEqual(1, result.events[1]["details"]["exit_code"])
        self.assertEqual("launch_or_runtime_error", result.events[1]["details"]["cause"])
        self.assertIn("launch/runtime", result.events[1]["details"]["reason"])
        self.assertEqual(0, result.events[2]["details"]["exit_code"])
        self.assertEqual("normal_exit", result.events[2]["details"]["cause"])
        self.assertEqual("process ended normally", result.events[2]["details"]["reason"])

    def test_monster_log_tailer_builds_monster_exit_code_from_end_block(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_monster_log_events import (
            StarCraft116MonsterLogTailer,
        )

        plugin_root = self.make_plugin_root()
        log_path = plugin_root / "monster_log.txt"
        log_path.write_text(
            "\n".join([
                "Start: 2026-07-06 16:19:34.17",
                "Connected to BWAPI.",
                "Joined a game.",
                "failed, disconnecting",
                "Game ended.",
                "Disconnected",
                "End: 2026-07-06 16:19:56.99",
                "ExitCode: 1",
            ])
            + "\n",
            encoding="utf-8",
        )
        tailer = StarCraft116MonsterLogTailer(start_at_end=False)

        result = tailer.read_new_events(str(log_path), max_events=10)

        self.assertEqual([], result.errors)
        self.assertEqual([
            "monster_connection_successful",
            "monster_joined_game",
            "monster_game_ended",
            "monster_disconnected",
            "monster_exit_code",
        ], [event["event_type"] for event in result.events])
        exit_event = result.events[-1]
        self.assertEqual(1, exit_event["details"]["exit_code"])
        self.assertEqual("disconnected", exit_event["details"]["cause"])
        self.assertIn("disconnection", exit_event["details"]["reason"])

    def test_monster_log_tailer_accepts_explicit_lav_event_json(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_monster_log_events import (
            StarCraft116MonsterLogTailer,
        )

        plugin_root = self.make_plugin_root()
        log_path = plugin_root / "monster_log.txt"
        payload = {
            "event_type": "state_snapshot",
            "summary": "Economy snapshot: 50 minerals, 0 gas, supply 4/9.",
            "frame": 144,
            "resources": {
                "minerals": 50,
                "gas": 0,
                "supply": "4/9",
            },
        }
        log_path.write_text(
            "Connected to BWAPI.\n"
            + "LAV_EVENT "
            + json.dumps(payload)
            + "\n",
            encoding="utf-8",
        )
        tailer = StarCraft116MonsterLogTailer(start_at_end=False)

        result = tailer.read_new_events(str(log_path), max_events=10)

        self.assertEqual([
            "monster_connection_successful",
            "state_snapshot",
        ], [event["event_type"] for event in result.events])
        self.assertFalse(result.events[0]["tts_eligible"])
        self.assertTrue(result.events[1]["tts_eligible"])
        self.assertEqual("lav_starcraft116_bwapi_event_v1", result.events[1]["schema"])
        self.assertEqual("Monster.exe", result.events[1]["source"])
        self.assertEqual(50, result.events[1]["resources"]["minerals"])

    def test_config_resolves_bwapi_proxy_events_path(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config

        plugin_root = self.make_plugin_root()
        game_dir = plugin_root / "Star Craft 116"
        bwapi_dir = game_dir / "bwapi-data"
        bwapi_dir.mkdir(parents=True)
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "monster",
                "profiles": {
                    "monster": {
                        "display_name": "Monster",
                        "starcraft_working_dir": str(game_dir),
                    },
                },
            },
        )

        config = StarCraft116Config(str(plugin_root))

        self.assertEqual(
            str(bwapi_dir / "bwapi_proxy_events.jsonl"),
            config.resolve_bwapi_proxy_events_path(),
        )

    def test_game_event_policy_builds_openai_message(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_reaction_policy import (
            build_starcraft116_fallback_reaction,
            build_starcraft116_game_event,
            build_starcraft116_reaction_tts_text,
            build_starcraft116_reaction_user_message,
        )

        event = build_starcraft116_game_event({
            "event_type": "enemy_spotted",
            "summary": "Enemy Zergling spotted near natural.",
            "frame": 3270,
            "resources": {"minerals": 180, "gas": 0, "supply": "12/17"},
            "units": {"Probe": 12, "Zealot": 1},
        }, profile="stardust")

        self.assertEqual("game_event", event["source"])
        self.assertEqual("enemy_spotted", event["event_type"])
        self.assertEqual("stardust", event["profile"])
        self.assertIn("저글링", [
            mention["speak_name"]
            for mention in event["unit_mentions"]
        ])
        user_message = build_starcraft116_reaction_user_message(event)
        self.assertIn("StarCraft 1.16 BWAPI game event", user_message)
        self.assertIn("allowed_unit_names:", user_message)
        self.assertIn("저글링", user_message)
        self.assertNotIn("English exactly", user_message)
        self.assertIn("Enemy Zergling spotted", user_message)
        self.assertIn("minerals", user_message)
        self.assertEqual(
            "Enemy Zergling spotted near natural.",
            build_starcraft116_fallback_reaction(event),
        )

        combat_event = build_starcraft116_game_event({
            "event_type": "combat_started",
            "summary": "Combat started: Protoss_Dragoon vs enemy Protoss_Forge.",
            "friendly_unit": {"type": "Protoss_Dragoon", "owner": "self"},
            "enemy_unit": {"type": "Protoss_Forge", "owner": "enemy"},
        }, profile="stardust")
        polished = build_starcraft116_reaction_tts_text(
            combat_event,
            "경제 상황은 괜찮은데, 공급이 막히면 포지가 완공됐어도 조심해. 드라군이 포지와 싸우고 있어.",
        )
        self.assertNotIn("경제", polished)
        self.assertNotIn("공급", polished)
        self.assertNotIn("완공", polished)
        self.assertIn("자원 상태", polished)
        self.assertIn("인구수", polished)
        self.assertIn("완성", polished)
        self.assertIn("조심해야겠다", polished)
        self.assertIn("드라군이 포지를 박살내고 있어", polished)

        already_polished = build_starcraft116_reaction_tts_text(
            combat_event,
            polished,
        )
        self.assertEqual(polished, already_polished)

        warning = build_starcraft116_reaction_tts_text(
            combat_event,
            "적의 SCV를 발견했어! 조심해야겠다.",
        )
        self.assertEqual("내 드라군이 포지를 박살내고 있어.", warning)

        build_started = build_starcraft116_game_event({
            "event_type": "building_started",
            "summary": "Protoss_Pylon started.",
            "frame": 1105,
        }, profile="stardust")
        started_text = build_starcraft116_reaction_tts_text(
            build_started,
            "내가 파일런을 완성하고 있어!",
        )
        self.assertEqual("내가 파일런을 짓고 있어!", started_text)

        assimilator = build_starcraft116_game_event({
            "event_type": "building_completed",
            "summary": "Protoss_Assimilator completed.",
            "unit": {"type": "Protoss_Assimilator", "owner": "self"},
        }, profile="stardust")
        assimilator_text = build_starcraft116_reaction_tts_text(
            assimilator,
            "아시밀레이터 완공됐어!",
        )
        self.assertEqual("어시밀레이터 완성됐어!", assimilator_text)

        barracks = build_starcraft116_game_event({
            "event_type": "combat_started",
            "summary": "Combat started: Protoss_Dragoon vs enemy Terran_Barracks.",
            "friendly_unit": {"type": "Protoss_Dragoon", "owner": "self"},
            "enemy_unit": {"type": "Terran_Barracks", "owner": "enemy"},
        }, profile="stardust")
        barracks_message = build_starcraft116_reaction_user_message(barracks)
        self.assertIn("드라군", barracks_message)
        self.assertIn("배럭", barracks_message)
        barracks_text = build_starcraft116_reaction_tts_text(
            barracks,
            "드라군이 바락스와 싸우고 있어!",
        )
        self.assertEqual("내 드라군이 배럭을 박살내고 있어.", barracks_text)
        self.assertNotIn("바락스", barracks_text)

        scv_destroyed = build_starcraft116_game_event({
            "event_type": "unit_destroyed",
            "summary": "Enemy Terran_SCV was destroyed.",
            "unit": {"type": "Terran_SCV", "owner": "enemy"},
        }, profile="stardust")
        no_zergling = build_starcraft116_reaction_tts_text(
            scv_destroyed,
            "적의 저글링을 죽이고 있어!",
        )
        self.assertEqual("적 SCV 하나 잡았어.", no_zergling)

    def test_plugin_polls_game_events_and_emits_reaction_event(self):
        from plugins.StarCraft116.starcraft116 import StarCraft116
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_game_events import (
            StarCraft116GameEventTailer,
        )

        plugin_root = self.make_plugin_root()
        event_path = plugin_root / "game_events.jsonl"
        event_path.write_text(
            json.dumps({
                "event_type": "enemy_spotted",
                "summary": "Enemy Zergling spotted near natural.",
                "frame": 3270,
            })
            + "\n",
            encoding="utf-8",
        )
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "stardust",
                "game_events_enabled": True,
                "game_events_path": str(event_path),
                "game_events_reaction_cooldown_sec": 0,
            },
        )
        config = StarCraft116Config(str(plugin_root))
        plugin = StarCraft116.__new__(StarCraft116)
        plugin.config_manager = config
        plugin.status_event_callback = mock.Mock()
        plugin.game_event_tailer = StarCraft116GameEventTailer(start_at_end=False)
        plugin._game_event_key_times = {}
        plugin._last_game_event_emit_time = 0.0

        emitted = plugin._poll_game_events()

        self.assertEqual(1, emitted)
        plugin.status_event_callback.assert_called_once()
        event = plugin.status_event_callback.call_args[0][0]
        self.assertEqual("game_event", event["source"])
        self.assertEqual("enemy_spotted", event["event_type"])
        self.assertEqual("Enemy Zergling spotted near natural.", event["summary"])

    def test_plugin_logs_monster_console_events_without_tts_by_default(self):
        from plugins.StarCraft116.starcraft116 import StarCraft116
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_game_events import (
            StarCraft116GameEventTailer,
        )
        from plugins.StarCraft116.starcraft116_core.starcraft116_monster_log_events import (
            StarCraft116MonsterLogTailer,
        )

        plugin_root = self.make_plugin_root()
        event_path = plugin_root / "game_events.jsonl"
        event_path.write_text("", encoding="utf-8")
        log_path = plugin_root / "monster_log.txt"
        log_path.write_text("Joined a game.\n", encoding="utf-8")
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "monster",
                "game_events_enabled": True,
                "game_events_path": str(event_path),
                "game_events_reaction_cooldown_sec": 0,
                "monster_log_events_enabled": True,
                "monster_log_tts_enabled": False,
                "monster_log_path": str(log_path),
                "profiles": {
                    "monster": {
                        "display_name": "Monster",
                    },
                    "stardust": {
                        "display_name": "Stardust",
                    },
                },
            },
        )
        config = StarCraft116Config(str(plugin_root))
        plugin = StarCraft116.__new__(StarCraft116)
        plugin.config_manager = config
        plugin.status_event_callback = mock.Mock()
        plugin.game_event_tailer = StarCraft116GameEventTailer(start_at_end=False)
        plugin.monster_log_tailer = StarCraft116MonsterLogTailer(start_at_end=False)
        plugin._game_event_key_times = {}
        plugin._last_game_event_emit_time = 0.0

        emitted = plugin._poll_game_events()

        self.assertEqual(0, emitted)
        plugin.status_event_callback.assert_not_called()

        plugin.status_event_callback.reset_mock()
        config.set_active_profile("stardust")
        log_path.write_text("Joined a game.\nConnected to BWAPI.\n", encoding="utf-8")
        plugin.monster_log_tailer.reset(str(log_path))

        emitted = plugin._poll_game_events()

        self.assertEqual(0, emitted)
        plugin.status_event_callback.assert_not_called()

    def test_plugin_emits_only_explicit_monster_lav_event_json(self):
        from plugins.StarCraft116.starcraft116 import StarCraft116
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_game_events import (
            StarCraft116GameEventTailer,
        )
        from plugins.StarCraft116.starcraft116_core.starcraft116_monster_log_events import (
            StarCraft116MonsterLogTailer,
        )

        plugin_root = self.make_plugin_root()
        event_path = plugin_root / "game_events.jsonl"
        event_path.write_text("", encoding="utf-8")
        log_path = plugin_root / "monster_log.txt"
        log_path.write_text(
            "Joined a game.\n"
            + "LAV_EVENT "
            + json.dumps({
                "event_type": "unit_created",
                "summary": "Zerg_Drone created.",
                "unit": {
                    "type": "Zerg_Drone",
                    "owner": "self",
                },
            })
            + "\n",
            encoding="utf-8",
        )
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "monster",
                "game_events_enabled": True,
                "game_events_path": str(event_path),
                "game_events_reaction_cooldown_sec": 0,
                "monster_log_events_enabled": True,
                "monster_log_tts_enabled": False,
                "monster_log_path": str(log_path),
                "profiles": {
                    "monster": {
                        "display_name": "Monster",
                    },
                },
            },
        )
        config = StarCraft116Config(str(plugin_root))
        plugin = StarCraft116.__new__(StarCraft116)
        plugin.config_manager = config
        plugin.status_event_callback = mock.Mock()
        plugin.game_event_tailer = StarCraft116GameEventTailer(start_at_end=False)
        plugin.monster_log_tailer = StarCraft116MonsterLogTailer(start_at_end=False)
        plugin._game_event_key_times = {}
        plugin._last_game_event_emit_time = 0.0

        emitted = plugin._poll_game_events()

        self.assertEqual(1, emitted)
        plugin.status_event_callback.assert_called_once()
        event = plugin.status_event_callback.call_args[0][0]
        self.assertEqual("game_event", event["source"])
        self.assertEqual("monster", event["profile"])
        self.assertEqual("unit_created", event["event_type"])
        self.assertEqual("Zerg_Drone created.", event["summary"])

    def test_plugin_emits_monster_bwapi_proxy_jsonl_event(self):
        from plugins.StarCraft116.starcraft116 import StarCraft116
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_game_events import (
            StarCraft116GameEventTailer,
        )
        from plugins.StarCraft116.starcraft116_core.starcraft116_monster_log_events import (
            StarCraft116MonsterLogTailer,
        )

        plugin_root = self.make_plugin_root()
        event_path = plugin_root / "game_events.jsonl"
        event_path.write_text("", encoding="utf-8")
        monster_log_path = plugin_root / "monster_log.txt"
        monster_log_path.write_text("", encoding="utf-8")
        proxy_path = plugin_root / "bwapi_proxy_events.jsonl"
        proxy_path.write_text(
            json.dumps({
                "schema": "lav_starcraft116_bwapi_proxy_event_v1",
                "source": "BWAPI.dll proxy",
                "event_type": "bwapi_real_loaded",
                "summary": "Original BWAPI_real.dll loaded successfully.",
                "severity": "info",
                "tts_eligible": True,
                "details": {
                    "process_path": "C:\\StarCraft\\StarCraft.exe",
                },
            })
            + "\n",
            encoding="utf-8",
        )
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "monster",
                "game_events_enabled": True,
                "game_events_path": str(event_path),
                "game_events_reaction_cooldown_sec": 0,
                "monster_log_events_enabled": True,
                "monster_log_path": str(monster_log_path),
                "bwapi_proxy_events_enabled": True,
                "bwapi_proxy_events_tts_enabled": True,
                "bwapi_proxy_events_path": str(proxy_path),
                "profiles": {
                    "monster": {
                        "display_name": "Monster",
                    },
                    "stardust": {
                        "display_name": "Stardust",
                    },
                },
            },
        )
        config = StarCraft116Config(str(plugin_root))
        plugin = StarCraft116.__new__(StarCraft116)
        plugin.config_manager = config
        plugin.status_event_callback = mock.Mock()
        plugin.game_event_tailer = StarCraft116GameEventTailer(start_at_end=False)
        plugin.monster_log_tailer = StarCraft116MonsterLogTailer(start_at_end=False)
        plugin.bwapi_proxy_event_tailer = StarCraft116GameEventTailer(
            start_at_end=False,
        )
        plugin._game_event_key_times = {}
        plugin._last_game_event_emit_time = 0.0

        emitted = plugin._poll_game_events()

        self.assertEqual(1, emitted)
        plugin.status_event_callback.assert_called_once()
        event = plugin.status_event_callback.call_args[0][0]
        self.assertEqual("game_event", event["source"])
        self.assertEqual("monster", event["profile"])
        self.assertEqual("bwapi_real_loaded", event["event_type"])

        plugin.status_event_callback.reset_mock()
        config.set_active_profile("stardust")
        with proxy_path.open("a", encoding="utf-8") as file:
            file.write(json.dumps({
                "event_type": "bwapi_proxy_loaded",
                "summary": "BWAPI proxy loaded.",
                "tts_eligible": True,
            }) + "\n")

        emitted = plugin._poll_game_events()

        self.assertEqual(0, emitted)
        plugin.status_event_callback.assert_not_called()

    def test_plugin_applies_global_cooldown_to_bwapi_proxy_unit_movement(self):
        from plugins.StarCraft116.starcraft116 import StarCraft116
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_game_events import (
            StarCraft116GameEventTailer,
        )
        from plugins.StarCraft116.starcraft116_core.starcraft116_monster_log_events import (
            StarCraft116MonsterLogTailer,
        )

        plugin_root = self.make_plugin_root()
        event_path = plugin_root / "game_events.jsonl"
        event_path.write_text("", encoding="utf-8")
        monster_log_path = plugin_root / "monster_log.txt"
        monster_log_path.write_text("", encoding="utf-8")
        proxy_path = plugin_root / "bwapi_proxy_events.jsonl"
        proxy_path.write_text(
            json.dumps({
                "event_type": "unit_moved",
                "summary": "Zerg Drone movement detected.",
                "tts_eligible": True,
                "details": {"unit": {"id": 1, "type": "Zerg Drone"}},
            })
            + "\n"
            + json.dumps({
                "event_type": "unit_moved",
                "summary": "Zerg Zergling movement detected.",
                "tts_eligible": True,
                "details": {"unit": {"id": 2, "type": "Zerg Zergling"}},
            })
            + "\n",
            encoding="utf-8",
        )
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "monster",
                "game_events_enabled": True,
                "game_events_path": str(event_path),
                "game_events_reaction_cooldown_sec": 60,
                "monster_log_events_enabled": True,
                "monster_log_path": str(monster_log_path),
                "bwapi_proxy_events_enabled": True,
                "bwapi_proxy_events_tts_enabled": True,
                "bwapi_proxy_events_path": str(proxy_path),
                "profiles": {
                    "monster": {
                        "display_name": "Monster",
                    },
                },
            },
        )
        config = StarCraft116Config(str(plugin_root))
        plugin = StarCraft116.__new__(StarCraft116)
        plugin.config_manager = config
        plugin.status_event_callback = mock.Mock()
        plugin.game_event_tailer = StarCraft116GameEventTailer(start_at_end=False)
        plugin.monster_log_tailer = StarCraft116MonsterLogTailer(start_at_end=False)
        plugin.bwapi_proxy_event_tailer = StarCraft116GameEventTailer(
            start_at_end=False,
        )
        plugin._game_event_key_times = {}
        plugin._last_game_event_emit_time = 0.0

        emitted = plugin._poll_game_events()

        self.assertEqual(1, emitted)
        plugin.status_event_callback.assert_called_once()
        event = plugin.status_event_callback.call_args[0][0]
        self.assertEqual("unit_moved", event["event_type"])

    def test_plugin_game_event_global_cooldown_limits_bursts(self):
        from plugins.StarCraft116.starcraft116 import StarCraft116
        from plugins.StarCraft116.starcraft116_core.starcraft116_config import StarCraft116Config
        from plugins.StarCraft116.starcraft116_core.starcraft116_game_events import (
            StarCraft116GameEventTailer,
        )

        plugin_root = self.make_plugin_root()
        event_path = plugin_root / "game_events.jsonl"
        event_path.write_text(
            json.dumps({"event_type": "worker_created", "summary": "Probe made"})
            + "\n"
            + json.dumps({"event_type": "build_started", "summary": "Pylon started"})
            + "\n",
            encoding="utf-8",
        )
        self.write_config(
            plugin_root,
            {
                "enabled": True,
                "active_profile": "stardust",
                "game_events_enabled": True,
                "game_events_path": str(event_path),
                "game_events_reaction_cooldown_sec": 60,
            },
        )
        config = StarCraft116Config(str(plugin_root))
        plugin = StarCraft116.__new__(StarCraft116)
        plugin.config_manager = config
        plugin.status_event_callback = mock.Mock()
        plugin.game_event_tailer = StarCraft116GameEventTailer(start_at_end=False)
        plugin._game_event_key_times = {}
        plugin._last_game_event_emit_time = 0.0

        emitted = plugin._poll_game_events()

        self.assertEqual(1, emitted)
        plugin.status_event_callback.assert_called_once()

    def test_game_event_gate_reports_duplicate_and_global_cooldown(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_event_gate import (
            decide_game_event_emit,
        )
        from plugins.StarCraft116.starcraft116_core.starcraft116_reaction_policy import (
            build_starcraft116_game_event_key,
        )

        class Config:
            def get_float(self, key, default):
                return 30

        event = {
            "source": "game_event",
            "profile": "monster",
            "event_type": "unit_destroyed",
            "severity": "info",
            "summary": "Zerg_Drone destroyed.",
            "details": {"unit": {"type": "Zerg_Drone", "id": 7}},
        }
        event_key = build_starcraft116_game_event_key(event)

        duplicate = decide_game_event_emit(
            event=event,
            callback=lambda payload: payload,
            config_manager=Config(),
            game_event_key_times={event_key: 100.0},
            last_game_event_emit_time=0.0,
            now=120.0,
        )
        self.assertFalse(duplicate.allowed)
        self.assertEqual("duplicate_cooldown", duplicate.reason)

        global_cooldown = decide_game_event_emit(
            event=event,
            callback=lambda payload: payload,
            config_manager=Config(),
            game_event_key_times={},
            last_game_event_emit_time=100.0,
            now=120.0,
        )
        self.assertFalse(global_cooldown.allowed)
        self.assertEqual("global_cooldown", global_cooldown.reason)

    def test_game_event_gate_marks_emit_without_global_cooldown(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_event_gate import (
            decide_game_event_emit,
            mark_game_event_emitted,
        )

        class Config:
            def get_float(self, key, default):
                return 30

        event = {
            "source": "game_event",
            "profile": "monster",
            "event_type": "bwapi_real_loaded",
            "severity": "info",
            "summary": "BWAPI loaded.",
            "details": {},
        }
        decision = decide_game_event_emit(
            event=event,
            callback=lambda payload: payload,
            config_manager=Config(),
            game_event_key_times={},
            last_game_event_emit_time=10.0,
            now=120.0,
            use_global_cooldown=False,
        )

        key_times, last_emit = mark_game_event_emitted(
            decision,
            {},
            10.0,
            use_global_cooldown=False,
        )

        self.assertTrue(decision.allowed)
        self.assertEqual(1, len(key_times))
        self.assertEqual(10.0, last_emit)

    def test_ui_callbacks_profile_change_preserves_ui_values(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_ui_callbacks import (
            StarCraft116UiCallbacks,
        )

        class Config:
            def __init__(self):
                self.active_profile = "stardust"

            def get_active_profile_name(self):
                return self.active_profile

        class Owner:
            def __init__(self):
                self.config_manager = Config()
                self.last_launch_message = ""
                self.selected = []

            def _select_profile(self, profile_name, reload_config=False):
                self.selected.append((profile_name, reload_config))
                self.config_manager.active_profile = profile_name

            def _ui_values(self, **kwargs):
                return ("ui", self.last_launch_message, kwargs)

        owner = Owner()

        result = StarCraft116UiCallbacks(owner).on_profile_change("monster")

        self.assertEqual([("monster", False)], owner.selected)
        self.assertEqual(
            "Selected StarCraft 1.16 profile: monster",
            owner.last_launch_message,
        )
        self.assertEqual(("ui", owner.last_launch_message, {}), result)

    def test_ui_callbacks_open_management_delegates_target(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_ui_callbacks import (
            StarCraft116UiCallbacks,
        )

        class Owner:
            def __init__(self):
                self.last_launch_message = ""
                self.selected = []
                self.opened = []

            def _select_profile(self, profile_name, reload_config=False):
                self.selected.append((profile_name, reload_config))

            def _open_management_target(self, target_key, target_type):
                self.opened.append((target_key, target_type))
                return f"opened {target_key}"

            def _ui_values(self, **kwargs):
                return ("ui", self.last_launch_message)

        owner = Owner()

        result = StarCraft116UiCallbacks(owner).on_open_management_click(
            "monster",
            "bwapi_ini",
            "file",
        )

        self.assertEqual([("monster", False)], owner.selected)
        self.assertEqual([("bwapi_ini", "file")], owner.opened)
        self.assertEqual("opened bwapi_ini", owner.last_launch_message)
        self.assertEqual(("ui", "opened bwapi_ini"), result)

    def test_ui_callbacks_clear_tracking_preserves_state_reset(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_ui_callbacks import (
            StarCraft116UiCallbacks,
        )

        class State:
            def __init__(self):
                self.processes = ["old"]
                self.running = True
                self.last_message = "old"

        class Owner:
            def __init__(self):
                self.process_entries = ["old"]
                self.state = State()
                self.last_launch_message = ""
                self.write_count = 0

            def _select_profile(self, profile_name, reload_config=False):
                self.selected = (profile_name, reload_config)

            def _write_state_log(self):
                self.write_count += 1

            def _ui_values(self, **kwargs):
                return ("ui", self.last_launch_message)

        owner = Owner()

        result = StarCraft116UiCallbacks(owner).on_clear_tracking_click("monster")

        self.assertEqual(("monster", False), owner.selected)
        self.assertEqual([], owner.process_entries)
        self.assertEqual([], owner.state.processes)
        self.assertFalse(owner.state.running)
        self.assertEqual(1, owner.write_count)
        self.assertIn("Cleared LAV-owned", owner.last_launch_message)
        self.assertEqual(("ui", owner.last_launch_message), result)

    def test_start_respects_auto_launch_for_startup_only(self):
        from plugins.StarCraft116.starcraft116 import StarCraft116

        class Config:
            def get_active_profile_name(self):
                return "monster"

            def get_bool(self, key, default=False):
                if key == "enabled":
                    return True
                if key == "auto_launch":
                    return False
                return default

        class State:
            def __init__(self):
                self.failures = []

            def mark_launch_failed(self, *args):
                self.failures.append(args)

            def mark_launched(self, *args):
                raise AssertionError("startup launch should not happen")

        class Result:
            ok = False
            message = "should not be used"
            processes = []
            commands = []

        class Launcher:
            def __init__(self):
                self.calls = []

            def launch(self, profile_name):
                self.calls.append(profile_name)
                return Result()

            def build_command_display(self, command):
                return str(command)

        plugin = StarCraft116.__new__(StarCraft116)
        plugin.config_manager = Config()
        plugin.state = State()
        plugin.launcher = Launcher()
        plugin.last_launch_message = ""
        plugin.process_entries = []
        plugin._sync_exporter_config = lambda profile_name: (
            True,
            "",
        )

        with mock.patch("plugins.StarCraft116.starcraft116.log_print") as log_mock:
            result = plugin.start(profile_name="monster", launch_source="startup")

        self.assertFalse(result)
        self.assertEqual([], plugin.launcher.calls)
        log_mock.assert_any_call("[StarCraft116] [startup] start skipped: auto_launch=false")

    def test_manual_start_bypasses_auto_launch_disabled(self):
        from plugins.StarCraft116.starcraft116 import StarCraft116

        class Config:
            def get_active_profile_name(self):
                return "monster"

            def get_bool(self, key, default=False):
                if key == "enabled":
                    return True
                if key == "auto_launch":
                    return False
                return default

        class State:
            def __init__(self):
                self.marked = []

            def mark_launch_failed(self, *args):
                raise AssertionError("manual launch should not fail")

            def mark_launched(self, profile, result, command_displays):
                self.marked.append((profile, command_displays))

        class Command:
            def __init__(self, command):
                self.command = command

        class ProcessEntry:
            def __init__(self, label):
                self.label = label
                self.process = object()
                self.command = ["echo"]

        class Result:
            def __init__(self):
                self.ok = True
                self.message = "StarCraft 1.16 profile launched."
                self.processes = [ProcessEntry("test")]
                self.commands = [Command(["echo"])]

        class Launcher:
            def __init__(self):
                self.calls = []

            def launch(self, profile_name):
                self.calls.append(profile_name)
                return Result()

            def build_command_display(self, command):
                return " ".join(command)

        plugin = StarCraft116.__new__(StarCraft116)
        plugin.config_manager = Config()
        plugin.state = State()
        plugin.launcher = Launcher()
        plugin.last_launch_message = ""
        plugin.process_entries = []
        plugin._sync_exporter_config = lambda profile_name: (
            True,
            "",
        )

        with mock.patch("plugins.StarCraft116.starcraft116.log_print") as log_mock:
            result = plugin.start(profile_name="monster", launch_source="manual")

        self.assertTrue(result)
        self.assertEqual(["monster"], plugin.launcher.calls)
        self.assertEqual(1, len(plugin.state.marked))
        self.assertIn(
            "[StarCraft116] [manual] StarCraft 1.16 profile launched.",
            "".join(str(call.args[0]) for call in log_mock.call_args_list),
        )

    def test_launch_coordinator_uses_manual_launch_source(self):
        from plugins.StarCraft116.starcraft116_core.starcraft116_launch_coordinator import (
            StarCraft116LaunchCoordinator,
        )

        owner = mock.Mock()
        owner.state = mock.Mock(running=False)
        owner._ui_values = mock.Mock(return_value=("ok",))
        owner._write_state_log = mock.Mock()
        owner.start = mock.Mock(return_value=True)

        coordinator = StarCraft116LaunchCoordinator(owner)
        coordinator.on_launch_click("monster")

        owner.start.assert_called_once_with(
            profile_name="monster",
            launch_source="manual",
        )
        owner._write_state_log.assert_called_once()
        owner._ui_values.assert_called_once_with(
            emit_status_event=True,
            event_source="launch",
        )

    def test_event_poller_combines_game_monster_and_bwapi_events_without_temp_files(self):
        from types import SimpleNamespace

        from plugins.StarCraft116.starcraft116_core.starcraft116_event_poller import (
            StarCraft116EventPoller,
        )

        class Config:
            def get_bool(self, key, default=False):
                values = {
                    "openai_reactions_enabled": True,
                    "game_events_enabled": True,
                    "monster_log_events_enabled": True,
                    "bwapi_proxy_events_enabled": True,
                    "bwapi_proxy_events_tts_enabled": True,
                }
                return values.get(key, default)

            def get_int(self, key, default):
                return default

            def get_active_profile_name(self):
                return "monster"

            def resolve_game_events_path(self):
                return "game_events.jsonl"

            def resolve_monster_log_path(self):
                return "monster_log.txt"

            def resolve_bwapi_proxy_events_path(self):
                return "bwapi_proxy_events.jsonl"

        class Tailer:
            def __init__(self, events):
                self.events = events
                self.calls = []

            def read_new_events(self, path, max_events):
                self.calls.append((path, max_events))
                return SimpleNamespace(events=list(self.events), errors=[])

        class Owner:
            def __init__(self):
                self.config_manager = Config()
                self.game_event_tailer = Tailer([
                    {
                        "event_type": "enemy_spotted",
                        "summary": "Enemy Zergling spotted.",
                    }
                ])
                self.monster_log_tailer = Tailer([
                    {
                        "event_type": "unit_created",
                        "summary": "Zerg_Drone created.",
                        "tts_eligible": True,
                    }
                ])
                self.bwapi_proxy_event_tailer = Tailer([
                    {
                        "event_type": "bwapi_real_loaded",
                        "summary": "BWAPI_real loaded.",
                        "tts_eligible": True,
                    }
                ])
                self.emitted = []

            def _is_monster_log_events_active(self):
                return True

            def _is_bwapi_proxy_events_active(self):
                return True

            def _is_noisy_unknown_enemy_destroyed_event(self, raw_event):
                return False

            def _maybe_emit_game_event(self, event, use_global_cooldown=True):
                self.emitted.append((event, use_global_cooldown))
                return True

        owner = Owner()

        emitted = StarCraft116EventPoller(owner).poll_game_events()

        self.assertEqual(3, emitted)
        self.assertEqual(
            ["enemy_spotted", "unit_created", "bwapi_real_loaded"],
            [event["event_type"] for event, _cooldown in owner.emitted],
        )
        self.assertEqual(
            [True, True, False],
            [cooldown for _event, cooldown in owner.emitted],
        )
        self.assertEqual([("game_events.jsonl", 6)], owner.game_event_tailer.calls)
        self.assertEqual([("monster_log.txt", 6)], owner.monster_log_tailer.calls)
        self.assertEqual(
            [("bwapi_proxy_events.jsonl", 6)],
            owner.bwapi_proxy_event_tailer.calls,
        )

    def test_bwapi_proxy_event_logging_is_sampled_without_blocking_emits(self):
        from types import SimpleNamespace

        from plugins.StarCraft116.starcraft116_core.starcraft116_event_poller import (
            StarCraft116EventPoller,
        )

        class Config:
            def get_bool(self, key, default=False):
                values = {
                    "bwapi_proxy_events_tts_enabled": True,
                }
                return values.get(key, default)

            def get_int(self, key, default):
                values = {
                    "game_events_max_events_per_poll": 30,
                    "bwapi_proxy_events_log_sample_rate": 25,
                }
                return values.get(key, default)

            def get_active_profile_name(self):
                return "monster"

            def resolve_bwapi_proxy_events_path(self):
                return "bwapi_proxy_events.jsonl"

        class Tailer:
            def read_new_events(self, path, max_events):
                return SimpleNamespace(
                    events=[
                        {
                            "event_type": "unit_created",
                            "summary": f"unit created {index}",
                            "tts_eligible": True,
                        }
                        for index in range(30)
                    ],
                    errors=[],
                )

        class Owner:
            def __init__(self):
                self.config_manager = Config()
                self.bwapi_proxy_event_tailer = Tailer()
                self.emitted = []

            def _is_bwapi_proxy_events_active(self):
                return True

            def _is_noisy_unknown_enemy_destroyed_event(self, raw_event):
                return False

            def _maybe_emit_game_event(self, event, use_global_cooldown=True):
                self.emitted.append(event)
                return True

        owner = Owner()

        with mock.patch(
            "plugins.StarCraft116.starcraft116_core.starcraft116_event_poller.log_print"
        ) as log_mock:
            emitted = StarCraft116EventPoller(owner).poll_bwapi_proxy_events()

        event_logs = [
            call.args[0]
            for call in log_mock.call_args_list
            if "[StarCraft116BWAPIProxyEvents] event:" in str(call.args[0])
        ]
        self.assertEqual(30, emitted)
        self.assertEqual(30, len(owner.emitted))
        self.assertEqual(2, len(event_logs))
