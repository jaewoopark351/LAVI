#20260716_kpopmodder: Regression tests for install, config, and CI portability contracts.
import ast
import json
import os
import re
import subprocess
import unittest
from pathlib import Path
import tempfile
from unittest import mock

import core.config_manager as config_manager_module
from core.paths import LaviPaths


PROJECT_ROOT = Path(__file__).resolve().parents[1]


class RepositoryContractTests(unittest.TestCase):
    PACKAGE_NAME_ALIASES = {
        "pil": "pillow",
        "websocket": "websocket-client",
    }

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

    def _requirement_names(self, relative_path):
        names = set()
        for line in (PROJECT_ROOT / relative_path).read_text(encoding="utf-8").splitlines():
            text = line.strip()
            if not text or text.startswith("#") or text.startswith("--"):
                continue
            name = re.split(r"[<>=!~; ]+", text, maxsplit=1)[0]
            names.add(self._canonical_package_name(name))
        return names

    def _canonical_package_name(self, name):
        normalized = str(name).strip().lower().replace("_", "-")
        return self.PACKAGE_NAME_ALIASES.get(normalized, normalized)

    def _plugin_metadata_from_file(self, path):
        try:
            tree = ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
        except SyntaxError:
            return []
        metadata_items = []
        for node in tree.body:
            if not isinstance(node, ast.ClassDef):
                continue
            for statement in node.body:
                if not isinstance(statement, ast.Assign):
                    continue
                has_metadata_target = any(
                    isinstance(target, ast.Name)
                    and target.id in {"PLUGIN_METADATA", "plugin_metadata"}
                    for target in statement.targets
                )
                if not has_metadata_target:
                    continue
                value = ast.literal_eval(statement.value)
                if isinstance(value, dict):
                    metadata_items.append((node.name, value))
                break
        return metadata_items

    def _required_python_packages_from_metadata(self, metadata):
        availability_probe = metadata.get("availability_probe")
        if not isinstance(availability_probe, dict):
            availability_probe = {}
        value = (
            metadata.get("required_python_packages")
            or availability_probe.get("required_python_packages")
            or ()
        )
        if isinstance(value, str):
            return (value,)
        if isinstance(value, (list, tuple)):
            return tuple(str(item) for item in value if str(item))
        return ()

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

    def test_windows_ci_uses_committed_core_lock_and_smoke_gates(self):
        #20260716_kpopmodder: Keep CI aligned with the P0-C Core collection boundary.
        workflow_text = (
            PROJECT_ROOT / ".github" / "workflows" / "windows-ci.yml"
        ).read_text(encoding="utf-8")

        self.assertEqual(2, workflow_text.count("install_windows.ps1 -Profile Core -Accelerator CPU -Dev"))
        self.assertIn("-m pip check", workflow_text)
        self.assertIn("-m ruff check", workflow_text)
        self.assertIn("tests\\test_app_composer.py", workflow_text)
        self.assertIn("tests\\test_repository_contract.py", workflow_text)
        self.assertIn("tests\\test_smoke_startup.py", workflow_text)
        self.assertIn("tests\\test_plugin_system_imports.py", workflow_text)
        self.assertIn("--profile Core", workflow_text)
        self.assertIn("--modules-config config\\modules.core.json", workflow_text)
        self.assertIn("--production-config-smoke", workflow_text)
        self.assertIn("--production-readiness-smoke", workflow_text)
        self.assertNotIn('pytest -m "not gpu and not integration and not network and not slow"', workflow_text)

    def test_full_cu130_lock_covers_active_plugin_python_dependencies(self):
        #20260718_kpopmodder: Full install lock must cover enabled plugin metadata without using requirements_full.txt.
        active_modules = json.loads(
            (PROJECT_ROOT / "modules.json").read_text(encoding="utf-8")
        )
        locked_packages = self._requirement_names(
            "requirements/locks/windows-py314-full-cu130.txt"
        )
        missing = []

        for plugin_dir in sorted((PROJECT_ROOT / "plugins").iterdir()):
            if not plugin_dir.is_dir() or active_modules.get(plugin_dir.name) is not True:
                continue
            for python_file in sorted(plugin_dir.glob("*.py")):
                if python_file.name.startswith("_"):
                    continue
                for class_name, metadata in self._plugin_metadata_from_file(python_file):
                    for package in self._required_python_packages_from_metadata(metadata):
                        canonical_name = self._canonical_package_name(package)
                        if canonical_name not in locked_packages:
                            missing.append(
                                f"{plugin_dir.name}.{class_name}: {package}"
                            )

        from app_core.optional_module_manifest import OPTIONAL_MODULE_MANIFEST

        for module_name, manifest in OPTIONAL_MODULE_MANIFEST.items():
            if active_modules.get(module_name) is not True:
                continue
            for package in self._required_python_packages_from_metadata(manifest):
                canonical_name = self._canonical_package_name(package)
                if canonical_name not in locked_packages:
                    missing.append(f"{module_name}: {package}")

        self.assertEqual([], missing)

    def test_windows_installer_uses_committed_locks_not_requirements_full(self):
        #20260718_kpopmodder: requirements_full.txt is historical, not the canonical installer lock.
        install_text = (
            PROJECT_ROOT / "scripts" / "install_windows.ps1"
        ).read_text(encoding="utf-8")

        self.assertIn("requirements\\locks\\windows-py314-full-cu130.txt", install_text)
        self.assertIn("requirements\\locks\\windows-py314-core-cpu.txt", install_text)
        self.assertNotIn("requirements_full.txt", install_text)

    def test_windows_installer_exposes_profile_specific_locks(self):
        #20260718_kpopmodder: Optional runtime groups must stay installable without collapsing into Full.
        install_text = (
            PROJECT_ROOT / "scripts" / "install_windows.ps1"
        ).read_text(encoding="utf-8")
        preflight_text = (PROJECT_ROOT / "scripts" / "preflight.py").read_text(encoding="utf-8")

        expected_locks = {
            "Core|CPU": "requirements\\locks\\windows-py314-core-cpu.txt",
            "Voice|cu130": "requirements\\locks\\windows-py314-voice-cu130.txt",
            "Vision|cu130": "requirements\\locks\\windows-py314-vision-cu130.txt",
            "Games|CPU": "requirements\\locks\\windows-py314-games-cpu.txt",
            "Full|cu130": "requirements\\locks\\windows-py314-full-cu130.txt",
        }
        for matrix_key, lock_path in expected_locks.items():
            with self.subTest(matrix_key=matrix_key):
                self.assertIn(f'"{matrix_key}" = "{lock_path}"', install_text)
                self.assertTrue((PROJECT_ROOT / lock_path).exists())

        for profile in ("Core", "Voice", "Vision", "Games", "Full"):
            self.assertIn(f'"{profile}"', install_text)
            self.assertIn(f'"{profile}"', preflight_text)

    def test_preflight_checks_profile_specific_imports(self):
        #20260718_kpopmodder: Installer preflight should verify the selected profile after pip install.
        preflight_text = (PROJECT_ROOT / "scripts" / "preflight.py").read_text(encoding="utf-8")

        self.assertIn("PROFILE_REQUIRED_IMPORTS", preflight_text)
        for module_name in (
            "librosa",
            "resemblyzer",
            "speechbrain",
            "accelerate",
            "PIL",
            "chess",
            "pytchat",
            "twitchio",
            "websocket",
        ):
            with self.subTest(module_name=module_name):
                self.assertIn(f'"{module_name}"', preflight_text)
        self.assertIn("_check_profile_imports(errors, args.profile)", preflight_text)

    def test_profile_locks_cover_grouped_requirement_files(self):
        #20260718_kpopmodder: Profile locks must cover their source dependency groups.
        profile_locks = {
            "voice": "requirements/locks/windows-py314-voice-cu130.txt",
            "vision": "requirements/locks/windows-py314-vision-cu130.txt",
            "games": "requirements/locks/windows-py314-games-cpu.txt",
        }

        missing = []
        for group_name, lock_path in profile_locks.items():
            required = self._requirement_names(f"requirements/{group_name}.txt")
            locked = self._requirement_names(lock_path)
            for package in sorted(required - locked):
                missing.append(f"{group_name}: {package}")

        self.assertEqual([], missing)

    def test_optional_manifest_uses_specific_dependency_groups(self):
        #20260718_kpopmodder: Game and vision optional plugins should not collapse back into Full.
        from app_core.optional_module_manifest import OPTIONAL_MODULE_MANIFEST

        expected_groups = {
            "Chess": "Games",
            "StarCraftRemastered": "Games",
            "StarCraft116": "Games",
            "StarCraft2": "Games",
            "ScreenVision": "Vision",
        }
        actual_groups = {
            module_name: OPTIONAL_MODULE_MANIFEST[module_name]["dependency_group"]
            for module_name in expected_groups
        }

        self.assertEqual(expected_groups, actual_groups)

    def test_provider_metadata_uses_specific_dependency_groups(self):
        #20260718_kpopmodder: Provider diagnostics should suggest the narrow install profile when possible.
        expected_groups = {
            "plugins/VoiceInput/voiceInput.py": {"VoiceInput": "Voice"},
            "plugins/GPTSoVITS/GPTSoVITS.py": {"GPTSoVITS": "Voice"},
            "plugins/Local_EN_to_JA/Local_EN_to_JA.py": {"LocalENToJA": "Voice"},
            "plugins/silero/silero.py": {"Silero": "Voice"},
            "plugins/voicevox/voicevox.py": {"VoiceVox": "Voice"},
            "plugins/YoutubeChatFetch/youtubeChatFetch.py": {"YoutubeChatFetch": "Games"},
            "plugins/TwitchChatFetch/twitchChatFetch.py": {"TwitchChatFetch": "Games"},
        }

        actual_groups = {}
        for relative_path, expected_by_class in expected_groups.items():
            metadata_by_class = dict(self._plugin_metadata_from_file(PROJECT_ROOT / relative_path))
            for class_name in expected_by_class:
                actual_groups.setdefault(relative_path, {})[class_name] = (
                    metadata_by_class[class_name]["dependency_group"]
                )

        self.assertEqual(expected_groups, actual_groups)

    def test_profile_locks_cover_plugin_metadata_dependency_groups(self):
        #20260718_kpopmodder: Narrow profile suggestions must point at locks that contain the plugin deps.
        from app_core.optional_module_manifest import OPTIONAL_MODULE_MANIFEST

        lock_by_group = {
            "Voice": "requirements/locks/windows-py314-voice-cu130.txt",
            "Vision": "requirements/locks/windows-py314-vision-cu130.txt",
            "Games": "requirements/locks/windows-py314-games-cpu.txt",
        }
        packages_by_group = {
            group_name: self._requirement_names(lock_path)
            for group_name, lock_path in lock_by_group.items()
        }
        missing = []

        for relative_path in self._git_ls_files():
            if not relative_path.startswith("plugins/") or not relative_path.endswith(".py"):
                continue
            for class_name, metadata in self._plugin_metadata_from_file(
                PROJECT_ROOT / relative_path,
            ):
                group_name = metadata.get("dependency_group")
                if group_name not in lock_by_group:
                    continue
                for package in self._required_python_packages_from_metadata(metadata):
                    canonical_name = self._canonical_package_name(package)
                    if canonical_name not in packages_by_group[group_name]:
                        missing.append(f"{relative_path}:{class_name}: {package}")

        for module_name, manifest in OPTIONAL_MODULE_MANIFEST.items():
            group_name = manifest.get("dependency_group")
            if group_name not in lock_by_group:
                continue
            for package in self._required_python_packages_from_metadata(manifest):
                canonical_name = self._canonical_package_name(package)
                if canonical_name not in packages_by_group[group_name]:
                    missing.append(f"{module_name}: {package}")

        self.assertEqual([], missing)

    def test_root_modules_json_is_tracked_for_deployment_artifacts(self):
        #20260716_kpopmodder: Release/archive jobs must include the production default config.
        tracked = set(self._git_ls_files())

        self.assertIn("modules.json", tracked)

    def test_shareable_config_files_are_tracked_for_deployment_artifacts(self):
        tracked = set(self._git_ls_files())
        required_config_paths = {
            "config/audio_device_config.example.json",
            "config/audio_device_config.json",
            "config/gpu_device_config.json",
            "config/gpu_device_config.example.json",
            "config/modules.json",
            "config/modules.core.json",
            "config/modules.example.json",
            "config/chess_config.json",
            "config/chess_config.example.json",
            "config/starcraft2_config.json",
            "config/starcraft2_config.example.json",
            "config/voice_input_config.example.json",
        }

        self.assertEqual([], sorted(required_config_paths - tracked))

    def test_legacy_plugin_local_config_files_are_not_tracked(self):
        tracked = set(self._git_ls_files())
        local_config_paths = {
            "plugins/Chess/config/chess_config.json",
            "plugins/StarCraft2/config_starcraft2.json",
            "plugins/StarCraft2/config/starcraft2_config.json",
        }

        self.assertFalse(tracked.intersection(local_config_paths))

    def test_zero_byte_root_python_file_is_not_tracked(self):
        self.assertNotIn("python", set(self._git_ls_files()))

    def test_production_python_files_have_one_top_level_class(self):
        #20260718_kpopmodder: Keep production modules aligned with AGENTS one-class-per-file rule.
        offenders = []
        for relative_path in self._git_ls_files():
            if not relative_path.endswith(".py") or relative_path.startswith("tests/"):
                continue
            path = PROJECT_ROOT / relative_path
            try:
                tree = ast.parse(path.read_text(encoding="utf-8-sig"), filename=relative_path)
            except SyntaxError as exc:
                self.fail(f"{relative_path} cannot be parsed: {exc}")

            class_names = [
                node.name
                for node in tree.body
                if isinstance(node, ast.ClassDef)
            ]
            if len(class_names) > 1:
                offenders.append(f"{relative_path}: {', '.join(class_names)}")

        self.assertEqual([], offenders)

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
