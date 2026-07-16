#20260630_kpopmodder: Added a small lifecycle owner for shutdown and idle timer wiring.
import atexit
import threading
import traceback

from core.global_state import GlobalKeys, global_state
from core.logger import log_print


class RuntimeLifecycle:
    #20260630_kpopmodder: Keep shutdown/timer state together while preserving startup wiring in main.py.
    def __init__(
        self,
        managed_components,
        llm,
        translate,
        tts,
        song_player=None,
        global_state_instance=global_state,
        update_interval_sec=0.5,
        timer_factory=None,
        core_components=None,
        optional_components=None,
        shutdown_register=atexit.register,
    ):
        self.managed_components = list(managed_components)
        self.llm = llm
        self.translate = translate
        self.tts = tts
        self.song_player = song_player
        self.global_state = global_state_instance
        self.update_interval_sec = update_interval_sec
        self.timer_factory = timer_factory or threading.Timer
        self.global_update_loop = None
        self.core_components = list(core_components or [])
        self.optional_components = list(optional_components or [])
        self.app_shutdown_done = False#20260623_kpopmodder
        self._started_components = set()
        #20260717_kpopmodder: RuntimeLifecycle owns process-exit shutdown hook registration.
        self.shutdown_register = shutdown_register
        self._shutdown_registered = False
        self._runtime_started = False

    def start(self):
        #20260717_kpopmodder: Keep AppComposer from coordinating lifecycle sub-steps.
        if self._runtime_started:
            return

        self._register_shutdown_hook()
        self.start_components()
        self.start_global_updates()
        self._runtime_started = True

    def _register_shutdown_hook(self):
        if self._shutdown_registered:
            return
        if not callable(self.shutdown_register):
            return

        self.shutdown_register(self.shutdown)
        self._shutdown_registered = True

    def shutdown(self):
        if self.app_shutdown_done:
            log_print("[Shutdown] already in progress or completed.")#20260630_kpopmodder
            return

        self.app_shutdown_done = True
        log_print("[Shutdown] cleanup started.")#20260630_kpopmodder
        #20260623_kpopmodder: Shutdown owns event unsubscription and background loop cleanup.
        if self.global_update_loop is not None:
            try:
                self.global_update_loop.cancel()
            except KeyboardInterrupt:
                log_print("[Shutdown] global update loop cancel interrupted.")#20260630_kpopmodder
            except Exception as e:
                log_print(f"[Shutdown] global update loop cancel failed: {e}")

        for component in list(self.managed_components):
            shutdown = getattr(component, "shutdown", None)
            if not callable(shutdown):
                continue

            try:
                shutdown()
            except KeyboardInterrupt:
                log_print(
                    f"[Shutdown] component shutdown interrupted: "
                    f"{component.__class__.__name__}"
                )#20260630_kpopmodder
            except Exception as e:
                log_print(
                    f"[Shutdown] component shutdown failed: "
                    f"{component.__class__.__name__}: {e}"
                )
        log_print("[Shutdown] cleanup finished.")#20260630_kpopmodder

    def start_components(self):
        core_component_ids = set(map(id, self.core_components))
        optional_component_ids = set(map(id, self.optional_components))
        started_components = []

        for component in self.managed_components:
            required = self._get_component_start_mode(
                component,
                core_component_ids,
                optional_component_ids,
            )
            if required is None:
                continue
            self._start_component_with_strategy(
                component,
                required,
                started_components,
            )

    def _get_component_start_mode(
        self,
        component,
        core_component_ids,
        optional_component_ids,
    ):
        if self.core_components and self.optional_components:
            if id(component) in core_component_ids:
                return True
            if id(component) in optional_component_ids:
                return False
            return None

        if self.core_components:
            return id(component) in core_component_ids

        if self.optional_components:
            return id(component) not in optional_component_ids

        return True

    def _start_component_with_strategy(
        self,
        component,
        required,
        started_components,
    ):
        start = getattr(component, "start", None)
        if not callable(start):
            return
        if id(component) in self._started_components:
            return

        try:
            start()
            self._started_components.add(id(component))
            started_components.append(component)
        except KeyboardInterrupt:
            log_print(
                f"[Startup] component start interrupted: "
                f"{component.__class__.__name__}"
            )
            if required:
                self._rollback_startup(started_components)
                raise
            self._started_components.discard(id(component))
        except Exception as e:
            trace = traceback.format_exc().strip()
            log_print(
                "[Startup] component start failed with traceback:\n"
                f"{trace}"
            )
            log_print(
                f"[Startup] component start failed: "
                f"{component.__class__.__name__}: {type(e).__name__}: {e}"
            )
            if required:
                self._rollback_startup(started_components)
                raise
            self._started_components.discard(id(component))

    def _rollback_startup(self, started_components):
        self.app_shutdown_done = True
        for component in reversed(started_components):
            self._safe_shutdown_component(
                component,
                "[Startup] startup rollback",
            )
            self._started_components.discard(id(component))
        started_components.clear()

    def _safe_shutdown_component(self, component, prefix):
        shutdown = getattr(component, "shutdown", None)
        if not callable(shutdown):
            return

        try:
            shutdown()
        except KeyboardInterrupt:
            log_print(
                f"{prefix} interrupted: "
                f"{component.__class__.__name__}"
            )
        except Exception as e:
            log_print(
                f"{prefix} component shutdown failed: "
                f"{component.__class__.__name__}: {e}"
            )

    def start_global_updates(self):
        try:
            self.update_globals_periodic()
        except KeyboardInterrupt:
            log_print("Stopping...")#20260612_kpopmodder
            if self.global_update_loop:
                self.global_update_loop.cancel()

    # def update_globals_periodic():
    #     global_state.set_value(GlobalKeys.IS_IDLE, llm.input_queue.empty() and translate.input_queue.empty() and tts.input_queue.empty() and tts.audio_data_queue.empty())
    #     #log_print(f"global_state.get_value(GlobalKeys.IS_IDLE) {global_state.get_value(GlobalKeys.IS_IDLE)}")#20260612_kpopmodder
    #     global global_update_loop
    #     global_update_loop = threading.Timer(0.5, update_globals_periodic).start()

    def update_globals_periodic(self):#20260615_kpopmodder
        if self.app_shutdown_done:#20260627_kpopmodder: Do not recreate the Timer after shutdown() cancels it.
            return

        self.global_state.set_value(
            GlobalKeys.IS_IDLE,
            self.llm.input_queue.empty()
            and self.translate.input_queue.empty()
            and self.tts.input_queue.empty()
            and not (
                self.song_player is not None
                and self.song_player.is_playing()
            )
            #and tts.audio_data_queue.empty()#20260616_kpopmodder
        )

        if self.app_shutdown_done:#20260627_kpopmodder: Guard the re-register race during shutdown.
            return

        self.global_update_loop = self.timer_factory(
            self.update_interval_sec,
            self.update_globals_periodic,
        )
        self.global_update_loop.daemon = True
        self.global_update_loop.start()
