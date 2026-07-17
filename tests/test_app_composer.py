#202600707_kpopmodder
#20260706_kpopmodder: Add startup lifecycle regression tests for AppComposer.
import unittest
from queue import Queue
from types import ModuleType
from unittest import mock

from app_core import optional_plugin_loader
from app_core.app_composer import AppComposer
from app_core.core_component_composition import (
    CoreComponentCompositionResult,
    CoreComponentCompositionService,
)
from app_core.extensions import (
    GameExtensionCompositionResult,
    GameExtensionCompositionService,
)
from app_core.optional_plugin_composition import OptionalPluginCompositionResult


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
        fake_runtime.start.side_effect = RuntimeError("core startup failure")
        fake_runtime.shutdown = mock.Mock()

        def create_runtime_lifecycle():
            events.append("create_runtime_lifecycle")
            composer.runtime_lifecycle = fake_runtime
            fake_runtime.start()

        composer.create_runtime_lifecycle = mock.Mock(
            side_effect=create_runtime_lifecycle,
        )

        with self.assertRaises(RuntimeError):
            composer.run()

        self.assertEqual(1, fake_runtime.shutdown.call_count)
        self.assertEqual(1, events.count("create_runtime_lifecycle"))

    def test_log_gpu_startup_skips_gpu_preflight_for_core_profile(self):
        composer = AppComposer()

        with mock.patch.dict("os.environ", {"LAVI_PROFILE": "Core"}):
            with mock.patch(
                "app_core.app_composer.gpu_device_manager.log_startup_summary",
            ) as summary_mock:
                with mock.patch(
                    "app_core.app_composer.gpu_device_manager."
                    "log_startup_vram_preflight",
                ) as preflight_mock:
                    composer.log_gpu_startup()

        summary_mock.assert_not_called()
        preflight_mock.assert_not_called()

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

    def test_create_optional_plugins_consumes_composition_result(self):
        composer = AppComposer()
        song_player = object()
        chess_plugin = object()
        starcraft_plugin = object()
        starcraft116_plugin = object()
        starcraft2_plugin = object()
        screen_vision = object()
        composer.optional_plugin_composition_service = mock.Mock()
        composer.optional_plugin_composition_service.compose.return_value = (
            OptionalPluginCompositionResult(
                song_player=song_player,
                chess_plugin=chess_plugin,
                starcraft_plugin=starcraft_plugin,
                starcraft116_plugin=starcraft116_plugin,
                starcraft2_plugin=starcraft2_plugin,
                screen_vision=screen_vision,
                optional_components=(
                    song_player,
                    starcraft_plugin,
                    screen_vision,
                ),
                startup_components=(
                    song_player,
                    starcraft_plugin,
                    screen_vision,
                ),
            )
        )

        composer.create_optional_plugins()

        composer.optional_plugin_composition_service.compose.assert_called_once_with(
            memory_store=composer.memory_store,
        )
        self.assertIs(song_player, composer.song_player)
        self.assertIs(chess_plugin, composer.chess_plugin)
        self.assertIs(starcraft_plugin, composer.starcraft_plugin)
        self.assertIs(starcraft116_plugin, composer.starcraft116_plugin)
        self.assertIs(starcraft2_plugin, composer.starcraft2_plugin)
        self.assertIs(screen_vision, composer.screen_vision)
        self.assertEqual(
            [song_player, starcraft_plugin, screen_vision],
            composer.optional_components,
        )
        self.assertEqual(
            [song_player, starcraft_plugin, screen_vision],
            composer._startup_components,
        )

    def test_create_core_components_consumes_composition_result(self):
        composer = AppComposer()
        composer.memory_context_builder = object()
        composer.memory_command_handler = object()
        composer.screen_question_router = object()
        input_component = object()
        llm = object()
        translate = object()
        tts = object()
        vtuber = object()
        composer.core_component_composition_service = mock.Mock()
        composer.core_component_composition_service.compose.return_value = (
            CoreComponentCompositionResult(
                input=input_component,
                llm=llm,
                translate=translate,
                tts=tts,
                vtuber=vtuber,
                core_components=(input_component, translate, tts, vtuber, llm),
                startup_components=(input_component, translate, tts, vtuber, llm),
            )
        )

        composer.create_core_components()

        composer.core_component_composition_service.compose.assert_called_once_with(
            memory_context_builder=composer.memory_context_builder,
            memory_command_handler=composer.memory_command_handler,
            screen_question_router=composer.screen_question_router,
        )
        self.assertIs(input_component, composer.input)
        self.assertIs(llm, composer.llm)
        self.assertIs(translate, composer.translate)
        self.assertIs(tts, composer.tts)
        self.assertIs(vtuber, composer.vtuber)
        self.assertEqual(
            [input_component, translate, tts, vtuber, llm],
            composer.core_components,
        )
        self.assertEqual(
            [input_component, translate, tts, vtuber, llm],
            composer._startup_components,
        )

    def test_core_component_composition_service_builds_core_components(self):
        class FakeInput:
            pass

        class FakeTranslate:
            pass

        class FakeTTS:
            pass

        class FakeVtuber:
            pass

        class FakeLLM:
            def __init__(
                self,
                memory_context_builder=None,
                memory_command_handler=None,
                screen_question_router=None,
            ):
                self.memory_context_builder = memory_context_builder
                self.memory_command_handler = memory_command_handler
                self.screen_question_router = screen_question_router

        def module_with(module_name, class_name, class_value):
            module = ModuleType(module_name)
            setattr(module, class_name, class_value)
            return module

        memory_context_builder = object()
        memory_command_handler = object()
        screen_question_router = object()
        fake_modules = {
            "input_core.input_component": module_with(
                "input_core.input_component",
                "Input",
                FakeInput,
            ),
            "llm_core.llm_component": module_with(
                "llm_core.llm_component",
                "LLM",
                FakeLLM,
            ),
            "translation_core.translate_component": module_with(
                "translation_core.translate_component",
                "Translate",
                FakeTranslate,
            ),
            "tts_core.tts_component": module_with(
                "tts_core.tts_component",
                "TTS",
                FakeTTS,
            ),
            "vtuber_core.vtuber_component": module_with(
                "vtuber_core.vtuber_component",
                "Vtuber",
                FakeVtuber,
            ),
        }

        with mock.patch.dict("sys.modules", fake_modules):
            result = CoreComponentCompositionService().compose(
                memory_context_builder=memory_context_builder,
                memory_command_handler=memory_command_handler,
                screen_question_router=screen_question_router,
            )

        self.assertIsInstance(result.input, FakeInput)
        self.assertIsInstance(result.translate, FakeTranslate)
        self.assertIsInstance(result.tts, FakeTTS)
        self.assertIsInstance(result.vtuber, FakeVtuber)
        self.assertIsInstance(result.llm, FakeLLM)
        self.assertIs(result.llm.memory_context_builder, memory_context_builder)
        self.assertIs(result.llm.memory_command_handler, memory_command_handler)
        self.assertIs(result.llm.screen_question_router, screen_question_router)
        self.assertEqual(
            (result.input, result.translate, result.tts, result.vtuber, result.llm),
            result.startup_components,
        )

    #20260716_kpopmodder: Disabled StarCraft2 must not import/register its passive observer.
    def test_starcraft2_disabled_does_not_import_or_register_observer(self):
        registry = mock.Mock()
        service = GameExtensionCompositionService(registry)
        real_import = __import__

        def import_or_fail(name, *args, **kwargs):
            if name == "plugins.StarCraft2.starcraft2_core.sc2_extension":
                raise AssertionError("StarCraft2 observer should not be imported")
            return real_import(name, *args, **kwargs)

        with mock.patch("builtins.__import__", side_effect=import_or_fail):
            result = service.compose(context=object(), starcraft2_plugin=None)

        self.assertIsNone(result.starcraft2_game_extension)
        self.assertIsNone(result.starcraft2_changeling_observer_extension)
        self.assertEqual([], result.registered_extensions)
        registry.register.assert_not_called()
        registry.initialize.assert_not_called()

    def test_starcraft116_extension_owns_callback_import_during_startup(self):
        composer = AppComposer()

        def instantiate_manifest_plugin(module_name, *args, **kwargs):
            if module_name == "StarCraft116":
                return mock.Mock()
            return None

        composition_service = composer.optional_plugin_composition_service
        composition_service.instantiate_manifest_plugin = mock.Mock(
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
        #20260717_kpopmodder: Verify AppComposer tracks only the registry as lifecycle owner.
        composer.game_extension_registry = mock.Mock()
        composer.game_extension_registry.all.return_value = [
            composer.chess_game_extension,
            composer.starcraft116_game_extension,
            composer.starcraft2_game_extension,
        ]

        composer.build_managed_components()

        self.assertNotIn(composer.chess_plugin, composer.managed_components)
        self.assertNotIn(composer.starcraft116_plugin, composer.managed_components)
        self.assertNotIn(composer.starcraft2_plugin, composer.managed_components)
        self.assertNotIn(composer.chess_game_extension, composer.managed_components)
        self.assertNotIn(
            composer.starcraft116_game_extension,
            composer.managed_components,
        )
        self.assertNotIn(composer.starcraft2_game_extension, composer.managed_components)
        self.assertIn(composer.game_extension_registry, composer.managed_components)
        self.assertIn(composer.game_extension_registry, composer.optional_components)

    def test_game_extension_registry_keeps_extension_lifecycle_out_of_app_composer(self):
        #20260717_kpopmodder: AppComposer must not directly start or stop extensions.
        composer = AppComposer()
        composer.input = object()
        composer.llm = object()
        composer.translate = object()
        composer.tts = object()
        composer.vtuber = object()
        extension = mock.Mock()
        extension.name = "fake_game"
        composer.game_extension_registry = mock.Mock()
        composer.game_extension_registry.all.return_value = [extension]

        composer.build_managed_components()

        self.assertEqual(1, composer.managed_components.count(composer.game_extension_registry))
        self.assertEqual(1, composer.optional_components.count(composer.game_extension_registry))
        extension.start.assert_not_called()
        extension.stop.assert_not_called()

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

        with mock.patch("app_core.app_composer.RuntimeLifecycle") as lifecycle_class:
            lifecycle = mock.Mock()
            lifecycle_class.return_value = lifecycle
            composer.create_runtime_lifecycle()

        lifecycle.start.assert_called_once()
        lifecycle.start_components.assert_not_called()
        lifecycle.start_global_updates.assert_not_called()
        extension.start.assert_not_called()

    def test_register_game_extensions_delegates_to_composition_service(self):
        composer = AppComposer()
        composer.game_extension_context = object()
        composer.starcraft116_plugin = object()
        composer.starcraft2_plugin = object()
        composer.chess_plugin = object()
        starcraft116_extension = object()
        starcraft2_extension = object()
        observer_extension = object()
        chess_extension = object()
        composer.game_extension_composition_service = mock.Mock()
        composer.game_extension_composition_service.compose.return_value = (
            GameExtensionCompositionResult(
                starcraft116_game_extension=starcraft116_extension,
                starcraft2_game_extension=starcraft2_extension,
                starcraft2_changeling_observer_extension=observer_extension,
                chess_game_extension=chess_extension,
            )
        )

        composer._register_game_extensions()

        composer.game_extension_composition_service.compose.assert_called_once_with(
            context=composer.game_extension_context,
            starcraft116_plugin=composer.starcraft116_plugin,
            starcraft2_plugin=composer.starcraft2_plugin,
            chess_plugin=composer.chess_plugin,
            starcraft116_game_extension=None,
            starcraft2_game_extension=None,
            starcraft2_changeling_observer_extension=None,
            chess_game_extension=None,
        )
        self.assertIs(starcraft116_extension, composer.starcraft116_game_extension)
        self.assertIs(starcraft2_extension, composer.starcraft2_game_extension)
        self.assertIs(
            observer_extension,
            composer.starcraft2_changeling_observer_extension,
        )
        self.assertIs(chess_extension, composer.chess_game_extension)

    def test_game_debug_status_exposes_runtime_and_recent_events(self):
        composer = AppComposer()
        runtime_context = composer.game_runtime_contexts.get("starcraft116")
        runtime_context.mark_initialized(True)

        composer.game_event_bus.emit(
            {
                "event_type": "unit_created",
                "game": "starcraft116",
                "source": "test",
                "details": {
                    "unit": {"id": 7, "type": "Terran Marine"},
                    "summary": "Terran Marine created.",
                },
            }
        )

        status = composer.get_game_debug_status()

        self.assertTrue(status["runtime_contexts"]["starcraft116"]["initialized"])
        self.assertEqual(1, status["event_monitor"]["total_events"])
        recent = status["event_monitor"]["recent_events"][0]
        self.assertEqual("unit_created", recent["event_type"])
        self.assertEqual("starcraft116", recent["game"])
        self.assertEqual("test", recent["source"])
        self.assertEqual("dict", recent["details"]["unit"]["type"])
        self.assertEqual("Terran Marine created.", recent["details"]["summary"])
