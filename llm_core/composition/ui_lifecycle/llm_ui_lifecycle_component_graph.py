#20260905_kpopmodder: Own LLM UI, queue, and runtime-lifecycle binding.
from __future__ import annotations

from core.event_manager import EventType, event_manager
from llm_core.chat_session import LlmChatHistoryResetter
from llm_core.input_queue import LlmInputQueueDisplayUpdater
from llm_core.input_queue_worker import LLMInputQueueWorker
from llm_core.lifecycle import LlmRuntimeLifecycleCoordinator
from llm_core.ui import LlmChatUiBuilder
from ui_core.live_textbox import LiveTextbox


class LlmUiLifecycleComponentGraph:
    def __init__(
        self,
        facade,
        *,
        create_plugin_selection_ui_callback,
        create_plugin_ui_callback,
        base_shutdown_callback,
        output_listener_registry_callback,
    ) -> None:
        callbacks = (
            create_plugin_selection_ui_callback,
            create_plugin_ui_callback,
            base_shutdown_callback,
            output_listener_registry_callback,
        )
        if not all(callable(callback) for callback in callbacks):
            raise TypeError("LLM UI/lifecycle callbacks must be callable")
        self._facade = facade
        self._create_plugin_selection_ui_callback = (
            create_plugin_selection_ui_callback
        )
        self._create_plugin_ui_callback = create_plugin_ui_callback
        self._base_shutdown_callback = base_shutdown_callback
        self._output_listener_registry_callback = (
            output_listener_registry_callback
        )

    def install_display_foundations(self) -> None:
        facade = self._facade
        facade.liveTextbox = LiveTextbox()
        facade.process_queue_live_textbox = LiveTextbox()

    def install_runtime(self) -> None:
        facade = self._facade
        facade.input_queue_display_updater = LlmInputQueueDisplayUpdater(
            live_textbox=facade.process_queue_live_textbox,
            queue_callback=lambda: facade.input_queue,
        )
        facade.input_queue_worker = LLMInputQueueWorker(
            response_callback=facade.predict_wrapper,
            history_callback=lambda: facade.history,
            system_prompt_callback=lambda: facade.system_prompt_text,
            queue_updated_callback=facade.update_process_queue_textbox,
            queued_response_callback=facade.accept_queued_input,
        )
        facade._shutdown = False
        facade._interrupt_subscription = event_manager.subscribe(
            EventType.INTERRUPT,
            facade.handle_interrupt,
        )
        facade.runtime_lifecycle_coordinator = (
            self.ensure_runtime_lifecycle_coordinator()
        )
        self.ensure_chat_history_resetter()
        self.ensure_ui_builder()

    def ensure_chat_history_resetter(self):
        facade = self._facade
        resetter = getattr(facade, "chat_history_resetter", None)
        if resetter is None:
            resetter = LlmChatHistoryResetter(
                history_callback=lambda: facade.history,
                clear_pending_presentations_callback=(
                    facade._get_compatibility_graph_installer()
                    .ensure_ui_presentation_queue()
                    .clear
                ),
            )
            facade.chat_history_resetter = resetter
        return resetter

    def ensure_ui_builder(self):
        facade = self._facade
        builder = getattr(facade, "chat_ui_builder", None)
        if builder is None:
            builder = LlmChatUiBuilder(
                host=facade,
                create_plugin_selection_ui_callback=(
                    self._create_plugin_selection_ui_callback
                ),
                create_plugin_ui_callback=self._create_plugin_ui_callback,
                local_chat_interface_factory_callback=(
                    facade._get_local_chat_interface_factory
                ),
                load_content_callback=facade.load_content,
                update_content_callback=facade.update_file,
                speech_style_labels_callback=lambda: facade.speech_style_labels,
                speech_style_label_callback=facade.get_speech_style_label,
                update_speech_style_callback=facade.update_speech_style_mode,
                reset_history_callback=facade.reset_chat,
                live_textbox=facade.liveTextbox,
                queue_live_textbox=facade.process_queue_live_textbox,
                ui_presentation_queue_callback=(
                    facade._get_compatibility_graph_installer()
                    .ensure_ui_presentation_queue
                ),
            )
            facade.chat_ui_builder = builder
        return builder

    def ensure_input_queue_display_updater(self):
        facade = self._facade
        updater = getattr(facade, "input_queue_display_updater", None)
        if updater is None:
            updater = LlmInputQueueDisplayUpdater(
                live_textbox=facade.process_queue_live_textbox,
                queue_callback=lambda: facade.input_queue,
            )
            facade.input_queue_display_updater = updater
        return updater

    def ensure_runtime_lifecycle_coordinator(self):
        facade = self._facade
        coordinator = getattr(facade, "runtime_lifecycle_coordinator", None)
        if coordinator is None:
            coordinator = LlmRuntimeLifecycleCoordinator(
                clear_pending_inputs_callback=(
                    lambda: facade.input_queue_worker.clear_pending_inputs()
                ),
                request_interrupt_callback=(
                    lambda: facade.response_pipeline.request_interrupt()
                ),
                clear_listeners_callback=(
                    lambda: self._output_listener_registry_callback().clear()
                ),
                interrupt_message_callback=(
                    lambda: facade.liveTextbox.print(
                        "[LLM] Interrupt: cleared pending inputs."
                    )
                ),
                input_thread_callback=(
                    lambda: facade.input_queue_worker.input_process_thread
                ),
                shutdown_state_callback=(
                    lambda: getattr(facade, "_shutdown", False)
                ),
                mark_shutdown_callback=lambda: setattr(
                    facade,
                    "_shutdown",
                    True,
                ),
                interrupt_subscription_callback=(
                    lambda: getattr(facade, "_interrupt_subscription", None)
                ),
                clear_interrupt_subscription_callback=lambda: setattr(
                    facade,
                    "_interrupt_subscription",
                    None,
                ),
                base_shutdown_callback=self._base_shutdown_callback,
                clear_pending_presentations_callback=(
                    facade._get_compatibility_graph_installer()
                    .ensure_ui_presentation_queue()
                    .clear
                ),
            )
            facade.runtime_lifecycle_coordinator = coordinator
        return coordinator


__all__ = ("LlmUiLifecycleComponentGraph",)
