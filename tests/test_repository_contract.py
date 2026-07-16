#20260716_kpopmodder: Regression tests for install, config, and CI portability contracts.
import json
import os
import subprocess
import unittest
from pathlib import Path
import tempfile
from unittest import mock

import core.config_manager as config_manager_module
from core.paths import LaviPaths


PROJECT_ROOT = Path(__file__).resolve().parents[1]


class RepositoryContractTests(unittest.TestCase):
    def _git_ls_files(self):
        result = subprocess.run(
            ["git", "ls-files"],
            cwd=PROJECT_ROOT,
            check=True,
            capture_output=True,
            text=True,
            encoding="utf-8",
        )
        return [line.strip().replace("\\", "/") for line in result.stdout.splitlines() if line.strip()]

    def test_readme_and_install_scripts_agree_on_python_314(self):
        readme_text = (PROJECT_ROOT / "README.md").read_text(encoding="utf-8")
        readme_en_text = (PROJECT_ROOT / "README_EN.md").read_text(encoding="utf-8")
        install_text = (PROJECT_ROOT / "scripts" / "install_windows.ps1").read_text(encoding="utf-8")
        preflight_text = (PROJECT_ROOT / "scripts" / "preflight.py").read_text(encoding="utf-8")

        self.assertIn("Python 3.14", readme_text)
        self.assertIn("Python 3.14", readme_en_text)
        self.assertIn("Python 3.14", install_text)
        self.assertIn("EXPECTED_PYTHON = (3, 14)", preflight_text)
        self.assertNotRegex(install_text, r"Python\s+3\.10")

    def test_actual_local_config_files_are_not_tracked(self):
        tracked = set(self._git_ls_files())
        local_config_paths = {
            "config/gpu_device_config.json",
            "config/modules.json",
            "config/starcraft2_config.json",
            "plugins/StarCraft2/config_starcraft2.json",
            "plugins/StarCraft2/config/starcraft2_config.json",
            "plugins/Chess/config/chess_config.json",
            "plugins/StarCraft116/config/starcraft116_config.json",
            "plugins/GPTSoVITS/config/gpt_sovits_config.json",
        }

        self.assertFalse(tracked.intersection(local_config_paths))

    def test_zero_byte_root_python_file_is_not_tracked(self):
        self.assertNotIn("python", set(self._git_ls_files()))

    def test_portable_runtime_files_do_not_contain_private_absolute_paths(self):
        tracked = self._git_ls_files()
        prefixes = (
            "app_core/",
            "core/",
            "plugin_system/",
            "scripts/",
            "config/",
            "plugins/StarCraft2/starcraft2_core/",
        )
        suffixes = (".py", ".ps1", ".bat", ".json", ".toml", ".yml", ".yaml")
        forbidden = (
            r"C:\Vtuber_Souorce_Code\LAV_v0.2",
            r"C:\Vtuber_Souorce_Code\StarCraft2\SC2AIApp_2025_S1",
        )
        offenders = []
        for relative_path in tracked:
            if not relative_path.startswith(prefixes) or not relative_path.endswith(suffixes):
                continue
            text = (PROJECT_ROOT / relative_path).read_text(encoding="utf-8", errors="ignore")
            for marker in forbidden:
                if marker in text:
                    offenders.append(f"{relative_path}: {marker}")

        self.assertEqual([], offenders)

    def test_config_example_copy_does_not_overwrite_existing_file(self):
        temp_root = PROJECT_ROOT / "test" / "test_Isolation" / "repo_contract_copy"
        temp_root.mkdir(parents=True, exist_ok=True)
        paths = LaviPaths(temp_root)
        example_path = temp_root / "example.json"
        target_path = temp_root / "target.json"
        example_path.write_text(json.dumps({"value": "example"}), encoding="utf-8")
        target_path.write_text(json.dumps({"value": "local"}), encoding="utf-8")

        copied = paths.copy_example_if_missing(example_path, target_path)

        self.assertFalse(copied)
        self.assertEqual({"value": "local"}, json.loads(target_path.read_text(encoding="utf-8")))

    def test_paths_handle_spaces_and_korean_characters(self):
        temp_root = PROJECT_ROOT / "test" / "test_Isolation" / "공백 포함 LAVI"
        temp_root.mkdir(parents=True, exist_ok=True)
        paths = LaviPaths(temp_root)

        resolved = paths.resolve_path("config/음성 설정.json")
        self.assertEqual(
            temp_root / "config" / "음성 설정.json",
            resolved,
        )

        with mock.patch.dict(os.environ, {"LAVI_CONFIG_DIR": "로컬 설정"}):
            self.assertEqual(
                temp_root / "로컬 설정",
                paths.config_dir,
            )

    def test_config_manager_resolves_relative_paths_from_project_root(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_root = Path(temp_dir)
            with mock.patch.object(
                config_manager_module,
                "PROJECT_ROOT",
                str(temp_root),
            ):
                manager = config_manager_module.ConfigManager(
                    "nested/config.ini",
                )

            self.assertEqual(
                temp_root / "nested" / "config.ini",
                Path(manager.config_file),
            )

    def test_config_manager_default_file_can_be_redirected_by_environment(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_root = Path(temp_dir)
            config_file = temp_root / "isolated.ini"
            with mock.patch.dict(
                os.environ,
                {"LAVI_CONFIG_FILE": str(config_file)},
            ):
                manager = config_manager_module.ConfigManager()

            self.assertEqual(config_file, Path(manager.config_file))

    def test_config_manager_atomic_write_preserves_existing_file_on_replace_failure(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            target_path = Path(temp_dir) / "config.ini"
            target_path.write_text(
                "[LLM]\nspeech_style = polite\n",
                encoding="utf-8",
            )
            manager = config_manager_module.ConfigManager(target_path)

            with mock.patch(
                "core.config_manager.os.replace",
                side_effect=RuntimeError("replace failed"),
            ):
                with self.assertRaises(RuntimeError):
                    manager.save_config("LLM", "speech_style", "casual")

            self.assertEqual(
                "[LLM]\nspeech_style = polite\n",
                target_path.read_text(encoding="utf-8"),
            )


if __name__ == "__main__":
    unittest.main()
