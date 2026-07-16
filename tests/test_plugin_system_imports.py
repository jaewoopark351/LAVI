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
        self.assertNotIn(module_name, sys.modules)

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


if __name__ == "__main__":
    unittest.main()
