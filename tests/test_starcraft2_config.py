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
        modules = json.loads((PROJECT_ROOT / "modules.json").read_text(encoding="utf-8"))

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
        self.assertTrue(example["runtime_download"]["enabled"])
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
        self.assertEqual(
            os.path.normcase(expected),
            os.path.normcase(config.resolve_path_value(legacy_value)),
        )


if __name__ == "__main__":
    unittest.main()
