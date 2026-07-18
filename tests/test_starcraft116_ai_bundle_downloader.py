#20260718_kpopmodder: Added StarCraft116 AI bundle restore tests without network access.
import json
import shutil
import unittest
import uuid
from pathlib import Path
from unittest import mock

from plugins.StarCraft116.starcraft116_core.starcraft116_ai_bundle_downloader import (
    DEFAULT_STARCRAFT116_AI_BUNDLE_REPO_ID,
    DEFAULT_STARCRAFT116_AI_BUNDLE_REMOTE_SUBDIR,
    DEFAULT_STARCRAFT116_AI_REQUIRED_FILES,
    DEFAULT_STARCRAFT116_MONSTER_LAUNCHER_MARKER,
    StarCraft116AIBundleDownloader,
)
from plugins.StarCraft116.starcraft116_core.starcraft116_config import (
    StarCraft116Config,
)


PROJECT_ROOT = Path(__file__).resolve().parents[1]
TEST_TMP_ROOT = PROJECT_ROOT / "test" / "test_Isolation" / "tmp"


class StarCraft116AIBundleDownloaderTests(unittest.TestCase):
    def _make_temp_dir(self) -> Path:
        TEST_TMP_ROOT.mkdir(parents=True, exist_ok=True)
        path = TEST_TMP_ROOT / f"starcraft116_ai_bundle_{uuid.uuid4().hex}"
        path.mkdir()
        self.addCleanup(lambda: shutil.rmtree(path, ignore_errors=True))
        return path

    def _write_required_files(self, bundle_dir: Path) -> None:
        for relative_path in DEFAULT_STARCRAFT116_AI_REQUIRED_FILES:
            path = bundle_dir / Path(relative_path)
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("stub", encoding="utf-8")

    def test_downloader_skips_when_ai_bundle_is_ready(self):
        def fail_download(**kwargs):
            raise AssertionError("download should not be called")

        bundle_dir = self._make_temp_dir() / "StarCraft_1_16_Bots"
        self._write_required_files(bundle_dir)

        result = StarCraft116AIBundleDownloader(fail_download).ensure_bundle(
            str(bundle_dir),
        )

        self.assertTrue(result["ok"])
        self.assertFalse(result["downloaded"])
        self.assertEqual("ai_bundle_present", result["skipped"])

    def test_downloader_restores_missing_ai_bundle_from_hf_root(self):
        calls = []

        def fake_download(**kwargs):
            calls.append(kwargs)
            bundle_dir = Path(kwargs["local_dir"])
            self._write_required_files(bundle_dir)
            return str(bundle_dir)

        bundle_dir = self._make_temp_dir() / "StarCraft_1_16_Bots"

        result = StarCraft116AIBundleDownloader(fake_download).ensure_bundle(
            str(bundle_dir),
        )

        self.assertTrue(result["ok"])
        self.assertTrue(result["downloaded"])
        self.assertEqual(DEFAULT_STARCRAFT116_AI_BUNDLE_REPO_ID, calls[0]["repo_id"])
        self.assertEqual(str(bundle_dir), calls[0]["local_dir"])
        self.assertIsNone(calls[0]["allow_patterns"])
        self.assertTrue((bundle_dir / "Monster" / "Monster.exe").is_file())

    def test_downloader_can_restore_missing_ai_bundle_from_hf_subdir_override(self):
        calls = []

        def fake_download(**kwargs):
            calls.append(kwargs)
            bundle_dir = Path(kwargs["local_dir"]) / "StarCraft_1_16_Bots"
            self._write_required_files(bundle_dir)
            return str(bundle_dir)

        bundle_dir = self._make_temp_dir() / "StarCraft_1_16_Bots"

        result = StarCraft116AIBundleDownloader(fake_download).ensure_bundle(
            str(bundle_dir),
            remote_subdir="StarCraft_1_16_Bots",
        )

        self.assertTrue(result["ok"])
        self.assertTrue(result["downloaded"])
        self.assertEqual(str(bundle_dir.parent), calls[0]["local_dir"])
        self.assertEqual(["StarCraft_1_16_Bots/**"], calls[0]["allow_patterns"])
        self.assertTrue((bundle_dir / "Monster" / "Monster.exe").is_file())

    def test_downloader_repairs_legacy_monster_launcher_from_template(self):
        def fail_download(**kwargs):
            raise AssertionError("download should not be called")

        root_dir = self._make_temp_dir()
        bundle_dir = root_dir / "StarCraft_1_16_Bots"
        self._write_required_files(bundle_dir)
        template_path = root_dir / "tools" / "run_monster_robust_log.example.bat"
        template_path.parent.mkdir(parents=True, exist_ok=True)
        template_text = (
            "@echo off\n"
            f"REM {DEFAULT_STARCRAFT116_MONSTER_LAUNCHER_MARKER}\n"
            "set \"BWAPI_DATA_DIR=plugins\\StarCraft116\\BWAPI_APP\"\n"
        )
        template_path.write_text(template_text, encoding="utf-8")
        launcher_path = bundle_dir / "Monster" / "run_monster_robust_log.bat"
        launcher_path.write_text(
            "set \"STAR_DIR=%MONSTER_DIR%\\..\\StarCraft\"\n",
            encoding="utf-8",
        )

        result = StarCraft116AIBundleDownloader(fail_download).ensure_bundle(
            str(bundle_dir),
        )

        self.assertTrue(result["ok"])
        self.assertFalse(result["downloaded"])
        self.assertTrue(result["repair"]["repaired"])
        self.assertEqual(template_text, launcher_path.read_text(encoding="utf-8"))

    def test_downloader_reports_disabled_when_ai_bundle_is_missing(self):
        bundle_dir = self._make_temp_dir() / "StarCraft_1_16_Bots"

        result = StarCraft116AIBundleDownloader().ensure_bundle(
            str(bundle_dir),
            enabled=False,
        )

        self.assertFalse(result["ok"])
        self.assertFalse(result["downloaded"])
        self.assertEqual(
            "starcraft116_ai_bundle_download_disabled",
            result["error"],
        )

    def test_config_validation_repairs_internal_ai_bundle_paths(self):
        temp_dir = self._make_temp_dir()
        plugin_root = temp_dir / "plugins" / "StarCraft116"
        config_dir = plugin_root / "config"
        config_dir.mkdir(parents=True)
        bundle_dir = plugin_root / "StarCraft_1_16_Bots"
        config = {
            "enabled": True,
            "active_profile": "monster",
            "ai_bundle_dir": "plugins/StarCraft116/StarCraft_1_16_Bots",
            "profiles": {
                "monster": {
                    "display_name": "Monster",
                    "start_chaoslauncher": False,
                    "start_starcraft": False,
                    "start_bot_process": True,
                    "bot_binary_path": (
                        "plugins/StarCraft116/StarCraft_1_16_Bots/"
                        "Monster/Monster.exe"
                    ),
                    "bot_process_path": (
                        "plugins/StarCraft116/StarCraft_1_16_Bots/"
                        "Monster/run_monster_robust_log.bat"
                    ),
                    "bot_process_working_dir": (
                        "plugins/StarCraft116/StarCraft_1_16_Bots/Monster"
                    ),
                },
            },
        }
        (config_dir / "starcraft116_config.json").write_text(
            json.dumps(config),
            encoding="utf-8",
        )

        test_case = self

        def fake_ensure(_downloader, bundle_path, **kwargs):
            test_case.assertTrue(str(bundle_path).endswith("StarCraft_1_16_Bots"))
            test_case._write_required_files(bundle_dir)
            return {
                "ok": True,
                "downloaded": True,
                "bundle_dir": str(bundle_dir),
            }

        with mock.patch.object(
            StarCraft116AIBundleDownloader,
            "ensure_bundle",
            autospec=True,
            side_effect=fake_ensure,
        ) as ensure_mock:
            validation = StarCraft116Config(str(plugin_root)).validate_paths()

        self.assertTrue(validation.ok)
        self.assertEqual(1, ensure_mock.call_count)


if __name__ == "__main__":
    unittest.main()
