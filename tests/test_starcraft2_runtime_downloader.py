#20260712_kpopmodder: Added tests for StarCraft2 runtime restore without network access.
import shutil
import unittest
import uuid
import zipfile
from pathlib import Path
from unittest import mock

from plugins.StarCraft2.starcraft2 import StarCraft2
from plugins.StarCraft2.starcraft2_core.starcraft2_runtime_downloader import (
    DEFAULT_RUNTIME_REPO_ID,
    StarCraft2RuntimeDownloader,
)


PROJECT_ROOT = Path(__file__).resolve().parents[1]
TEST_TMP_ROOT = PROJECT_ROOT / ".test_tmp"


class StarCraft2RuntimeDownloaderTests(unittest.TestCase):
    def _make_temp_dir(self) -> Path:
        TEST_TMP_ROOT.mkdir(exist_ok=True)
        path = TEST_TMP_ROOT / f"starcraft2_runtime_{uuid.uuid4().hex}"
        path.mkdir()
        self.addCleanup(lambda: shutil.rmtree(path, ignore_errors=True))
        return path

    def _write_minimum_runtime(self, runtime_dir: Path) -> None:
        required_files = [
            "HumanLadder.json",
            "PlayerIds",
            "Bots/BenBotBC/BenBotBC.jar",
            "Bots/changeling/changeling.exe",
            "Bots/changeling/config.yml",
            "Bots/changeling/ladderbots.json",
            "Bots/sharkbot/sharkbot.exe",
            "Bots/sharkbot/ladderbots.json",
            "jre/bin/java.exe",
        ]
        for relative_path in required_files:
            path = runtime_dir / Path(relative_path)
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("stub", encoding="utf-8")

    def _write_minimum_runtime_archive(self, archive_path: Path) -> None:
        required_files = [
            "HumanLadder.json",
            "PlayerIds",
            "Bots/BenBotBC/BenBotBC.jar",
            "Bots/changeling/changeling.exe",
            "Bots/changeling/config.yml",
            "Bots/changeling/ladderbots.json",
            "Bots/sharkbot/sharkbot.exe",
            "Bots/sharkbot/ladderbots.json",
            "jre/bin/java.exe",
        ]
        with zipfile.ZipFile(archive_path, "w") as archive:
            for relative_path in required_files:
                archive.writestr(relative_path, "stub")

    def test_downloader_restores_empty_runtime_folder(self):
        calls = []

        def fake_download(**kwargs):
            calls.append(kwargs)
            runtime_dir = Path(kwargs["local_dir"])
            self._write_minimum_runtime(runtime_dir)
            return str(runtime_dir)

        temp_dir = self._make_temp_dir()
        runtime_dir = temp_dir / "runtime"
        runtime_dir.mkdir()
        (runtime_dir / ".gitkeep").write_text("", encoding="utf-8")

        result = StarCraft2RuntimeDownloader(fake_download).ensure_runtime(
            str(runtime_dir)
        )

        self.assertTrue(result["ok"])
        self.assertTrue(result["downloaded"])
        self.assertTrue(result["validation"]["ok"])
        self.assertEqual(DEFAULT_RUNTIME_REPO_ID, calls[0]["repo_id"])

    def test_downloader_skips_when_runtime_is_ready(self):
        def fail_download(**kwargs):
            raise AssertionError("download should not be called")

        temp_dir = self._make_temp_dir()
        runtime_dir = temp_dir / "runtime"
        self._write_minimum_runtime(runtime_dir)

        result = StarCraft2RuntimeDownloader(fail_download).ensure_runtime(
            str(runtime_dir)
        )

        self.assertTrue(result["ok"])
        self.assertFalse(result["downloaded"])
        self.assertEqual("runtime_present", result["skipped"])
        self.assertTrue(result["validation"]["ok"])

    def test_downloader_repairs_partial_runtime_folder(self):
        calls = []

        def fake_download(**kwargs):
            calls.append(kwargs)
            runtime_dir = Path(kwargs["local_dir"])
            self._write_minimum_runtime(runtime_dir)
            return str(runtime_dir)

        temp_dir = self._make_temp_dir()
        runtime_dir = temp_dir / "runtime"
        (runtime_dir / "Bots").mkdir(parents=True)

        result = StarCraft2RuntimeDownloader(fake_download).ensure_runtime(
            str(runtime_dir)
        )

        self.assertTrue(result["ok"])
        self.assertTrue(result["downloaded"])
        self.assertEqual(1, len(calls))
        self.assertTrue(result["validation"]["ok"])

    def test_downloader_restores_from_local_archive_before_network(self):
        def fail_download(**kwargs):
            raise AssertionError("network download should not be called")

        temp_dir = self._make_temp_dir()
        runtime_dir = temp_dir / "runtime"
        archive_path = temp_dir / "runtime.Zip"
        self._write_minimum_runtime_archive(archive_path)

        result = StarCraft2RuntimeDownloader(fail_download).ensure_runtime(
            str(runtime_dir),
            local_archive_path=str(archive_path),
        )

        self.assertTrue(result["ok"])
        self.assertTrue(result["downloaded"])
        self.assertEqual("local_archive", result["source"])
        self.assertTrue((runtime_dir / "Bots" / "changeling" / "changeling.exe").is_file())
        self.assertTrue(result["validation"]["ok"])

    def test_downloader_fails_when_download_is_incomplete(self):
        def fake_download(**kwargs):
            runtime_dir = Path(kwargs["local_dir"])
            (runtime_dir / "Bots").mkdir(parents=True, exist_ok=True)
            (runtime_dir / "HumanLadder.json").write_text("{}", encoding="utf-8")
            return str(runtime_dir)

        temp_dir = self._make_temp_dir()
        runtime_dir = temp_dir / "runtime"

        result = StarCraft2RuntimeDownloader(fake_download).ensure_runtime(
            str(runtime_dir)
        )

        self.assertFalse(result["ok"])
        self.assertTrue(result["downloaded"])
        self.assertEqual("starcraft2_runtime_incomplete", result["error"])
        self.assertIn(
            str(Path("Bots") / "changeling" / "changeling.exe"),
            result["validation"]["missing_files"],
        )

    def test_local_match_downloads_repo_runtime_before_bot_validation(self):
        calls = []

        def fake_download(**kwargs):
            calls.append(kwargs)
            runtime_dir = Path(kwargs["local_dir"])
            self._write_minimum_runtime(runtime_dir)
            return str(runtime_dir)

        temp_dir = self._make_temp_dir()
        plugin_root = temp_dir / "plugins" / "StarCraft2"
        runtime_dir = plugin_root / "runtime"
        exe_path = plugin_root / "native" / "Sc2LadderServer" / "bin" / "LavHumanVsBot.exe"
        exe_path.parent.mkdir(parents=True)
        exe_path.write_text("stub", encoding="utf-8")
        runtime_dir.mkdir(parents=True)

        facade = StarCraft2()
        facade.plugin_root = str(plugin_root)
        facade._match_config_service.plugin_root = str(plugin_root)
        facade.runtime_downloader = StarCraft2RuntimeDownloader(fake_download)
        facade._match_config_service.runtime_downloader = facade.runtime_downloader
        facade.config_manager.config["runtime_download"] = {
            "enabled": True,
            "repo_id": DEFAULT_RUNTIME_REPO_ID,
            "repo_type": "model",
            "revision": "main",
        }
        facade.config_manager.config["local_match"] = {
            "enabled": False,
            "executable_path": str(exe_path),
            "working_directory": str(runtime_dir),
            "args": ["--bot", "changeling", "--race", "Protoss"],
            "ports": [5677, 5678],
            "capture_output": True,
        }

        with mock.patch.object(
            facade.ladder_proxy,
            "start",
            return_value={"ok": True, "running": True},
        ) as start:
            facade.on_local_human_vs_changeling_click(
                str(exe_path),
                str(runtime_dir),
                "--bot changeling --race Protoss",
                "5677,5678",
                "Zerg",
            )

        started_config = start.call_args.args[0]

        self.assertEqual(1, len(calls))
        self.assertTrue(started_config.runtime_download["downloaded"])
        self.assertTrue(started_config.bot_profile_validation["ok"])

    def test_local_match_blocks_missing_selected_bot_runtime(self):
        temp_dir = self._make_temp_dir()
        runtime_dir = temp_dir / "external_runtime"
        exe_path = temp_dir / "LavHumanVsBot.exe"
        exe_path.write_text("stub", encoding="utf-8")
        runtime_dir.mkdir(parents=True)

        facade = StarCraft2()

        with mock.patch.object(facade.ladder_proxy, "start") as start:
            result_json = facade.on_local_human_vs_changeling_click(
                str(exe_path),
                str(runtime_dir),
                "--bot changeling --race Protoss",
                "5677,5678",
                "Zerg",
            )

        start.assert_not_called()
        self.assertIn("bot_runtime_missing", result_json)


if __name__ == "__main__":
    unittest.main()
