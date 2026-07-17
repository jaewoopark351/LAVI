#20260707_kpopmodder: Added StarCraft2 config regression tests for disabled defaults and SC2PATH fallback.
import json
import os
import unittest
from pathlib import Path
from unittest import mock

from plugins.StarCraft2.starcraft2_core.starcraft2_config import StarCraft2Config


PROJECT_ROOT = Path(__file__).resolve().parents[1]


class StarCraft2ConfigTests(unittest.TestCase):
    def test_modules_json_declares_starcraft2_toggle(self):
        modules = json.loads(
            (PROJECT_ROOT / "config" / "modules.example.json").read_text(encoding="utf-8")
        )

        self.assertIn("StarCraft2", modules)
        self.assertIsInstance(modules.get("StarCraft2"), bool)

    def test_example_config_loads_and_is_disabled(self):
        config = StarCraft2Config(str(PROJECT_ROOT / "plugins" / "StarCraft2"))
        example = config.load_example_config()

        self.assertFalse(example["enabled"])
        self.assertEqual("internal_lav_bot", example["engine"])
        self.assertEqual("AbyssalReefLE", example["map_name"])
        self.assertIn("external_exe", example)
        self.assertIn("ares_sc2", example)
        self.assertIn("micromachine", example)
        self.assertIn("external_jar", example)
        self.assertIn("human_vs_bot", example)
        self.assertIn("runtime_download", example)
        self.assertFalse(example["runtime_download"]["enabled"])
        self.assertEqual(
            "jaewoopark96/plugins_StarCraft2_runtime",
            example["runtime_download"]["repo_id"],
        )
#         #20260712_kpopmodder: LAN Lobby config assertions are commented out
#         # with the archived LAN Lobby code path.
#         self.assertIn("lan_lobby", example)
#         self.assertFalse(example["lan_lobby"]["enabled"])
        self.assertIn("ladder_proxy", example)
        self.assertFalse(example["ladder_proxy"]["enabled"])

    def test_sc2path_fallback_when_config_path_is_blank(self):
        config = StarCraft2Config(str(PROJECT_ROOT / "plugins" / "StarCraft2"))
        config.set_runtime_value("starcraft2_path", "")

        with mock.patch.dict(os.environ, {"SC2PATH": "D:\\Games\\StarCraft II"}):
            self.assertEqual(
                os.path.normpath("D:\\Games\\StarCraft II"),
                config.resolve_starcraft2_path(),
            )

    def test_relative_path_is_resolved_against_project_root(self):
        config = StarCraft2Config(str(PROJECT_ROOT / "plugins" / "StarCraft2"))
        expected = os.path.normpath(
            PROJECT_ROOT / "plugins" / "StarCraft2" / "runtime"
        )
        self.assertEqual(
            expected,
            config.resolve_path_value("plugins\\StarCraft2\\runtime"),
        )

    def test_legacy_repo_root_is_migrated_to_current_root(self):
        config = StarCraft2Config(str(PROJECT_ROOT / "plugins" / "StarCraft2"))
        legacy_value = r"C:\Vtuber_Souorce_Code\LAV_v0.2\plugins\StarCraft2\native\Sc2LadderServer\bin\LavHumanVsBot.exe"
        expected = os.path.normpath(
            PROJECT_ROOT
            / "plugins"
            / "StarCraft2"
            / "native"
            / "Sc2LadderServer"
            / "bin"
            / "LavHumanVsBot.exe"
        )
        with mock.patch.dict(
            os.environ,
            {"LAVI_LEGACY_PROJECT_ROOTS": r"C:\Vtuber_Souorce_Code\LAV_v0.2"},
        ):
            self.assertEqual(
                os.path.normcase(expected),
                os.path.normcase(config.resolve_path_value(legacy_value)),
            )

    def test_runtime_paths_refresh_stale_sc2_base_after_update(self):
        with mock.patch.object(
            StarCraft2Config,
            "_migrate_legacy_config_if_missing",
            lambda self: None,
        ):
            config = StarCraft2Config(
                str(PROJECT_ROOT / "plugins" / "StarCraft2"),
                config_path=str(PROJECT_ROOT / "missing_starcraft2_config.json"),
            )
        install_path = os.path.normpath(r"C:\Program Files (x86)\StarCraft II")
        versions_dir = os.path.join(install_path, "Versions")
        stale_base = os.path.join(versions_dir, "Base97425")
        fresh_base = os.path.join(versions_dir, "Base97563")
        stale_exe = os.path.join(stale_base, "SC2_x64.exe")
        fresh_exe = os.path.join(fresh_base, "SC2_x64.exe")
        support64_path = os.path.join(install_path, "Support64")
        config.config.update(
            {
                "starcraft2_install_path": install_path,
                "starcraft2_support64_path": support64_path,
            }
        )
        directories = {install_path, versions_dir, stale_base, fresh_base, support64_path}
        files = {fresh_exe}

        def fake_isdir(path):
            return os.path.normpath(path) in directories

        def fake_isfile(path):
            return os.path.normpath(path) in files

        def fake_listdir(path):
            if os.path.normpath(path) == versions_dir:
                return ["Base97425", "Base97563"]
            return []

        with mock.patch(
            "plugins.StarCraft2.starcraft2_core.starcraft2_config.os.path.isdir",
            side_effect=fake_isdir,
        ), mock.patch(
            "plugins.StarCraft2.starcraft2_core.starcraft2_config.os.path.isfile",
            side_effect=fake_isfile,
        ), mock.patch(
            "plugins.StarCraft2.starcraft2_core.starcraft2_config.os.listdir",
            side_effect=fake_listdir,
        ):
            result = config.resolve_starcraft2_runtime_paths(
                {
                    "starcraft2_exe_path": stale_exe,
                    "starcraft2_base_path": stale_base,
                }
            )

        self.assertEqual(fresh_exe, result["starcraft2_exe_path"])
        self.assertEqual(fresh_base, result["starcraft2_base_path"])
        self.assertEqual(support64_path, result["starcraft2_support64_path"])


if __name__ == "__main__":
    unittest.main()
