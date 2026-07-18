#202600707_kpopmodder
#20260705_kpopmodder: Added AppComposer to keep main.py as a thin Windows startup entry point.
import logging
import os

from app_core.component_wiring_composition import AppComponentWiringService
from app_core.composition_core.app_ui_composition_service import (
    AppUiCompositionService,
)
from app_core.core_component_composition import CoreComponentCompositionService
from app_core.gradio_launch import load_gradio_launch_options
from app_core.gradio_runtime_launcher import GradioRuntimeLauncher
from app_core.memory_bootstrap import bootstrap_memory
from app_core.extensions import (
    ExtensionRegistry,
    GameEventBus,
    GameEventMonitor,
    GameExtensionCompositionService,
    GameExtensionContext,
    GameRuntimeContextRegistry,
)
from app_core.optional_plugin_composition import OptionalPluginCompositionService
from app_core.runtime_lifecycle import RuntimeLifecycle
from app_core.screen_router_bootstrap import build_screen_question_router
from core.global_state import GlobalKeys, global_state
from core.config_manager import config_manager
from core.gpu_device_manager import gpu_device_manager
from core.import_path import ensure_import_path
from core.logger import log_print
from core.profile_resolver import active_profile
from plugin_system.loader import plugin_loader


class AppComposer:
    #20260705_kpopmodder: This class only moves startup assembly out of main.py; component behavior stays owned by existing modules.
    def __init__(self, current_module_directory=None):
        self.current_module_directory = (
            current_module_directory
            or os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        )
        self.plugin_directory = os.path.join(
            self.current_module_directory,
            "plugins",
        )
        self.memory_store = None
        self.memory_context_builder = None
        self.screen_question_router = None
        self.memory_command_handler = None
        self.managed_components = []#20260623_kpopmodder
        self.main_interface = None
        self.runtime_lifecycle = None

        self.input = None
        self.llm = None
        self.translate = None
        self.tts = None
        self.vtuber = None
        self.song_player = None
        self.chess_plugin = None
        self.chess_game_extension = None
        self.starcraft_plugin = None
        self.starcraft116_plugin = None
        self.starcraft116_game_extension = None
        self.starcraft2_plugin = None
        self.starcraft2_game_extension = None
        self.starcraft2_changeling_observer_extension = None
        self.screen_vision = None
        self.game_extension_context = None
        self.game_runtime_contexts = GameRuntimeContextRegistry()
        self.game_event_bus = GameEventBus()
        self.game_event_monitor = GameEventMonitor()
        self.game_event_monitor.attach(self.game_event_bus)
        self.handle_chess_ai_move_applied = None
        self.game_extension_registry = ExtensionRegistry()
        self.game_extension_composition_service = GameExtensionCompositionService(
            self.game_extension_registry,
        )
        self.component_wiring_service = AppComponentWiringService()
        self.ui_composition_service = AppUiCompositionService()
        self.gradio_runtime_launcher = GradioRuntimeLauncher()
        self.gradio_launch_config_loader = config_manager.load_section
        self.core_component_composition_service = CoreComponentCompositionService()
        self.optional_plugin_composition_service = OptionalPluginCompositionService(
            self.current_module_directory,
        )
        self.core_components = []
        self.optional_components = []
        self._startup_components = []  #20260706_kpopmodder: Keep startup-instantiated components for guaranteed cleanup.

    def run(self):
        self.configure_logging()
        try:
            self.prepare_plugin_path()
            self.load_plugins()
            self.log_gpu_startup()
            self.bootstrap_memory()
            self.build_screen_question_router()
            self.build_interface()
            self.create_runtime_lifecycle()
            self.launch_gradio()
        except Exception as error:
            self._shutdown_on_startup_failure()
            log_print(f"[AppComposer] startup failed: {error}")
            raise

    def _shutdown_on_startup_failure(self):
        if self.runtime_lifecycle is not None:
            self.runtime_lifecycle.shutdown()
            return
        self._shutdown_managed_components()

    def _shutdown_managed_components(self):
        components = self._collect_shutdown_components()
        if not components:
            return
        seen = set()
        for component in components:
            component_id = id(component)
            if component_id in seen:
                continue
            seen.add(component_id)
            shutdown = getattr(component, "shutdown", None)
            if not callable(shutdown):
                continue
            try:
                shutdown()
            except KeyboardInterrupt:
                log_print(
                    f"[Shutdown] component shutdown interrupted (startup fallback): "
                    f"{component.__class__.__name__}"
                )
            except Exception as e:
                log_print(
                    f"[Shutdown] component shutdown failed (startup fallback): "
                    f"{component.__class__.__name__}: {e}"
                )

    def _collect_shutdown_components(self):
        if getattr(self, "managed_components", None):
            return list(self.managed_components)
        return list(self._startup_components)

    def _register_startup_component(self, component):
        if component is None:
            return
        if component in self._startup_components:
            return
        self._startup_components.append(component)

    def configure_logging(self):
        logging.basicConfig(level=logging.WARNING)

    def prepare_plugin_path(self):
        #20260718_kpopmodder: Route legacy plugin path mutation through one helper.
        ensure_import_path(self.plugin_directory)

    def load_plugins(self):
        # load plugins
        plugin_loader.load_plugins()

    def log_gpu_startup(self):
        if active_profile() == "Core":
            log_print(
                "[GPUDeviceManager] startup GPU preflight skipped for Core profile"
            )#20260716_kpopmodder
            return
        gpu_device_manager.log_startup_summary(#20260626_kpopmodder
            ("VoiceInput", "ScreenVision", "GPTSoVITS")
        )
        gpu_device_manager.log_startup_vram_preflight(#20260627_kpopmodder
            ("VoiceInput", "ScreenVision", "GPTSoVITS")
        )

    def bootstrap_memory(self):
        #20260630_kpopmodder: Keep startup focused; app_core owns memory/router wiring.
        self.memory_store, self.memory_context_builder = bootstrap_memory(
            self.current_module_directory,
        )
        #20260626_kpopmodder: Manual "기억해줘" long-term memory commands are disabled by default.
        # from memory_core.memory_command_handler import MemoryCommandHandler#20260621_kpopmodder
        #20260626_kpopmodder: Keep memory_command_handler unset so "기억해줘" does not short-circuit into long_term_memory.json.
        # memory_command_handler = MemoryCommandHandler(memory_store)#20260621_kpopmodder
        self.memory_command_handler = None#20260626_kpopmodder

    def build_screen_question_router(self):
        self.screen_question_router = build_screen_question_router()

    def build_interface(self):
        import gradio as gr

        # load ui
        with gr.Blocks() as self.main_interface:
            self.create_core_components()
            self.create_optional_plugins()
            self.build_game_extension_context()
            self.wire_optional_plugin_callbacks()
            self.create_component_ui()
            self.wire_event_listeners()
            self.build_managed_components()

    def build_game_extension_context(self):
        self.game_extension_context = GameExtensionContext(
            app_composer=self,
            input_component=self.input,
            llm=self.llm,
            translate=self.translate,
            tts=self.tts,
            vtuber=self.vtuber,
            screen_vision=self.screen_vision,
            song_player=self.song_player,
            memory_store=self.memory_store,
            memory_context_builder=self.memory_context_builder,
            screen_question_router=self.screen_question_router,
            global_state=global_state,
            runtime_contexts=self.game_runtime_contexts,
            event_bus=self.game_event_bus,
            runtime_state={},
        )
        self._register_game_extensions()

    def get_game_debug_status(self):
        #20260715_kpopmodder: Expose shared game runtime/event snapshots without adding UI-specific logic here.
        return {
            "runtime_contexts": self.game_runtime_contexts.snapshot(),
            "event_monitor": self.game_event_monitor.snapshot(),
        }

    def create_core_components(self):
        result = self.core_component_composition_service.compose(
            memory_context_builder=self.memory_context_builder,
            memory_command_handler=self.memory_command_handler,
            screen_question_router=self.screen_question_router,#20260628_kpopmodder
        )
        for attribute_name, component in result.attribute_map().items():
            setattr(self, attribute_name, component)
        self.core_components = list(result.core_components)
        for component in result.startup_components:
            self._register_startup_component(component)

    def create_optional_plugins(self):
        #20260717_kpopmodder: Optional plugin construction/roles are owned by the composition service.
        result = self.optional_plugin_composition_service.compose(
            memory_store=self.memory_store,
        )
        for attribute_name, plugin in result.attribute_map().items():
            setattr(self, attribute_name, plugin)
        self.optional_components = list(result.optional_components)
        for component in result.startup_components:
            self._register_startup_component(component)

        #20260707_kpopmodder: Chess plugin lifecycle/callback wiring is owned by ChessGameExtension.
        self.handle_chess_ai_move_applied = None
        #20260707_kpopmodder: StarCraft116 plugin startup/watcher callback is owned by StarCraft116GameExtension.
        #20260707_kpopmodder: StarCraft2 plugin lifecycle/callback wiring is owned by StarCraft2GameExtension.

    def instantiate_manifest_plugin(self, module_name, *args, **kwargs):
        #20260717_kpopmodder: Compatibility facade; new code should use OptionalPluginCompositionService.
        return self.optional_plugin_composition_service.instantiate_manifest_plugin(
            module_name,
            *args,
            **kwargs,
        )

    def wire_optional_plugin_callbacks(self):
        self.component_wiring_service.wire_optional_plugin_callbacks(
            starcraft_plugin=self.starcraft_plugin,
            screen_vision=self.screen_vision,
        )

    def _register_game_extensions(self):
        result = self.game_extension_composition_service.compose(
            context=self.game_extension_context,
            starcraft116_plugin=self.starcraft116_plugin,
            starcraft2_plugin=self.starcraft2_plugin,
            chess_plugin=self.chess_plugin,
            starcraft116_game_extension=self.starcraft116_game_extension,
            starcraft2_game_extension=self.starcraft2_game_extension,
            starcraft2_changeling_observer_extension=(
                self.starcraft2_changeling_observer_extension
            ),
            chess_game_extension=self.chess_game_extension,
        )
        self.starcraft116_game_extension = result.starcraft116_game_extension
        self.starcraft2_game_extension = result.starcraft2_game_extension
        self.starcraft2_changeling_observer_extension = (
            result.starcraft2_changeling_observer_extension
        )
        self.chess_game_extension = result.chess_game_extension

    def create_component_ui(self):
        self.ui_composition_service.create_component_ui(
            input_component=self.input,
            llm=self.llm,
            translate=self.translate,
            tts=self.tts,
            vtuber=self.vtuber,
            song_player=self.song_player,
            chess_plugin=self.chess_plugin,
            starcraft_plugin=self.starcraft_plugin,
            starcraft116_plugin=self.starcraft116_plugin,
            starcraft2_plugin=self.starcraft2_plugin,
            screen_vision=self.screen_vision,
        )

    def wire_event_listeners(self):
        self.component_wiring_service.wire_event_listeners(
            input_component=self.input,
            llm=self.llm,
            translate=self.translate,
            tts=self.tts,
            vtuber=self.vtuber,
            song_player=self.song_player,
            starcraft_plugin=self.starcraft_plugin,
            screen_vision=self.screen_vision,
            screen_vision_input_callback=self.receive_screen_vision_input,
        )

    def receive_screen_vision_input(self, text):#20260628_kpopmodder
        if global_state.get_value(GlobalKeys.IS_SONG_PLAYING, False):
            log_print(
                "[ScreenVision] song playing state blocked LLM input."
            )#20260628_kpopmodder
            return
        self.llm.receive_input(text)

    def build_managed_components(self):
        result = self.component_wiring_service.build_managed_components(
            input_component=self.input,
            llm=self.llm,
            translate=self.translate,
            tts=self.tts,
            vtuber=self.vtuber,
            screen_vision=self.screen_vision,
            song_player=self.song_player,
            starcraft_plugin=self.starcraft_plugin,
            game_extension_registry=self.game_extension_registry,
            optional_components=self.optional_components,
        )
        self.managed_components = list(result.managed_components)
        self.core_components = list(result.core_components)
        self.optional_components = list(result.optional_components)
        for component in result.startup_components:
            self._register_startup_component(component)

    def create_runtime_lifecycle(self):
        self.runtime_lifecycle = RuntimeLifecycle(#20260630_kpopmodder
            managed_components=self.managed_components,
            llm=self.llm,
            translate=self.translate,
            tts=self.tts,
            song_player=self.song_player,
            core_components=self.core_components,
            optional_components=self.optional_components,
            global_state_instance=global_state,
        )
        self.runtime_lifecycle.start()

    def launch_gradio(self):
        gradio_launch_options = load_gradio_launch_options(
            self.gradio_launch_config_loader("Gradio")
        )
        self.gradio_runtime_launcher.launch(
            self.main_interface,
            runtime_lifecycle=self.runtime_lifecycle,
            **gradio_launch_options,
        )
