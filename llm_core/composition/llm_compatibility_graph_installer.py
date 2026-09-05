#20260905_kpopmodder: Sequence focused component graphs behind the LLM facade.
from __future__ import annotations

from .generation_output import LlmGenerationOutputComponentGraph
from .speech_content import LlmSpeechContentComponentGraph
from .trusted_ingress import LlmTrustedIngressComponentGraph
from .ui_lifecycle import LlmUiLifecycleComponentGraph


class LlmCompatibilityGraphInstaller:
    def __init__(
        self,
        facade,
        *,
        create_plugin_selection_ui_callback,
        create_plugin_ui_callback,
        base_shutdown_callback,
    ) -> None:
        callbacks = (
            create_plugin_selection_ui_callback,
            create_plugin_ui_callback,
            base_shutdown_callback,
        )
        if not all(callable(callback) for callback in callbacks):
            raise TypeError("LLM compatibility callbacks must be callable")
        self._facade = facade
        self._speech_content_graph = LlmSpeechContentComponentGraph(facade)
        self._trusted_ingress_graph = LlmTrustedIngressComponentGraph(facade)
        self._generation_output_graph = LlmGenerationOutputComponentGraph(
            facade,
            trusted_ingress_graph_callback=(
                self.ensure_trusted_ingress_graph
            ),
        )
        self._ui_lifecycle_graph = LlmUiLifecycleComponentGraph(
            facade,
            create_plugin_selection_ui_callback=(
                create_plugin_selection_ui_callback
            ),
            create_plugin_ui_callback=create_plugin_ui_callback,
            base_shutdown_callback=base_shutdown_callback,
            output_listener_registry_callback=(
                self.ensure_output_listener_registry
            ),
        )

    def install(
        self,
        *,
        memory_context_builder=None,
        memory_command_handler=None,
        screen_question_router=None,
    ) -> None:
        facade = self._facade
        facade.memory_context_builder = memory_context_builder
        facade.memory_command_handler = memory_command_handler
        facade.screen_question_router = screen_question_router
        facade.history = []

        self._speech_content_graph.install()
        self._ui_lifecycle_graph.install_display_foundations()
        self._generation_output_graph.install(
            memory_context_builder=memory_context_builder,
            memory_command_handler=memory_command_handler,
            screen_question_router=screen_question_router,
        )
        self._trusted_ingress_graph.install_trusted_inputs()
        self._generation_output_graph.ensure_routed_external_response_publisher()
        self._trusted_ingress_graph.install_routing()
        self._ui_lifecycle_graph.install_runtime()

    def ensure_speech_style_helper(self):
        return self._speech_content_graph.ensure_speech_style_helper()

    def ensure_speech_style_state_controller(self):
        return self._speech_content_graph.ensure_speech_style_state_controller()

    def ensure_speech_style_persistence(self):
        return self._speech_content_graph.ensure_speech_style_persistence()

    def ensure_speech_style_update_coordinator(self):
        return (
            self._speech_content_graph.ensure_speech_style_update_coordinator()
        )

    def ensure_effective_system_prompt_builder(self):
        return (
            self._speech_content_graph.ensure_effective_system_prompt_builder()
        )

    def ensure_content_repository(self):
        return self._speech_content_graph.ensure_content_repository()

    def ensure_generation_facade(self):
        return self._generation_output_graph.ensure_generation_facade()

    def ensure_output_listener_registry(self):
        return self._generation_output_graph.ensure_output_listener_registry()

    def ensure_chat_history_resetter(self):
        return self._ui_lifecycle_graph.ensure_chat_history_resetter()

    def ensure_ui_builder(self):
        return self._ui_lifecycle_graph.ensure_ui_builder()

    def ensure_input_event_normalizer(self):
        return self._trusted_ingress_graph.ensure_input_event_normalizer()

    def ensure_trusted_ingress_graph(self):
        return self._trusted_ingress_graph.ensure_trusted_ingress_graph()

    def sync_trusted_ingress_compatibility_fields(
        self,
        graph,
        *,
        include_local_chat=False,
    ) -> None:
        self._trusted_ingress_graph.sync_trusted_ingress_compatibility_fields(
            graph,
            include_local_chat=include_local_chat,
        )

    def ensure_local_chat_input_adapter(self):
        return self._trusted_ingress_graph.ensure_local_chat_input_adapter()

    def ensure_trusted_local_chat_input_dispatch_coordinator(self):
        return (
            self._trusted_ingress_graph
            .ensure_trusted_local_chat_input_dispatch_coordinator()
        )

    def ensure_local_chat_prediction_entrypoint(self):
        return (
            self._trusted_ingress_graph.ensure_local_chat_prediction_entrypoint()
        )

    def ensure_local_chat_interface_factory(self):
        return self._trusted_ingress_graph.ensure_local_chat_interface_factory()

    def set_input_router(self, router) -> None:
        self._trusted_ingress_graph.set_input_router(router)

    def ensure_routed_input_dispatch_coordinator(self):
        return (
            self._trusted_ingress_graph
            .ensure_routed_input_dispatch_coordinator()
        )

    def ensure_routed_external_response_publisher(self):
        return (
            self._generation_output_graph
            .ensure_routed_external_response_publisher()
        )

    def ensure_prediction_dispatch_coordinator(self):
        return (
            self._trusted_ingress_graph.ensure_prediction_dispatch_coordinator()
        )

    def ensure_text_only_generation_helper(self):
        return (
            self._generation_output_graph
            .ensure_text_only_generation_helper()
        )

    def ensure_input_queue_display_updater(self):
        return self._ui_lifecycle_graph.ensure_input_queue_display_updater()

    def ensure_runtime_lifecycle_coordinator(self):
        return (
            self._ui_lifecycle_graph.ensure_runtime_lifecycle_coordinator()
        )


__all__ = ("LlmCompatibilityGraphInstaller",)
