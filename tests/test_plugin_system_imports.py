import json
import sys
#20260622_kpopmodder: Verify canonical plugin_system imports after removing root modules.
import tempfile
import unittest
from pathlib import Path
from unittest import mock


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


class PluginSystemImportTests(unittest.TestCase):
    def test_plugin_contracts_are_shared_and_serializable(self):#20260716_kpopmodder
        from plugin_system import (
            AvailabilityProbeContract,
            PluginContractIssue,
            PluginDiagnostic,
            PluginDiagnosticSnapshot,
            PluginRuntimeContract,
            PluginState,
            PluginSupports,
        )
        from plugin_system.contracts import (
            PluginDiagnostic as ContractDiagnostic,
            PluginState as ContractState,
        )
        from plugin_system.loader import (
            PluginDiagnostic as LoaderDiagnostic,
            PluginState as LoaderState,
        )

        self.assertIs(PluginState, ContractState)
        self.assertIs(PluginState, LoaderState)
        self.assertIs(PluginDiagnostic, ContractDiagnostic)
        self.assertIs(PluginDiagnostic, LoaderDiagnostic)
        self.assertEqual(
            {
                "code": "example",
                "message": "message",
                "path": "manifest.id",
                "severity": "warning",
            },
            PluginContractIssue(
                code="example",
                message="message",
                path="manifest.id",
                severity="warning",
            ).to_dict(),
        )
        self.assertEqual("FAILED", PluginState.BROKEN)

        contract = PluginRuntimeContract(
            plugin_id="Example",
            manifest={
                "id": "Example",
                "display_name": "Example",
                "api_version": "1",
                "category": "input_gathering",
                "entrypoint": "plugins.Example.Example:Example",
                "dependency_group": "Core",
            },
            config_schema={"type": "object"},
            availability_probe=AvailabilityProbeContract(
                required_services=("microphone_input_device",),
                timeout_sec=0.1,
                log_reference="test probe",
            ),
            capabilities=("input",),
            supports=PluginSupports(offline=True, cpu=True, requires_gpu=False),
        )

        self.assertEqual(
            {
                "plugin_id": "Example",
                "manifest": {
                    "id": "Example",
                    "display_name": "Example",
                    "api_version": "1",
                    "category": "input_gathering",
                    "entrypoint": "plugins.Example.Example:Example",
                    "dependency_group": "Core",
                },
                "config_schema": {"type": "object"},
                "availability_probe": {
                    "required_python_packages": [],
                    "required_files": [],
                    "required_executables": [],
                    "required_services": ["microphone_input_device"],
                    "timeout_sec": 0.1,
                    "log_reference": "test probe",
                },
                "lifecycle_methods": ["init", "start", "stop", "shutdown"],
                "capabilities": ["input"],
                "supports": {
                    "offline": True,
                    "cpu": True,
                    "requires_gpu": False,
                },
            },
            contract.to_dict(),
        )
        self.assertEqual([], list(contract.validation_errors()))

        snapshot = PluginDiagnosticSnapshot(
            plugin_id="Example",
            name="ExamplePlugin",
            category="input_gathering",
            state=PluginState.READY,
            detail="ready",
            diagnostic=PluginDiagnostic(
                plugin_id="Example",
                state=PluginState.READY,
                reason_code="ok",
                human_readable_message="ready",
            ),
            runtime_contract=contract,
        )

        self.assertEqual(
            {
                "plugin_id": "Example",
                "name": "ExamplePlugin",
                "category": "input_gathering",
                "state": PluginState.READY,
                "detail": "ready",
                "diagnostic": {
                    "plugin_id": "Example",
                    "state": PluginState.READY,
                    "reason_code": "ok",
                    "human_readable_message": "ready",
                    "missing_python_packages": [],
                    "missing_files": [],
                    "missing_executables": [],
                    "missing_services": [],
                    "missing_environment_variables": [],
                    "suggested_install_profile": "",
                    "suggested_command": "",
                    "log_reference": "",
                },
                "runtime_contract": contract.to_dict(),
            },
            snapshot.to_dict(),
        )

    def test_plugin_descriptor_exposes_runtime_contract(self):#20260716_kpopmodder
        from plugin_system.loader import PluginDescriptor

        descriptor = PluginDescriptor(
            plugin_name="Example",
            class_name="ExamplePlugin",
            category="text_to_speech",
            interface_name="TTSPluginInterface",
            module_name="plugins.Example.Example",
            module_path=str(PROJECT_ROOT / "plugins" / "Example" / "Example.py"),
            id="example.tts",
            display_name="Example TTS",
            api_version="1",
            dependency_group="tts",
            capabilities=("synthesize",),
            config_schema={"type": "object"},
            required_python_packages=("requests",),
            required_files=("plugin:model.pth",),
            required_executables=("ffmpeg",),
            required_services=("http://127.0.0.1:9880",),
            supports_offline=False,
            supports_cpu=False,
            requires_gpu=True,
        )

        contract = descriptor.runtime_contract.to_dict()

        self.assertEqual("example.tts", contract["plugin_id"])
        self.assertEqual(
            {
                "id": "example.tts",
                "display_name": "Example TTS",
                "api_version": "1",
                "category": "text_to_speech",
                "entrypoint": "plugins.Example.Example:ExamplePlugin",
                "dependency_group": "tts",
            },
            contract["manifest"],
        )
        self.assertEqual(["synthesize"], contract["capabilities"])
        self.assertEqual(
            ["requests"],
            contract["availability_probe"]["required_python_packages"],
        )
        self.assertEqual(
            {
                "offline": False,
                "cpu": False,
                "requires_gpu": True,
            },
            contract["supports"],
        )

    def test_loader_exposes_contract_diagnostics_without_importing_plugin(self):#20260716_kpopmodder
        from plugin_system.loader import PluginLoader, PluginState

        module_name = "plugins.ContractDiagnostics.ContractDiagnosticsPlugin"
        sys.modules.pop(module_name, None)

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "ContractDiagnostics"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "ContractDiagnosticsPlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "IMPORT_COUNT = 0",
                    "IMPORT_COUNT += 1",
                    "class ContractDiagnosticsPlugin(LLMPluginInterface):",
                    "    PLUGIN_METADATA = {",
                    "        'id': 'contract.diagnostics',",
                    "        'display_name': 'Contract Diagnostics',",
                    "        'api_version': '1',",
                    "        'capabilities': ['llm', 'diagnostics'],",
                    "        'supports': {",
                    "            'offline': True,",
                    "            'cpu': True,",
                    "            'requires_gpu': False,",
                    "        },",
                    "    }",
                    "    def init(self): pass",
                    "    def predict(self, message, history): return ''",
                    "    def create_ui(self): return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            loader._load_plugins_from_directory(str(plugin_dir))

        self.assertNotIn(module_name, sys.modules)
        contract = loader.get_runtime_contracts()["language_model"][0]
        diagnostics = loader.get_diagnostics()["language_model"][0]

        self.assertEqual("contract.diagnostics", contract["plugin_id"])
        self.assertEqual(
            "plugins.ContractDiagnostics.ContractDiagnosticsPlugin:ContractDiagnosticsPlugin",
            contract["manifest"]["entrypoint"],
        )
        self.assertEqual(["llm", "diagnostics"], contract["capabilities"])
        self.assertEqual(PluginState.READY, diagnostics["state"])
        self.assertEqual(contract, diagnostics["runtime_contract"])
        self.assertEqual({}, diagnostics["diagnostic"])

    def test_interfaces_export_plugin_contracts(self):
        from plugin_system.interfaces import (
            InputPluginInterface,
            LLMPluginInterface,
            TranslationPluginInterface,
            TTSPluginInterface,
            VtuberPluginInterface,
        )
        from plugin_system.interfaces import (
            InputPluginInterface as CanonicalInput,
            LLMPluginInterface as CanonicalLLM,
            TranslationPluginInterface as CanonicalTranslation,
            TTSPluginInterface as CanonicalTTS,
            VtuberPluginInterface as CanonicalVtuber,
        )

        self.assertIs(InputPluginInterface, CanonicalInput)
        self.assertIs(LLMPluginInterface, CanonicalLLM)
        self.assertIs(TranslationPluginInterface, CanonicalTranslation)
        self.assertIs(TTSPluginInterface, CanonicalTTS)
        self.assertIs(VtuberPluginInterface, CanonicalVtuber)

    def test_loader_exports_plugin_loader_singleton(self):
        from plugin_system.loader import PluginLoader, plugin_loader
        from plugin_system.loader import (
            PluginLoader as CanonicalLoader,
            plugin_loader as canonical_plugin_loader,
        )

        self.assertIs(PluginLoader, CanonicalLoader)
        self.assertIs(plugin_loader, canonical_plugin_loader)

    def test_loader_allows_plugin_missing_from_modules_json(self):#20260627_kpopmodder
        from plugin_system.loader import PluginLoader

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "MissingKeyPlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "MissingKeyPlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "",
                    "class MissingKeyPlugin(LLMPluginInterface):",
                    "    def init(self):",
                    "        pass",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            loader._load_plugins_from_directory(str(plugin_dir))

            loaded_names = [
                plugin.name
                for plugin in loader.plugins["language_model"]
            ]
            self.assertEqual(["MissingKeyPlugin"], loaded_names)

    def test_modules_resolution_uses_root_production_default(self):#20260716_kpopmodder
        from core.profile_resolver import load_module_settings

        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            (root / "config").mkdir()
            (root / "modules.json").write_text(
                json.dumps({"RootOnly": True}),
                encoding="utf-8",
            )
            (root / "config" / "modules.example.json").write_text(
                json.dumps({"ExampleOnly": True}),
                encoding="utf-8",
            )

            resolution = load_module_settings(root, argv=[], environ={})

        self.assertEqual("production", resolution.source)
        self.assertEqual({"RootOnly": True}, resolution.settings)

    def test_modules_resolution_uses_core_config_only_when_profile_core(self):#20260716_kpopmodder
        from core.profile_resolver import load_module_settings

        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            (root / "config").mkdir()
            (root / "modules.json").write_text(
                json.dumps({"ProductionOnly": True}),
                encoding="utf-8",
            )
            (root / "config" / "modules.core.json").write_text(
                json.dumps({"CoreOnly": True}),
                encoding="utf-8",
            )

            resolution = load_module_settings(
                root,
                argv=["--profile", "Core"],
                environ={},
            )

        self.assertEqual("profile_core", resolution.source)
        self.assertEqual("Core", resolution.profile)
        self.assertEqual({"CoreOnly": True}, resolution.settings)

    def test_modules_resolution_does_not_use_example_as_runtime_fallback(self):#20260716_kpopmodder
        from core.profile_resolver import ModuleSettingsNotFound, load_module_settings

        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            (root / "config").mkdir()
            (root / "config" / "modules.example.json").write_text(
                json.dumps({"ExampleOnly": True}),
                encoding="utf-8",
            )

            with self.assertRaises(ModuleSettingsNotFound):
                load_module_settings(root, argv=[], environ={})

    def test_module_enabled_fails_closed_on_general_settings_error(self):#20260716_kpopmodder
        from app_core import module_config

        with mock.patch.object(
            module_config,
            "load_module_settings",
            side_effect=ValueError("bad json"),
        ):
            self.assertFalse(
                module_config.module_enabled(
                    "VoiceInput",
                    default=True,
                    current_module_directory=str(PROJECT_ROOT),
                )
            )

    def test_loader_malformed_modules_config_fails_closed_before_plugin_import(self):#20260716_kpopmodder
        from core.profile_resolver import ModuleSettingsError
        from plugin_system.loader import PluginLoader

        module_name = "plugins.BadConfigPlugin.BadConfigPlugin"
        sys.modules.pop(module_name, None)

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "BadConfigPlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "BadConfigPlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "IMPORT_COUNT = 0",
                    "CONSTRUCT_COUNT = 0",
                    "INIT_COUNT = 0",
                    "START_COUNT = 0",
                    "IMPORT_COUNT += 1",
                    "",
                    "class BadConfigPlugin(LLMPluginInterface):",
                    "    def __init__(self):",
                    "        global CONSTRUCT_COUNT",
                    "        CONSTRUCT_COUNT += 1",
                    "    def init(self):",
                    "        global INIT_COUNT",
                    "        INIT_COUNT += 1",
                    "    def start(self):",
                    "        global START_COUNT",
                    "        START_COUNT += 1",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text("{ malformed", encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)

            with self.assertRaises(ModuleSettingsError):
                loader.load_plugins()

            self.assertEqual([], loader.plugins["language_model"])
            self.assertNotIn(module_name, sys.modules)

    def test_core_profile_discovers_null_required_providers_only(self):#20260716_kpopmodder
        from plugin_system.loader import PluginLoader

        loader = PluginLoader("plugins")
        with mock.patch.dict("os.environ", {"LAVI_PROFILE": "Core"}):
            loader.load_plugins()

        self.assertEqual(
            ["NullInput"],
            [provider.name for provider in loader.plugins["input_gathering"]],
        )
        self.assertEqual(
            ["NullLLM"],
            [provider.name for provider in loader.plugins["language_model"]],
        )
        self.assertEqual(
            ["NoTranslate"],
            [provider.name for provider in loader.plugins["translation"]],
        )
        self.assertEqual(
            ["NullTTS"],
            [provider.name for provider in loader.plugins["text_to_speech"]],
        )
        self.assertEqual(
            ["NullVtuber"],
            [provider.name for provider in loader.plugins["vtuber"]],
        )

    def test_null_core_providers_construct_without_side_effects(self):#20260716_kpopmodder
        from plugin_system.interfaces import (
            InputPluginInterface,
            LLMPluginInterface,
            TTSPluginInterface,
            VtuberPluginInterface,
        )
        from plugins.NullInput.NullInput import NullInput
        from plugins.NullLLM.NullLLM import NullLLM
        from plugins.NullTTS.NullTTS import NullTTS
        from plugins.NullVtuber.NullVtuber import NullVtuber

        null_input = NullInput()
        null_llm = NullLLM()
        null_tts = NullTTS()
        null_vtuber = NullVtuber()

        self.assertIsInstance(null_input, InputPluginInterface)
        self.assertIsInstance(null_llm, LLMPluginInterface)
        self.assertIsInstance(null_tts, TTSPluginInterface)
        self.assertIsInstance(null_vtuber, VtuberPluginInterface)
        self.assertEqual("", null_llm.predict("hello", [], ""))
        self.assertIsNone(null_tts.synthesize("hello"))
        null_vtuber.set_avatar_data(VtuberPluginInterface.AvatarData())

    def test_tts_wrapper_skips_runtime_setup_for_null_tts(self):#20260716_kpopmodder
        import TTS as tts_module
        from plugin_system.interfaces import TTSPluginInterface
        from plugins.NullTTS.NullTTS import NullTTS

        fake_loader = mock.Mock()
        fake_loader.interface_to_category = {
            TTSPluginInterface: "text_to_speech",
        }
        fake_loader.plugins = {
            "text_to_speech": [NullTTS()],
        }

        with mock.patch("plugin_system.selection.plugin_loader", fake_loader):
            with mock.patch(
                "plugin_system.selection.config_manager.load_section",
                return_value={},
            ):
                with mock.patch("TTS.ensure_ffmpeg_exists") as ffmpeg_mock:
                    with mock.patch.object(
                        tts_module.TTS,
                        "register_stop_hotkey",
                    ) as hotkey_mock:
                        with mock.patch.object(
                            tts_module.TTS,
                            "start_stop_hotkey_polling",
                        ) as polling_mock:
                            tts = tts_module.TTS()
                            try:
                                ffmpeg_mock.assert_not_called()
                                hotkey_mock.assert_not_called()
                                polling_mock.assert_not_called()
                            finally:
                                tts.shutdown()

    def test_tts_wrapper_keeps_runtime_setup_for_real_tts(self):#20260716_kpopmodder
        import TTS as tts_module
        from plugin_system.interfaces import TTSPluginInterface

        class FakeTTS(TTSPluginInterface):
            def init(self):
                pass

            def synthesize(self, text):
                return None

            def create_ui(self):
                return None

        fake_loader = mock.Mock()
        fake_loader.interface_to_category = {
            TTSPluginInterface: "text_to_speech",
        }
        fake_loader.plugins = {
            "text_to_speech": [FakeTTS()],
        }

        with mock.patch("plugin_system.selection.plugin_loader", fake_loader):
            with mock.patch(
                "plugin_system.selection.config_manager.load_section",
                return_value={},
            ):
                with mock.patch(
                    "TTS.ensure_ffmpeg_exists",
                    return_value=True,
                ) as ffmpeg_mock:
                    with mock.patch.object(
                        tts_module.TTS,
                        "register_stop_hotkey",
                    ) as hotkey_mock:
                        with mock.patch.object(
                            tts_module.TTS,
                            "start_stop_hotkey_polling",
                        ) as polling_mock:
                            tts = tts_module.TTS()
                            try:
                                ffmpeg_mock.assert_called_once_with()
                                hotkey_mock.assert_called_once_with()
                                polling_mock.assert_called_once_with()
                            finally:
                                tts.shutdown()

    def test_selection_exports_provider_base(self):
        from plugin_system.selection import PluginSelectionBase, Provider
        from plugin_system.selection import (
            PluginSelectionBase as CanonicalSelectionBase,
            Provider as CanonicalProvider,
        )

        self.assertIs(PluginSelectionBase, CanonicalSelectionBase)
        self.assertIs(Provider, CanonicalProvider)

    def test_selection_uses_configured_provider_before_builtin_default(self):#20260627_kpopmodder
        from plugin_system.selection import Provider, select_default_provider

        hybrid = Provider()
        hybrid.name = "Hybrid_OpenAI_LLM"
        chatgpt = Provider()
        chatgpt.name = "ChatGPT_OpenAI"

        selected, source = select_default_provider(
            [hybrid, chatgpt],
            "language_model",
            configured_default_name="ChatGPT_OpenAI",
        )

        self.assertIs(chatgpt, selected)
        self.assertEqual("config", source)

    def test_selection_uses_builtin_llm_default_without_config(self):#20260627_kpopmodder
        from plugin_system.selection import Provider, select_default_provider

        chatgpt = Provider()
        chatgpt.name = "ChatGPT_OpenAI"
        hybrid = Provider()
        hybrid.name = "Hybrid_OpenAI_LLM"

        selected, source = select_default_provider(
            [chatgpt, hybrid],
            "language_model",
        )

        self.assertIs(hybrid, selected)
        self.assertEqual("builtin", source)

    def test_selection_initializes_only_selected_provider_at_startup(self):#20260630_kpopmodder
        from plugin_system.selection import PluginSelectionBase

        class FakeSelectionInterface:
            pass

        class SelectedProvider(FakeSelectionInterface):
            def __init__(self):
                self.init_count = 0
                self.ui_count = 0

            def init(self):
                self.init_count += 1

            def create_ui(self):
                self.ui_count += 1
                return object()

        class LazyProvider(FakeSelectionInterface):
            def __init__(self):
                self.init_count = 0
                self.ui_count = 0

            def init(self):
                self.init_count += 1

            def create_ui(self):
                self.ui_count += 1
                return object()

        selected = SelectedProvider()
        lazy = LazyProvider()
        fake_loader = mock.Mock()
        fake_loader.interface_to_category = {
            FakeSelectionInterface: "fake_category",
        }
        fake_loader.plugins = {
            "fake_category": [selected, lazy],
        }

        with mock.patch("plugin_system.selection.plugin_loader", fake_loader):
            with mock.patch(
                "plugin_system.selection.config_manager.load_section",
                return_value={},
            ):
                selection = PluginSelectionBase(FakeSelectionInterface)

        self.assertIs(selection.current_plugin, selected)
        self.assertEqual(1, selected.init_count)
        self.assertEqual(0, lazy.init_count)

        selection.on_dropdown_change("LazyProvider")

        self.assertIs(selection.current_plugin, lazy)
        self.assertEqual(1, selected.init_count)
        self.assertEqual(1, lazy.init_count)

    def test_selection_disables_only_failed_lazy_provider(self):#20260630_kpopmodder
        from plugin_system.selection import PluginSelectionBase

        class FakeSelectionInterface:
            pass

        class StableProvider(FakeSelectionInterface):
            def __init__(self):
                self.init_count = 0

            def init(self):
                self.init_count += 1

            def create_ui(self):
                return None

        class BrokenProvider(FakeSelectionInterface):
            def init(self):
                raise RuntimeError("provider boom")

            def create_ui(self):
                return None

        stable = StableProvider()
        broken = BrokenProvider()
        fake_loader = mock.Mock()
        fake_loader.interface_to_category = {
            FakeSelectionInterface: "fake_category",
        }
        fake_loader.plugins = {
            "fake_category": [stable, broken],
        }

        with mock.patch("plugin_system.selection.plugin_loader", fake_loader):
            with mock.patch(
                "plugin_system.selection.config_manager.load_section",
                return_value={},
            ):
                selection = PluginSelectionBase(FakeSelectionInterface)

        selection.on_dropdown_change("BrokenProvider")
        broken_provider = selection.find_provider_by_name(
            selection.provider_list,
            "BrokenProvider",
        )

        self.assertIs(selection.current_plugin, stable)
        self.assertEqual(1, stable.init_count)
        self.assertTrue(broken_provider.disabled)
        self.assertIn("provider boom", broken_provider.init_error)

    def test_loader_skips_runtime_pip_install_by_default(self):#20260630_kpopmodder
        from plugin_system.loader import PluginLoader

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "ReqPlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "requirements.txt").write_text(
                "some-fragile-package==1.0\n",
                encoding="utf-8",
            )
            (plugin_dir / "ReqPlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "",
                    "class ReqPlugin(LLMPluginInterface):",
                    "    def init(self):",
                    "        pass",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)

            with mock.patch("plugin_system.loader.subprocess.run") as run_mock:
                loader.load_plugins()

            run_mock.assert_not_called()
            loaded_names = [
                plugin.name
                for plugin in loader.plugins["language_model"]
            ]
            self.assertEqual(["ReqPlugin"], loaded_names)

    def test_loader_isolates_plugin_import_failure_on_selected_handle(self):#20260630_kpopmodder
        from plugin_system.loader import PluginLoader
        from plugin_system.interfaces import LLMPluginInterface

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "MixedPlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "BadPlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "raise RuntimeError('import boom')",
                    "class BadPlugin(LLMPluginInterface):",
                    "    def init(self):",
                    "        pass",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                ]),
                encoding="utf-8",
            )
            (plugin_dir / "GoodPlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "",
                    "class GoodPlugin(LLMPluginInterface):",
                    "    def init(self):",
                    "        pass",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            loader._load_plugins_from_directory(str(plugin_dir))

            handles = loader.plugins["language_model"]
            loaded_names = [plugin.name for plugin in handles]
            self.assertEqual(["BadPlugin", "GoodPlugin"], loaded_names)
            self.assertIsNone(handles[0].construct(LLMPluginInterface))
            self.assertIsNotNone(handles[1].construct(LLMPluginInterface))

    def test_loader_defers_and_isolates_plugin_constructor_failure(self):#20260630_kpopmodder
        from plugin_system.loader import PluginLoader
        from plugin_system.loader import PluginState
        from plugin_system.interfaces import LLMPluginInterface

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "ConstructPlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "ConstructPlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "",
                    "class FailingPlugin(LLMPluginInterface):",
                    "    def __init__(self):",
                    "        raise RuntimeError('constructor boom')",
                    "    def init(self):",
                    "        pass",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                    "",
                    "class GoodPlugin(LLMPluginInterface):",
                    "    def init(self):",
                    "        pass",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            loader._load_plugins_from_directory(str(plugin_dir))

            handles = loader.plugins["language_model"]
            loaded_names = [plugin.name for plugin in handles]
            self.assertEqual(["FailingPlugin", "GoodPlugin"], loaded_names)
            self.assertIsNone(handles[0].instance)
            self.assertEqual(PluginState.READY, handles[0].status)
            self.assertIsNone(handles[0].construct(LLMPluginInterface))
            self.assertEqual(PluginState.BROKEN, handles[0].status)
            self.assertIsNotNone(handles[1].construct(LLMPluginInterface))

    def test_loader_module_name_keeps_policy_suffix(self):#20260703_kpopmodder
        from plugin_system.loader import PluginLoader

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "PolicyPlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "policy.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "",
                    "class PolicyPlugin(LLMPluginInterface):",
                    "    imported_module_name = __name__",
                    "    def init(self):",
                    "        pass",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            loader._load_plugins_from_directory(str(plugin_dir))

            loaded = loader.plugins["language_model"][0].construct()
            self.assertTrue(loaded.__class__.__module__.endswith(".policy"))

    def test_p1a_representative_metadata_is_discovered_without_imports(self):#20260716_kpopmodder
        from plugin_system.loader import PluginLoader

        voice_module = "plugins.VoiceInput.voiceInput"
        vtube_module = "plugins.VtubeStudio.VtubeStudio"
        sys.modules.pop(voice_module, None)
        sys.modules.pop(vtube_module, None)

        with tempfile.TemporaryDirectory() as temp_dir:
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(
                json.dumps({"VoiceInput": True, "VtubeStudio": True}),
                encoding="utf-8",
            )
            loader = PluginLoader("plugins")
            loader.plugin_setting_path = str(modules_path)

            loader._load_plugins_from_directory(str(PROJECT_ROOT / "plugins" / "VoiceInput"))
            loader._load_plugins_from_directory(str(PROJECT_ROOT / "plugins" / "VtubeStudio"))

        voice_handle = loader.plugins["input_gathering"][0]
        vtube_handle = loader.plugins["vtuber"][0]

        self.assertEqual("VoiceInput", voice_handle.descriptor.id)
        self.assertEqual("Voice Input", voice_handle.descriptor.display_name)
        self.assertEqual("Full", voice_handle.descriptor.dependency_group)
        self.assertIn("torch", voice_handle.descriptor.required_python_packages)
        self.assertFalse(voice_handle.descriptor.supports_offline)
        self.assertEqual(
            "plugins.VoiceInput.voiceInput:VoiceInput",
            voice_handle.descriptor.entrypoint,
        )
        self.assertEqual("VtubeStudio", vtube_handle.descriptor.id)
        self.assertIn("websocket", vtube_handle.descriptor.required_python_packages)
        self.assertIn(
            "VTube Studio websocket ws://localhost:8001",
            vtube_handle.descriptor.required_services,
        )
        self.assertNotIn(voice_module, sys.modules)
        self.assertNotIn(vtube_module, sys.modules)

    def test_p1b_gpt_sovits_metadata_is_discovered_without_import(self):#20260716_kpopmodder
        from plugin_system.loader import PluginLoader

        module_name = "plugins.GPTSoVITS.GPTSoVITS"
        sys.modules.pop(module_name, None)

        with tempfile.TemporaryDirectory() as temp_dir:
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(
                json.dumps({"GPTSoVITS": True}),
                encoding="utf-8",
            )
            loader = PluginLoader("plugins")
            loader.plugin_setting_path = str(modules_path)

            loader._load_plugins_from_directory(
                str(PROJECT_ROOT / "plugins" / "GPTSoVITS")
            )

        handle = loader.plugins["text_to_speech"][0]

        self.assertEqual("GPTSoVITS", handle.descriptor.id)
        self.assertEqual("GPT-SoVITS", handle.descriptor.display_name)
        self.assertEqual("Full", handle.descriptor.dependency_group)
        self.assertIn("requests", handle.descriptor.required_python_packages)
        self.assertIn("plugin:gpt_sovits_ckpt_dir", handle.descriptor.required_files)
        self.assertIn(
            "GPT-SoVITS API server http://127.0.0.1:9880",
            handle.descriptor.required_services,
        )
        self.assertEqual(
            "plugins.GPTSoVITS.GPTSoVITS:GPTSoVITS",
            handle.descriptor.entrypoint,
        )
        self.assertTrue(handle.descriptor.requires_gpu)
        self.assertNotIn(module_name, sys.modules)

    def test_metadata_supports_requires_gpu_contract(self):#20260716_kpopmodder
        from plugin_system.loader import PluginLoader

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "GpuContractPlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "GpuContractPlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "",
                    "class GpuContractPlugin(LLMPluginInterface):",
                    "    PLUGIN_METADATA = {",
                    "        'id': 'GpuContractPlugin',",
                    "        'api_version': '1',",
                    "        'supports': {",
                    "            'offline': True,",
                    "            'cpu': False,",
                    "            'requires_gpu': True,",
                    "        },",
                    "    }",
                    "    def init(self):",
                    "        pass",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            loader._load_plugins_from_directory(str(plugin_dir))

        handle = loader.plugins["language_model"][0]

        self.assertTrue(handle.descriptor.supports_offline)
        self.assertFalse(handle.descriptor.supports_cpu)
        self.assertTrue(handle.descriptor.requires_gpu)
        self.assertEqual(
            {
                "offline": True,
                "cpu": False,
                "requires_gpu": True,
            },
            handle.descriptor.runtime_contract.to_dict()["supports"],
        )

    def test_metadata_nested_contract_is_discovered_without_import(self):#20260717_kpopmodder
        from plugin_system.loader import PluginLoader, PluginState

        module_name = "plugins.NestedContractPlugin.NestedContractPlugin"
        sys.modules.pop(module_name, None)

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "NestedContractPlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "NestedContractPlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "IMPORT_COUNT = 0",
                    "IMPORT_COUNT += 1",
                    "class NestedContractPlugin(LLMPluginInterface):",
                    "    PLUGIN_METADATA = {",
                    "        'manifest': {",
                    "            'id': 'nested.contract',",
                    "            'display_name': 'Nested Contract',",
                    "            'api_version': '1',",
                    "            'category': 'language_model',",
                    "            'entrypoint': 'plugins.NestedContractPlugin.NestedContractPlugin:NestedContractPlugin',",
                    "            'dependency_group': 'Full',",
                    "        },",
                    "        'capabilities': ['llm', 'nested_contract'],",
                    "        'config_schema': {'type': 'object'},",
                    "        'availability_probe': {",
                    "            'required_python_packages': ['json'],",
                    "            'required_files': [],",
                    "            'required_executables': [],",
                    "            'required_services': [],",
                    "            'timeout_sec': 0.5,",
                    "            'log_reference': 'nested probe',",
                    "        },",
                    "        'supports': {",
                    "            'offline': True,",
                    "            'cpu': True,",
                    "            'requires_gpu': False,",
                    "        },",
                    "    }",
                    "    def init(self): pass",
                    "    def predict(self, message, history): return ''",
                    "    def create_ui(self): return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            loader._load_plugins_from_directory(str(plugin_dir))

        handle = loader.plugins["language_model"][0]
        contract = handle.runtime_contract.to_dict()

        self.assertNotIn(module_name, sys.modules)
        self.assertEqual(PluginState.READY, handle.status)
        self.assertEqual("nested.contract", handle.descriptor.id)
        self.assertEqual("Nested Contract", handle.descriptor.display_name)
        self.assertEqual(["llm", "nested_contract"], contract["capabilities"])
        self.assertEqual(
            ["json"],
            contract["availability_probe"]["required_python_packages"],
        )
        self.assertEqual(0.5, contract["availability_probe"]["timeout_sec"])
        self.assertEqual("nested probe", contract["availability_probe"]["log_reference"])
        self.assertEqual([], list(handle.runtime_contract.validation_errors()))

    def test_metadata_invalid_nested_availability_probe_is_failed_plugin_only(self):#20260717_kpopmodder
        from plugin_system.loader import PluginLoader, PluginState

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "BadNestedProbePlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "BadNestedProbePlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "class BadNestedProbePlugin(LLMPluginInterface):",
                    "    PLUGIN_METADATA = {'availability_probe': ['not', 'object']}",
                    "    def init(self): pass",
                    "    def predict(self, message, history): return ''",
                    "    def create_ui(self): return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            loader._load_plugins_from_directory(str(plugin_dir))

        handle = loader.plugins["language_model"][0]

        self.assertEqual(PluginState.FAILED, handle.status)
        self.assertEqual(
            "metadata_invalid_availability_probe",
            handle.diagnostic.reason_code,
        )

    def test_p1a_missing_static_dependency_becomes_unavailable_without_import(self):#20260716_kpopmodder
        from plugin_system.loader import PluginLoader, PluginState
        from plugin_system.interfaces import LLMPluginInterface

        missing_package = "definitely_missing_lavi_p1a_package"
        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "UnavailablePlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "UnavailablePlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "",
                    "class UnavailablePlugin(LLMPluginInterface):",
                    "    PLUGIN_METADATA = {",
                    "        'id': 'UnavailablePlugin',",
                    "        'api_version': '1',",
                    "        'dependency_group': 'Voice',",
                    f"        'required_python_packages': ['{missing_package}'],",
                    "    }",
                    "    def init(self):",
                    "        pass",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            loader._load_plugins_from_directory(str(plugin_dir))

            handle = loader.plugins["language_model"][0]
            module_name = handle.descriptor.module_name
            self.assertIsNone(handle.construct(LLMPluginInterface))

        self.assertEqual(PluginState.UNAVAILABLE, handle.status)
        self.assertNotIn(module_name, sys.modules)
        self.assertEqual("missing_static_dependency", handle.diagnostic.reason_code)
        self.assertEqual([missing_package], list(handle.diagnostic.missing_python_packages))

    def test_p1a_voice_input_no_microphone_device_is_unavailable(self):#20260716_kpopmodder
        import types
        from plugin_system.loader import PluginLoader, PluginState

        fake_sounddevice = types.SimpleNamespace(query_devices=lambda: [])
        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "VoiceProbePlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "VoiceProbePlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import InputPluginInterface",
                    "class VoiceProbePlugin(InputPluginInterface):",
                    "    PLUGIN_METADATA = {",
                    "        'id': 'VoiceInput',",
                    "        'required_services': ['microphone_input_device'],",
                    "    }",
                    "    def init(self): pass",
                    "    def create_ui(self): return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            with mock.patch.dict(sys.modules, {"sounddevice": fake_sounddevice}):
                loader._load_plugins_from_directory(str(plugin_dir))

            handle = loader.plugins["input_gathering"][0]

        self.assertEqual(PluginState.UNAVAILABLE, handle.status)
        self.assertEqual("required_service_unavailable", handle.diagnostic.reason_code)
        self.assertEqual(["microphone_input_device"], list(handle.diagnostic.missing_services))

    def test_p1a_vtube_studio_missing_websocket_is_unavailable(self):#20260716_kpopmodder
        from plugin_system.loader import PluginLoader, PluginState

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "VtubeProbePlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "VtubeProbePlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import VtuberPluginInterface",
                    "class VtubeProbePlugin(VtuberPluginInterface):",
                    "    PLUGIN_METADATA = {",
                    "        'id': 'VtubeProbePlugin',",
                    "        'required_services': ['VTube Studio websocket ws://localhost:8001'],",
                    "    }",
                    "    def init(self): pass",
                    "    def create_ui(self): return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            with mock.patch("plugin_system.loader.socket.create_connection", side_effect=OSError("refused")):
                loader._load_plugins_from_directory(str(plugin_dir))

            handle = loader.plugins["vtuber"][0]

        self.assertEqual(PluginState.UNAVAILABLE, handle.status)
        self.assertEqual("required_service_unavailable", handle.diagnostic.reason_code)
        self.assertEqual(
            ["VTube Studio websocket ws://localhost:8001"],
            list(handle.diagnostic.missing_services),
        )

    def test_p1b_gpt_sovits_empty_model_dirs_are_unavailable(self):#20260716_kpopmodder
        from plugin_system.loader import PluginLoader, PluginState

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "GPTProbePlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "gpt_sovits_ckpt_dir").mkdir()
            (plugin_dir / "gpt_sovits_model_dir").mkdir()
            (plugin_dir / "GPTProbePlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import TTSPluginInterface",
                    "class GPTProbePlugin(TTSPluginInterface):",
                    "    PLUGIN_METADATA = {",
                    "        'id': 'GPTSoVITS',",
                    "        'required_files': ['plugin:gpt_sovits_ckpt_dir', 'plugin:gpt_sovits_model_dir'],",
                    "    }",
                    "    def init(self): pass",
                    "    def synthesize(self, text): return None",
                    "    def create_ui(self): return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            loader._load_plugins_from_directory(str(plugin_dir))

            handle = loader.plugins["text_to_speech"][0]

        self.assertEqual(PluginState.UNAVAILABLE, handle.status)
        self.assertEqual("missing_model_configuration", handle.diagnostic.reason_code)
        self.assertTrue(any(path.endswith("*.ckpt") for path in handle.diagnostic.missing_files))
        self.assertTrue(any(path.endswith("*.pth") for path in handle.diagnostic.missing_files))

    def test_p1a_api_version_mismatch_is_isolated(self):#20260716_kpopmodder
        from plugin_system.loader import PluginLoader, PluginState

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "VersionPlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "VersionPlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "",
                    "class VersionPlugin(LLMPluginInterface):",
                    "    PLUGIN_METADATA = {'id': 'VersionPlugin', 'api_version': '999'}",
                    "    def init(self):",
                    "        pass",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            loader._load_plugins_from_directory(str(plugin_dir))

            handle = loader.plugins["language_model"][0]

        self.assertEqual(PluginState.FAILED, handle.status)
        self.assertEqual("api_version_mismatch", handle.diagnostic.reason_code)
        self.assertIsNone(handle.construct())

    def test_p1a_duplicate_plugin_id_is_isolated(self):#20260716_kpopmodder
        from plugin_system.loader import PluginLoader, PluginState

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "DuplicatePlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "DuplicatePlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "",
                    "class FirstPlugin(LLMPluginInterface):",
                    "    PLUGIN_METADATA = {'id': 'duplicate.plugin'}",
                    "    def init(self):",
                    "        pass",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                    "",
                    "class SecondPlugin(LLMPluginInterface):",
                    "    PLUGIN_METADATA = {'id': 'duplicate.plugin'}",
                    "    def init(self):",
                    "        pass",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            loader._load_plugins_from_directory(str(plugin_dir))

            handles = loader.plugins["language_model"]

        self.assertEqual(PluginState.READY, handles[0].status)
        self.assertEqual(PluginState.FAILED, handles[1].status)
        self.assertEqual("duplicate_plugin_id", handles[1].diagnostic.reason_code)

    def test_p1a_metadata_category_mismatch_is_failed_plugin_only(self):#20260716_kpopmodder
        from plugin_system.loader import PluginLoader, PluginState

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "CategoryPlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "CategoryPlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "",
                    "class CategoryPlugin(LLMPluginInterface):",
                    "    PLUGIN_METADATA = {'id': 'CategoryPlugin', 'category': 'input_gathering'}",
                    "    def init(self):",
                    "        pass",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            loader._load_plugins_from_directory(str(plugin_dir))

            handle = loader.plugins["language_model"][0]

        self.assertEqual(PluginState.FAILED, handle.status)
        self.assertEqual("metadata_category_mismatch", handle.diagnostic.reason_code)
        self.assertIsNone(handle.construct())

    def test_p1a_metadata_entrypoint_mismatch_is_failed_plugin_only(self):#20260716_kpopmodder
        from plugin_system.loader import PluginLoader, PluginState

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "EntrypointPlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "EntrypointPlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "",
                    "class EntrypointPlugin(LLMPluginInterface):",
                    "    PLUGIN_METADATA = {",
                    "        'id': 'EntrypointPlugin',",
                    "        'entrypoint': 'plugins.EntrypointPlugin.EntrypointPlugin:WrongClass',",
                    "    }",
                    "    def init(self):",
                    "        pass",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            loader._load_plugins_from_directory(str(plugin_dir))

            handle = loader.plugins["language_model"][0]

        self.assertEqual(PluginState.FAILED, handle.status)
        self.assertEqual("metadata_entrypoint_mismatch", handle.diagnostic.reason_code)
        self.assertIsNone(handle.construct())

    def test_p1a_malformed_config_schema_does_not_abort_load_plugins(self):#20260716_kpopmodder
        from plugin_system.loader import PluginLoader, PluginState

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "SchemaPlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "SchemaPlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "",
                    "class BadSchemaPlugin(LLMPluginInterface):",
                    "    PLUGIN_METADATA = {'id': 'BadSchemaPlugin', 'config_schema': ['not', 'object']}",
                    "    def init(self):",
                    "        pass",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                    "",
                    "class GoodSchemaPlugin(LLMPluginInterface):",
                    "    PLUGIN_METADATA = {'id': 'GoodSchemaPlugin', 'config_schema': {}}",
                    "    def init(self):",
                    "        pass",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            loader._load_plugins_from_directory(str(plugin_dir))

            handles = loader.plugins["language_model"]

        self.assertEqual(["BadSchemaPlugin", "GoodSchemaPlugin"], [h.name for h in handles])
        self.assertEqual(PluginState.FAILED, handles[0].status)
        self.assertEqual("metadata_invalid_config_schema", handles[0].diagnostic.reason_code)
        self.assertEqual(PluginState.READY, handles[1].status)

    def test_p1a_duplicate_plugin_id_uses_sorted_discovery_order(self):#20260716_kpopmodder
        from plugin_system.loader import PluginLoader, PluginState

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "SortedDuplicatePlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "z_second.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "class ZSecondPlugin(LLMPluginInterface):",
                    "    PLUGIN_METADATA = {'id': 'sorted.duplicate'}",
                    "    def init(self): pass",
                    "    def predict(self, message, history): return ''",
                    "    def create_ui(self): return None",
                ]),
                encoding="utf-8",
            )
            (plugin_dir / "a_first.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "class AFirstPlugin(LLMPluginInterface):",
                    "    PLUGIN_METADATA = {'id': 'sorted.duplicate'}",
                    "    def init(self): pass",
                    "    def predict(self, message, history): return ''",
                    "    def create_ui(self): return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            loader._load_plugins_from_directory(str(plugin_dir))

            handles = loader.plugins["language_model"]

        self.assertEqual(["AFirstPlugin", "ZSecondPlugin"], [h.name for h in handles])
        self.assertEqual(PluginState.READY, handles[0].status)
        self.assertEqual(PluginState.FAILED, handles[1].status)
        self.assertEqual("duplicate_plugin_id", handles[1].diagnostic.reason_code)

    def test_loader_is_idempotent(self):#20260716_kpopmodder
        from plugin_system.loader import PluginLoader

        with tempfile.TemporaryDirectory() as temp_dir:
            plugin_root = Path(temp_dir) / "plugins"
            plugin_dir = plugin_root / "OncePlugin"
            plugin_dir.mkdir(parents=True)
            (plugin_dir / "OncePlugin.py").write_text(
                "\n".join([
                    "from plugin_system.interfaces import LLMPluginInterface",
                    "",
                    "class OncePlugin(LLMPluginInterface):",
                    "    def init(self):",
                    "        pass",
                    "    def predict(self, message, history):",
                    "        return ''",
                    "    def create_ui(self):",
                    "        return None",
                ]),
                encoding="utf-8",
            )
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(json.dumps({}), encoding="utf-8")

            loader = PluginLoader("plugins")
            loader.plugin_directory = str(plugin_root)
            loader.plugin_setting_path = str(modules_path)
            loader.load_plugins()
            loader.load_plugins()

            loaded_names = [plugin.name for plugin in loader.plugins["language_model"]]
            self.assertEqual(["OncePlugin"], loaded_names)

    def test_selection_with_handles_constructs_only_selected_provider(self):#20260716_kpopmodder
        from plugin_system.selection import PluginSelectionBase

        class FakeSelectionInterface:
            pass

        class SelectedProvider(FakeSelectionInterface):
            def __init__(self):
                self.init_count = 0
                self.ui_count = 0

            def init(self):
                self.init_count += 1

            def create_ui(self):
                self.ui_count += 1
                return object()

        class LazyProvider(FakeSelectionInterface):
            def __init__(self):
                self.init_count = 0
                self.ui_count = 0

            def init(self):
                self.init_count += 1

            def create_ui(self):
                self.ui_count += 1
                return object()

        class FakeHandle:
            def __init__(self, name, plugin):
                self.name = name
                self.plugin = plugin
                self.construct_count = 0
                self.error = ""

            def construct(self, expected_interface=None):
                self.construct_count += 1
                return self.plugin

            def mark_running(self):
                pass

        selected = SelectedProvider()
        lazy = LazyProvider()
        selected_handle = FakeHandle("SelectedProvider", selected)
        lazy_handle = FakeHandle("LazyProvider", lazy)
        fake_loader = mock.Mock()
        fake_loader.interface_to_category = {
            FakeSelectionInterface: "fake_category",
        }
        fake_loader.plugins = {
            "fake_category": [selected_handle, lazy_handle],
        }

        with mock.patch("plugin_system.selection.plugin_loader", fake_loader):
            with mock.patch(
                "plugin_system.selection.config_manager.load_section",
                return_value={},
            ):
                selection = PluginSelectionBase(FakeSelectionInterface)

        self.assertIs(selection.current_plugin, selected)
        self.assertEqual(1, selected_handle.construct_count)
        self.assertEqual(0, lazy_handle.construct_count)
        self.assertEqual(1, selected.init_count)
        self.assertEqual(0, lazy.init_count)

        selection.create_plugin_ui()
        selection.create_plugin_ui()

        self.assertEqual(1, selected_handle.construct_count)
        self.assertEqual(0, lazy_handle.construct_count)
        self.assertEqual(1, selected.ui_count)
        self.assertEqual(0, lazy.ui_count)

        selection.on_dropdown_change("LazyProvider")
        selection.on_dropdown_change("LazyProvider")

        self.assertIs(selection.current_plugin, lazy)
        self.assertEqual(1, lazy_handle.construct_count)
        self.assertEqual(1, lazy.init_count)

    def test_input_shared_ui_can_render_lazy_provider_panels(self):#20260716_kpopmodder
        from plugin_system.selection import PluginSelectionBase

        class FakeInputInterface:
            pass

        class VoiceProvider(FakeInputInterface):
            def __init__(self):
                self.init_count = 0
                self.ui_count = 0

            def init(self):
                self.init_count += 1

            def create_ui(self):
                self.ui_count += 1
                return object()

        class ChatProvider(FakeInputInterface):
            def __init__(self):
                self.init_count = 0
                self.ui_count = 0

            def init(self):
                self.init_count += 1

            def create_ui(self):
                self.ui_count += 1
                return object()

        class FakeHandle:
            def __init__(self, name, plugin):
                self.name = name
                self.plugin = plugin
                self.construct_count = 0
                self.error = ""

            def construct(self, expected_interface=None):
                self.construct_count += 1
                return self.plugin

            def mark_running(self):
                pass

        voice = VoiceProvider()
        chat = ChatProvider()
        voice_handle = FakeHandle("VoiceProvider", voice)
        chat_handle = FakeHandle("ChatProvider", chat)
        fake_loader = mock.Mock()
        fake_loader.interface_to_category = {
            FakeInputInterface: "input_gathering",
        }
        fake_loader.plugins = {
            "input_gathering": [voice_handle, chat_handle],
        }

        with mock.patch("plugin_system.selection.plugin_loader", fake_loader):
            with mock.patch(
                "plugin_system.selection.config_manager.load_section",
                return_value={},
            ):
                selection = PluginSelectionBase(FakeInputInterface)

        selection.create_all_provider_ui()
        selection.create_all_provider_ui()

        self.assertEqual(1, voice_handle.construct_count)
        self.assertEqual(1, chat_handle.construct_count)
        self.assertEqual(1, voice.init_count)
        self.assertEqual(0, chat.init_count)
        self.assertEqual(1, voice.ui_count)
        self.assertEqual(1, chat.ui_count)

    def test_p1b_ui_failure_isolates_other_provider(self):#20260716_kpopmodder
        from plugin_system.selection import PluginSelectionBase

        class FakeSelectionInterface:
            pass

        class BrokenUiProvider(FakeSelectionInterface):
            def __init__(self):
                self.init_count = 0

            def init(self):
                self.init_count += 1

            def create_ui(self):
                raise RuntimeError("ui boom")

        class StableProvider(FakeSelectionInterface):
            def __init__(self):
                self.init_count = 0

            def init(self):
                self.init_count += 1

            def create_ui(self):
                return object()

        class FakeHandle:
            def __init__(self, name, plugin):
                self.name = name
                self.plugin = plugin
                self.construct_count = 0
                self.failed_reason_code = ""
                self.error = ""

            def construct(self, expected_interface=None):
                self.construct_count += 1
                return self.plugin

            def mark_running(self):
                pass

            def mark_failed(self, error, reason_code="plugin_failed"):
                self.error = str(error)
                self.failed_reason_code = reason_code

        broken = BrokenUiProvider()
        stable = StableProvider()
        broken_handle = FakeHandle("BrokenUiProvider", broken)
        stable_handle = FakeHandle("StableProvider", stable)
        fake_loader = mock.Mock()
        fake_loader.interface_to_category = {
            FakeSelectionInterface: "fake_category",
        }
        fake_loader.plugins = {
            "fake_category": [broken_handle, stable_handle],
        }

        with mock.patch("plugin_system.selection.plugin_loader", fake_loader):
            with mock.patch(
                "plugin_system.selection.config_manager.load_section",
                return_value={},
            ):
                selection = PluginSelectionBase(FakeSelectionInterface)

        selection.create_plugin_ui()
        broken_provider = selection.find_provider_by_name(
            selection.provider_list,
            "BrokenUiProvider",
        )

        self.assertTrue(broken_provider.disabled)
        self.assertEqual("ui_failed", broken_handle.failed_reason_code)
        self.assertEqual(0, stable_handle.construct_count)

        selection.on_dropdown_change("StableProvider")

        self.assertIs(selection.current_plugin, stable)
        self.assertEqual(1, stable_handle.construct_count)
        self.assertEqual(1, stable.init_count)

    def test_selection_falls_back_when_configured_provider_fails(self):#20260716_kpopmodder
        from plugin_system.selection import PluginSelectionBase

        class FakeSelectionInterface:
            pass

        class BrokenProvider(FakeSelectionInterface):
            def init(self):
                raise RuntimeError("broken default")

            def create_ui(self):
                return None

        class StableProvider(FakeSelectionInterface):
            def __init__(self):
                self.init_count = 0

            def init(self):
                self.init_count += 1

            def create_ui(self):
                return None

        broken = BrokenProvider()
        stable = StableProvider()
        fake_loader = mock.Mock()
        fake_loader.interface_to_category = {
            FakeSelectionInterface: "fake_category",
        }
        fake_loader.plugins = {
            "fake_category": [broken, stable],
        }

        with mock.patch("plugin_system.selection.plugin_loader", fake_loader):
            with mock.patch(
                "plugin_system.selection.config_manager.load_section",
                return_value={"default_fake_category_provider": "BrokenProvider"},
            ):
                selection = PluginSelectionBase(FakeSelectionInterface)

        self.assertIs(selection.current_plugin, stable)
        self.assertEqual(1, stable.init_count)
        broken_provider = selection.find_provider_by_name(
            selection.provider_list,
            "BrokenProvider",
        )
        self.assertTrue(broken_provider.disabled)
        self.assertIn("broken default", broken_provider.init_error)

    def test_selection_cleans_partial_init_failure_and_shutdown_is_idempotent(self):#20260716_kpopmodder
        from plugin_system.selection import PluginSelectionBase

        class FakeSelectionInterface:
            pass

        class PartialProvider(FakeSelectionInterface):
            def __init__(self):
                self.resource_active = False
                self.shutdown_count = 0

            def init(self):
                self.resource_active = True
                raise RuntimeError("partial init")

            def shutdown(self):
                self.shutdown_count += 1
                self.resource_active = False

            def create_ui(self):
                return None

        class StableProvider(FakeSelectionInterface):
            def __init__(self):
                self.init_count = 0
                self.shutdown_count = 0

            def init(self):
                self.init_count += 1

            def shutdown(self):
                self.shutdown_count += 1

            def create_ui(self):
                return None

        partial = PartialProvider()
        stable = StableProvider()
        fake_loader = mock.Mock()
        fake_loader.interface_to_category = {
            FakeSelectionInterface: "fake_category",
        }
        fake_loader.plugins = {
            "fake_category": [partial, stable],
        }

        with mock.patch("plugin_system.selection.plugin_loader", fake_loader):
            with mock.patch(
                "plugin_system.selection.config_manager.load_section",
                return_value={"default_fake_category_provider": "PartialProvider"},
            ):
                selection = PluginSelectionBase(FakeSelectionInterface)

        self.assertIs(selection.current_plugin, stable)
        self.assertFalse(partial.resource_active)
        self.assertEqual(1, partial.shutdown_count)
        self.assertEqual(1, stable.init_count)

        selection.shutdown()
        selection.shutdown()

        self.assertEqual(1, partial.shutdown_count)
        self.assertEqual(1, stable.shutdown_count)

    def test_selection_cleanup_failure_does_not_block_fallback(self):#20260716_kpopmodder
        from plugin_system.selection import PluginSelectionBase

        class FakeSelectionInterface:
            pass

        class BrokenCleanupProvider(FakeSelectionInterface):
            def init(self):
                raise RuntimeError("init boom")

            def shutdown(self):
                raise RuntimeError("cleanup boom")

            def create_ui(self):
                return None

        class StableProvider(FakeSelectionInterface):
            def init(self):
                pass

            def create_ui(self):
                return None

        broken = BrokenCleanupProvider()
        stable = StableProvider()
        fake_loader = mock.Mock()
        fake_loader.interface_to_category = {
            FakeSelectionInterface: "fake_category",
        }
        fake_loader.plugins = {
            "fake_category": [broken, stable],
        }

        with mock.patch("plugin_system.selection.plugin_loader", fake_loader):
            with mock.patch(
                "plugin_system.selection.config_manager.load_section",
                return_value={"default_fake_category_provider": "BrokenCleanupProvider"},
            ):
                selection = PluginSelectionBase(FakeSelectionInterface)

        self.assertIs(selection.current_plugin, stable)

    def test_optional_plugin_loader_isolates_import_failure(self):#20260703_kpopmodder
        from app_core.optional_plugin_loader import instantiate_optional_plugin

        with tempfile.TemporaryDirectory() as temp_dir:
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(
                json.dumps({"Chess": True}),
                encoding="utf-8",
            )

            with mock.patch(
                "app_core.optional_plugin_loader.importlib.import_module",
                side_effect=ModuleNotFoundError("No module named 'chess'"),
            ):
                plugin = instantiate_optional_plugin(
                    "Chess",
                    "plugins.Chess.Chess",
                    "Chess",
                    False,
                    temp_dir,
                )

            self.assertIsNone(plugin)

    def test_optional_plugin_loader_records_unavailable_static_dependency(self):
        from app_core.optional_plugin_loader import instantiate_optional_plugin
        from plugin_system.loader import PluginState
        from plugin_system.registry import plugin_registry

        with tempfile.TemporaryDirectory() as temp_dir:
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(
                json.dumps({"MissingOptional": True}),
                encoding="utf-8",
            )

            plugin = instantiate_optional_plugin(
                "MissingOptional",
                "plugins.MissingOptional.missing_optional",
                "MissingOptional",
                False,
                temp_dir,
                manifest={
                    "id": "MissingOptional",
                    "display_name": "Missing Optional",
                    "dependency_group": "Full",
                    "required_python_packages": (
                        "lavi_missing_package_for_static_dependency_test",
                    ),
                },
            )

        self.assertIsNone(plugin)
        snapshot = plugin_registry.snapshot()
        self.assertEqual(
            PluginState.UNAVAILABLE,
            snapshot["MissingOptional"]["status"],
        )
        self.assertEqual(
            "missing_static_dependency",
            snapshot["MissingOptional"]["diagnostic"]["reason_code"],
        )
        self.assertEqual(
            ["lavi_missing_package_for_static_dependency_test"],
            snapshot["MissingOptional"]["diagnostic"]["missing_python_packages"],
        )
        self.assertEqual(
            "MissingOptional",
            snapshot["MissingOptional"]["runtime_contract"]["plugin_id"],
        )
        self.assertEqual(
            "plugins.MissingOptional.missing_optional:MissingOptional",
            snapshot["MissingOptional"]["runtime_contract"]["manifest"]["entrypoint"],
        )
        self.assertEqual(
            ["lavi_missing_package_for_static_dependency_test"],
            snapshot["MissingOptional"]["runtime_contract"]["availability_probe"][
                "required_python_packages"
            ],
        )

    def test_optional_plugin_loader_accepts_nested_contract_probe_metadata(self):#20260717_kpopmodder
        from app_core.optional_plugin_loader import instantiate_optional_plugin
        from plugin_system.loader import PluginState
        from plugin_system.registry import plugin_registry

        missing_package = "lavi_missing_nested_optional_contract_package"
        with tempfile.TemporaryDirectory() as temp_dir:
            modules_path = Path(temp_dir) / "modules.json"
            modules_path.write_text(
                json.dumps({"NestedOptional": True}),
                encoding="utf-8",
            )

            plugin = instantiate_optional_plugin(
                "NestedOptional",
                "plugins.NestedOptional.nested_optional",
                "NestedOptional",
                False,
                temp_dir,
                manifest={
                    "manifest": {
                        "id": "nested.optional",
                        "display_name": "Nested Optional",
                        "api_version": "1",
                        "dependency_group": "Full",
                    },
                    "availability_probe": {
                        "required_python_packages": (missing_package,),
                    },
                },
            )

        self.assertIsNone(plugin)
        snapshot = plugin_registry.snapshot()
        self.assertEqual(
            PluginState.UNAVAILABLE,
            snapshot["NestedOptional"]["status"],
        )
        self.assertEqual(
            "nested.optional",
            snapshot["NestedOptional"]["runtime_contract"]["plugin_id"],
        )
        self.assertEqual(
            [missing_package],
            snapshot["NestedOptional"]["runtime_contract"]["availability_probe"][
                "required_python_packages"
            ],
        )
        self.assertEqual(
            [missing_package],
            snapshot["NestedOptional"]["diagnostic"]["missing_python_packages"],
        )


if __name__ == "__main__":
    unittest.main()
