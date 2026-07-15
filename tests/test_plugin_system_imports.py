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

            def init(self):
                self.init_count += 1

            def create_ui(self):
                return None

        class LazyProvider(FakeSelectionInterface):
            def __init__(self):
                self.init_count = 0

            def init(self):
                self.init_count += 1

            def create_ui(self):
                return None

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

            def init(self):
                self.init_count += 1

            def create_ui(self):
                return None

        class LazyProvider(FakeSelectionInterface):
            def __init__(self):
                self.init_count = 0

            def init(self):
                self.init_count += 1

            def create_ui(self):
                return None

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


if __name__ == "__main__":
    unittest.main()
