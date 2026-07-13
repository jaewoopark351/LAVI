#202600707_kpopmodder
#20260705_kpopmodder: Added AppComposer to keep main.py as a thin Windows startup entry point.
import atexit
import logging
import os
import sys

from app_core.gradio_launch import find_available_port
from app_core.memory_bootstrap import bootstrap_memory
from app_core.extensions import ExtensionRegistry, GameExtensionContext
from app_core.optional_module_manifest import get_optional_module_manifest
from app_core.optional_plugin_loader import (
    instantiate_optional_plugin,
)
from app_core.runtime_lifecycle import RuntimeLifecycle
from app_core.screen_router_bootstrap import build_screen_question_router
from core.global_state import GlobalKeys, global_state
from core.gpu_device_manager import gpu_device_manager
from core.logger import log_print
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
        self.handle_chess_ai_move_applied = None
        self.game_extension_registry = ExtensionRegistry()
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
        # allow relative imports in plugins folder
        if self.plugin_directory not in sys.path:
            sys.path.append(self.plugin_directory)

    def load_plugins(self):
        # load plugins
        plugin_loader.load_plugins()

    def log_gpu_startup(self):
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
            runtime_state={},
        )
        self._register_game_extensions()

    def create_core_components(self):
        from Input import Input
        from LLM import LLM
        from Translate import Translate
        from TTS import TTS
        from VTuber import Vtuber

        self.core_components = []
        self.input = Input()
        self._register_startup_component(self.input)
        self.core_components.append(self.input)
        #llm = LLM()#20260621_kpopmodder
        self.translate = Translate()
        self._register_startup_component(self.translate)
        self.core_components.append(self.translate)
        self.tts = TTS()
        self._register_startup_component(self.tts)
        self.core_components.append(self.tts)
        self.vtuber = Vtuber()
        self._register_startup_component(self.vtuber)
        self.core_components.append(self.vtuber)
        self.llm = LLM(#20260621_kpopmodder
            memory_context_builder=self.memory_context_builder,
            memory_command_handler=self.memory_command_handler,
            screen_question_router=self.screen_question_router,#20260628_kpopmodder
        )
        self._register_startup_component(self.llm)
        self.core_components.append(self.llm)

    def create_optional_plugins(self):
        self.optional_components = []
        #20260630_kpopmodder: Keep direct optional plugins gated by modules.json.
        self.song_player = self.instantiate_manifest_plugin(
            "SongPlayer",
        )#20260629_kpopmodder: SongPlayer is optional via modules.json.
        self._register_startup_component(self.song_player)
        if self.song_player is not None:
            self.optional_components.append(self.song_player)
        self.chess_plugin = self.instantiate_manifest_plugin(
            "Chess",
        )#20260628_kpopmodder: Chess is optional via modules.json and owns its web server.
        #20260707_kpopmodder: Chess plugin lifecycle/callback wiring is owned by ChessGameExtension.
        self.handle_chess_ai_move_applied = None
        self.starcraft_plugin = self.instantiate_manifest_plugin(
            "StarCraftRemastered",
        )#20260630_kpopmodder: StarCraft Remastered support is optional via modules.json.
        self._register_startup_component(self.starcraft_plugin)
        if self.starcraft_plugin is not None:
            self.optional_components.append(self.starcraft_plugin)
        self.starcraft116_plugin = self.instantiate_manifest_plugin(
            "StarCraft116",
        )#20260702_kpopmodder: StarCraft 1.16 BWAPI bot launcher is separate from Remastered.
        #20260707_kpopmodder: StarCraft116 plugin startup/watcher callback is owned by StarCraft116GameExtension.
        self.starcraft2_plugin = self.instantiate_manifest_plugin(
            "StarCraft2",
        )#20260707_kpopmodder: StarCraft2 plugin lifecycle/callback wiring is owned by StarCraft2GameExtension.
        self.screen_vision = self.instantiate_manifest_plugin(
            "ScreenVision",
            memory_store=self.memory_store,
        )#20260629_kpopmodder: ScreenVision is optional via modules.json.
        self._register_startup_component(self.screen_vision)
        if self.screen_vision is not None:
            self.optional_components.append(self.screen_vision)

    def instantiate_manifest_plugin(self, module_name, *args, **kwargs):
        manifest = get_optional_module_manifest(module_name)
        return instantiate_optional_plugin(
            module_name,
            manifest["module_path"],
            manifest["class_name"],
            manifest["default_enabled"],
            self.current_module_directory,
            *args,
            **kwargs,
        )#20260703_kpopmodder

    def wire_optional_plugin_callbacks(self):
        if self.starcraft_plugin is not None:
            self.starcraft_plugin.set_screen_observation_provider(
                lambda: getattr(self.screen_vision, "last_screen_observation", "")
                if self.screen_vision is not None
                else ""
            )#20260630_kpopmodder

    def _register_game_extensions(self):
        registered_extensions = []
        if self.starcraft116_plugin is not None and (
            self.starcraft116_game_extension is None
        ):
            try:
                from app_core.extensions.starcraft116_game_extension import (
                    StarCraft116GameExtension,
                )

                self.starcraft116_game_extension = StarCraft116GameExtension(
                    plugin=self.starcraft116_plugin,
                )
                self.game_extension_registry.register(self.starcraft116_game_extension)
                registered_extensions.append(self.starcraft116_game_extension)
            except Exception as e:
                log_print(
                    "[AppComposer] register StarCraft116GameExtension failed: "
                    f"{type(e).__name__}: {e}"
                )
                self.starcraft116_game_extension = None

        if self.starcraft2_plugin is not None and self.starcraft2_game_extension is None:
            try:
                from app_core.extensions.starcraft2_game_extension import (
                    StarCraft2GameExtension,
                )

                self.starcraft2_game_extension = StarCraft2GameExtension(
                    plugin=self.starcraft2_plugin,
                )
                self.game_extension_registry.register(self.starcraft2_game_extension)
                registered_extensions.append(self.starcraft2_game_extension)
                log_print("[AppComposer] starcraft2 game extension registered")
            except Exception as e:
                log_print(
                    "[AppComposer] register StarCraft2GameExtension failed: "
                    f"{type(e).__name__}: {e}"
                )
                self.starcraft2_game_extension = None

        if self.starcraft2_changeling_observer_extension is None:
            try:
                from plugins.StarCraft2.starcraft2_core.sc2_extension import StarCraft2Extension

                self.starcraft2_changeling_observer_extension = StarCraft2Extension()
                self.game_extension_registry.register(
                    self.starcraft2_changeling_observer_extension
                )
                registered_extensions.append(
                    self.starcraft2_changeling_observer_extension
                )
                log_print(
                    "[AppComposer] starcraft2 Changeling observer extension registered"
                )
            except Exception as e:
                log_print(
                    "[AppComposer] register StarCraft2 Changeling observer failed: "
                    f"{type(e).__name__}: {e}"
                )
                self.starcraft2_changeling_observer_extension = None

        if self.chess_plugin is not None and self.chess_game_extension is None:
            try:
                from app_core.extensions.chess_game_extension import (
                    ChessGameExtension,
                )

                self.chess_game_extension = ChessGameExtension(
                    plugin=self.chess_plugin,
                )
                self.game_extension_registry.register(self.chess_game_extension)
                registered_extensions.append(self.chess_game_extension)
                log_print("[AppComposer] chess game extension registered")
            except Exception as e:
                log_print(
                    "[AppComposer] register ChessGameExtension failed: "
                    f"{type(e).__name__}: {e}"
                )
                self.chess_game_extension = None

        if registered_extensions:
            self.game_extension_registry.initialize(self.game_extension_context)

        if self.starcraft116_game_extension is not None:
            starcraft116_lookup = (
                self.game_extension_registry.get("starcraft116") is not None
            )
            log_print(
                f"[AppComposer] registry lookup starcraft116: {starcraft116_lookup}"
            )
        if self.starcraft2_game_extension is not None:
            starcraft2_lookup = (
                self.game_extension_registry.get("starcraft2") is not None
            )
            log_print(f"[AppComposer] registry lookup starcraft2: {starcraft2_lookup}")
        if self.starcraft2_changeling_observer_extension is not None:
            starcraft2_observer_lookup = (
                self.game_extension_registry.get("starcraft2_changeling_observer")
                is not None
            )
            log_print(
                "[AppComposer] registry lookup starcraft2_changeling_observer: "
                f"{starcraft2_observer_lookup}"
            )
        if self.chess_game_extension is not None:
            chess_lookup = self.game_extension_registry.get("chess") is not None
            log_print(f"[AppComposer] registry lookup chess: {chess_lookup}")

    def create_component_ui(self):
        import gradio as gr
        from audio_device_manager import audio_device_manager

        self.input.create_ui()
        self.llm.create_ui()
        self.translate.create_ui()
        self.tts.create_ui()
        if self.song_player is not None:
            self.song_player.create_ui()#20260628_kpopmodder
        if self.chess_plugin is not None:
            self.chess_plugin.create_ui()#20260628_kpopmodder
        if self.starcraft_plugin is not None:
            self.starcraft_plugin.create_ui()#20260630_kpopmodder
        if self.starcraft116_plugin is not None:
            self.starcraft116_plugin.create_ui()#20260702_kpopmodder
        if self.starcraft2_plugin is not None:
            self.starcraft2_plugin.create_ui()#20260707_kpopmodder
        with gr.Tab("Setting"):#20260629_kpopmodder: Group operational settings under one top-level tab.#Setting 아래에 설정 GUI 생성
            with gr.Tabs():
                self.vtuber.create_ui()
                audio_device_manager.create_ui()#20260614_kpopmodder
                if self.screen_vision is not None:
                    self.screen_vision.create_ui()#20260620_kpopmodder

    def wire_event_listeners(self):
        self.input.add_output_event_listener(self.llm.receive_input)
        self.llm.add_output_event_listener(self.translate.receive_input)
        self.translate.add_output_event_listener(self.tts.receive_input)
        self.tts.add_output_event_listener(self.vtuber.receive_input)
        if self.song_player is not None:
            self.song_player.add_output_event_listener(
                self.vtuber.receive_input,
            )#20260628_kpopmodder
            self.song_player.add_expression_event_listener(
                self.vtuber.receive_song_expression
            )#20260628_kpopmodder
        if self.starcraft_plugin is not None:
            self.starcraft_plugin.add_output_event_listener(
                self.llm.receive_input,
            )#20260630_kpopmodder
            self.llm.add_output_event_listener(
                self.starcraft_plugin.receive_coach_response,
                full_response=True,
            )#20260630_kpopmodder

        if self.screen_vision is not None:
            self.screen_vision.add_output_event_listener(
                self.receive_screen_vision_input,
            )#20260620_kpopmodder

    def receive_screen_vision_input(self, text):#20260628_kpopmodder
        if global_state.get_value(GlobalKeys.IS_SONG_PLAYING, False):
            log_print(
                "[ScreenVision] song playing state blocked LLM input."
            )#20260628_kpopmodder
            return
        self.llm.receive_input(text)

    def build_managed_components(self):
        self.managed_components = [
            self.input,
            self.llm,
            self.translate,
            self.tts,
            self.vtuber,
        ]#20260623_kpopmodder
        self.core_components = [
            self.input,
            self.llm,
            self.translate,
            self.tts,
            self.vtuber,
        ]
        if self.screen_vision is not None:
            self.managed_components.insert(0, self.screen_vision)#20260629_kpopmodder
        if self.song_player is not None:
            self.managed_components.insert(-1, self.song_player)#20260629_kpopmodder
        if self.starcraft_plugin is not None:
            self.managed_components.insert(-1, self.starcraft_plugin)#20260630_kpopmodder
        if self.starcraft116_game_extension is not None:
            self.optional_components.append(self.starcraft116_game_extension)
            self._register_startup_component(self.starcraft116_game_extension)
            self.managed_components.insert(-1, self.starcraft116_game_extension)
        if self.starcraft2_game_extension is not None:
            self.optional_components.append(self.starcraft2_game_extension)
            self._register_startup_component(self.starcraft2_game_extension)
            self.managed_components.insert(-1, self.starcraft2_game_extension)
        if self.starcraft2_changeling_observer_extension is not None:
            self.optional_components.append(
                self.starcraft2_changeling_observer_extension
            )
            self._register_startup_component(
                self.starcraft2_changeling_observer_extension
            )
            self.managed_components.insert(
                -1,
                self.starcraft2_changeling_observer_extension,
            )
        if self.chess_game_extension is not None:
            self.optional_components.append(self.chess_game_extension)
            self._register_startup_component(self.chess_game_extension)
            self.managed_components.insert(-1, self.chess_game_extension)

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
        atexit.register(self.runtime_lifecycle.shutdown)
        self.runtime_lifecycle.start_components()
        self.runtime_lifecycle.start_global_updates()

    def launch_gradio(self):
        gradio_host = "127.0.0.1"
        gradio_port = find_available_port(host=gradio_host, start_port=7860)
        #20260620_kpopmodder: Move to the next local port when Gradio's default port is already occupied.
        log_print(f"[Gradio] Starting at http://{gradio_host}:{gradio_port}/")
        #main_interface.queue().launch()#20260615_kpopmodder
        try:
            self.main_interface.queue().launch(#20260615_kpopmodder
                server_name=gradio_host,
                server_port=gradio_port,
                share=False,
                #show_api=False#20260616_kpopmodder
            )
        except KeyboardInterrupt:
            log_print("[Gradio] KeyboardInterrupt received; shutting down.")#20260630_kpopmodder
        finally:
            if self.runtime_lifecycle is not None:
                self.runtime_lifecycle.shutdown()
