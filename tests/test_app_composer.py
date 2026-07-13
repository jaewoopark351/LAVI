#202600707_kpopmodder
#20260706_kpopmodder: Add startup lifecycle regression tests for AppComposer.
import unittest
from queue import Queue
from unittest import mock

from app_core import optional_plugin_loader
from app_core.app_composer import AppComposer


class FakeQueueComponent:
    def __init__(self):
        self.input_queue = Queue()


class AppComposerTests(unittest.TestCase):
    def test_run_executes_startup_steps_in_expected_order(self):
        composer = AppComposer()
        events = []

        composer.configure_logging = mock.Mock(
            side_effect=lambda: events.append("configure_logging"),
        )
        composer.prepare_plugin_path = mock.Mock(
            side_effect=lambda: events.append("prepare_plugin_path"),
        )
        composer.load_plugins = mock.Mock(
            side_effect=lambda: events.append("load_plugins"),
        )
        composer.log_gpu_startup = mock.Mock(
            side_effect=lambda: events.append("log_gpu_startup"),
        )
        composer.bootstrap_memory = mock.Mock(
            side_effect=lambda: events.append("bootstrap_memory"),
        )
        composer.build_screen_question_router = mock.Mock(
            side_effect=lambda: events.append("build_screen_question_router"),
        )
        composer.build_interface = mock.Mock(
            side_effect=lambda: events.append("build_interface"),
        )
        composer.create_runtime_lifecycle = mock.Mock(
            side_effect=lambda: events.append("create_runtime_lifecycle"),
        )
        composer.launch_gradio = mock.Mock(
            side_effect=lambda: events.append("launch_gradio"),
        )

        composer.run()

        self.assertEqual(
            [
                "configure_logging",
                "prepare_plugin_path",
                "load_plugins",
                "log_gpu_startup",
                "bootstrap_memory",
                "build_screen_question_router",
                "build_interface",
                "create_runtime_lifecycle",
                "launch_gradio",
            ],
            events,
        )

    def test_core_startup_failure_triggers_shutdown(self):
        composer = AppComposer()
        events = []

        composer.configure_logging = mock.Mock(
            side_effect=lambda: events.append("configure_logging"),
        )
        composer.prepare_plugin_path = mock.Mock(
            side_effect=lambda: events.append("prepare_plugin_path"),
        )
        composer.load_plugins = mock.Mock(
            side_effect=lambda: events.append("load_plugins"),
        )
        composer.log_gpu_startup = mock.Mock(
            side_effect=lambda: events.append("log_gpu_startup"),
        )
        composer.bootstrap_memory = mock.Mock(
            side_effect=lambda: events.append("bootstrap_memory"),
        )
        composer.build_screen_question_router = mock.Mock(
            side_effect=lambda: events.append("build_screen_question_router"),
        )
        composer.build_interface = mock.Mock(
            side_effect=lambda: events.append("build_interface"),
        )
        composer.launch_gradio = mock.Mock()

        fake_runtime = mock.Mock()
        fake_runtime.start_components.side_effect = RuntimeError("core startup failure")
        fake_runtime.start_global_updates = mock.Mock()
        fake_runtime.shutdown = mock.Mock()

        def create_runtime_lifecycle():
            events.append("create_runtime_lifecycle")
            composer.runtime_lifecycle = fake_runtime
            fake_runtime.start_components()
            fake_runtime.start_global_updates()

        composer.create_runtime_lifecycle = mock.Mock(
            side_effect=create_runtime_lifecycle,
        )

        with self.assertRaises(RuntimeError):
            composer.run()

        self.assertEqual(1, fake_runtime.shutdown.call_count)
        self.assertEqual(1, events.count("create_runtime_lifecycle"))

    def test_receive_screen_vision_input_blocks_when_song_playing(self):
        composer = AppComposer()
        composer.llm = mock.Mock()
        composer.llm.receive_input = mock.Mock()

        with mock.patch(
            "app_core.app_composer.global_state.get_value",
            return_value=True,
        ):
            composer.receive_screen_vision_input("sample input")

        composer.llm.receive_input.assert_not_called()

    def test_optional_plugins_disabled_do_not_import_or_instantiate(self):
        composer = AppComposer()

        with mock.patch(
            "app_core.optional_plugin_loader.module_enabled",
            return_value=False,
        ) as module_enabled_mock:
            with mock.patch(
                "app_core.optional_plugin_loader.importlib.import_module",
            ) as import_module_mock:
                composer.create_optional_plugins()

        module_enabled_mock.assert_called()
        import_module_mock.assert_not_called()
        self.assertIsNone(composer.song_player)
        self.assertIsNone(composer.chess_plugin)
        self.assertIsNone(composer.starcraft_plugin)
        self.assertIsNone(composer.starcraft116_plugin)
        self.assertIsNone(composer.starcraft2_plugin)
        self.assertIsNone(composer.screen_vision)

    def test_starcraft116_extension_owns_callback_import_during_startup(self):
        composer = AppComposer()

        def instantiate_manifest_plugin(module_name, *args, **kwargs):
            if module_name == "StarCraft116":
                return mock.Mock()
            return None

        composer.instantiate_manifest_plugin = mock.Mock(
            side_effect=instantiate_manifest_plugin,
        )

        real_import_module = optional_plugin_loader.importlib.import_module

        def import_starcraft_callback_or_fail(module_name, *args, **kwargs):
            if (
                module_name
                == "plugins.StarCraft116.starcraft116_core.starcraft116_reaction_runtime"
            ):
                raise ModuleNotFoundError("callback dependency missing")
            return real_import_module(module_name, *args, **kwargs)

        with mock.patch(
            "app_core.optional_plugin_loader.importlib.import_module",
            side_effect=import_starcraft_callback_or_fail,
        ):
            composer.create_optional_plugins()

        self.assertIsNotNone(composer.starcraft116_plugin)
        self.assertFalse(
            hasattr(composer, "handle_starcraft116_status_event"),
            "AppComposer should not keep StarCraft116 callback holders after extension migration.",
        )
        self.assertFalse(
            hasattr(composer, "build_starcraft116_status_event_callback"),
            "AppComposer should not keep StarCraft116 callback builders after extension migration.",
        )

    def test_game_plugins_are_not_direct_lifecycle_components(self):
        composer = AppComposer()
        composer.input = object()
        composer.llm = object()
        composer.translate = object()
        composer.tts = object()
        composer.vtuber = object()
        composer.chess_plugin = object()
        composer.starcraft116_plugin = object()
        composer.starcraft2_plugin = object()
        composer.chess_game_extension = object()
        composer.starcraft116_game_extension = object()
        composer.starcraft2_game_extension = object()

        composer.build_managed_components()

        self.assertNotIn(composer.chess_plugin, composer.managed_components)
        self.assertNotIn(composer.starcraft116_plugin, composer.managed_components)
        self.assertNotIn(composer.starcraft2_plugin, composer.managed_components)
        self.assertIn(composer.chess_game_extension, composer.managed_components)
        self.assertIn(composer.starcraft116_game_extension, composer.managed_components)
        self.assertIn(composer.starcraft2_game_extension, composer.managed_components)

    def test_create_runtime_lifecycle_does_not_start_registry_extensions_again(self):
        composer = AppComposer()
        composer.input = FakeQueueComponent()
        composer.llm = FakeQueueComponent()
        composer.translate = FakeQueueComponent()
        composer.tts = FakeQueueComponent()
        composer.vtuber = FakeQueueComponent()
        extension = mock.Mock()
        extension.name = "fake_game"
        composer.managed_components = []
        composer.core_components = []
        composer.optional_components = []
        composer.game_extension_registry = mock.Mock()
        composer.game_extension_registry.all.return_value = [extension]

        with mock.patch("app_core.app_composer.atexit.register"):
            with mock.patch("app_core.app_composer.RuntimeLifecycle") as lifecycle_class:
                lifecycle = mock.Mock()
                lifecycle_class.return_value = lifecycle
                composer.create_runtime_lifecycle()

        lifecycle.start_components.assert_called_once()
        lifecycle.start_global_updates.assert_called_once()
        extension.start.assert_not_called()
