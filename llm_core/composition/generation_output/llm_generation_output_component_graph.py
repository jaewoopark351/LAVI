#20260905_kpopmodder: Own LLM generation-pipeline and output collaborator binding.
from __future__ import annotations

from core.logger import log_print
from llm_core.event_dispatcher import LLMEventDispatcher
from llm_core.generation import LlmGenerationFacade
from llm_core.output import LlmOutputListenerRegistry
from llm_core.response_pipeline import LLMResponsePipeline
from llm_core.routed_response import (
    RoutedExternalResponsePublisher,
    RoutedResponseUiPresentationQueue,
)
from llm_core.streaming_chunker import LLMStreamingChunker
from llm_core.text_only_generation import LLMTextOnlyGenerationHelper


class LlmGenerationOutputComponentGraph:
    def __init__(self, facade, *, trusted_ingress_graph_callback) -> None:
        if not callable(trusted_ingress_graph_callback):
            raise TypeError("trusted_ingress_graph_callback must be callable")
        self._facade = facade
        self._trusted_ingress_graph_callback = trusted_ingress_graph_callback

    def install(
        self,
        *,
        memory_context_builder=None,
        memory_command_handler=None,
        screen_question_router=None,
    ) -> None:
        facade = self._facade
        facade.streaming_chunker = LLMStreamingChunker()
        facade.event_dispatcher = LLMEventDispatcher()
        self.ensure_output_listener_registry()
        facade.response_pipeline = LLMResponsePipeline(
            current_plugin_callback=facade.get_current_plugin,
            send_output_callback=facade.send_output,
            send_full_output_callback=facade.send_full_output,
            history_callback=lambda: facade.history,
            remember_history_callback=lambda: facade.remember_history,
            live_textbox=facade.liveTextbox,
            streaming_chunker=facade.streaming_chunker,
            memory_context_builder=memory_context_builder,
            memory_command_handler=memory_command_handler,
            screen_question_router=screen_question_router,
        )
        self.ensure_generation_facade()
        facade.text_only_generation_helper = (
            self.ensure_text_only_generation_helper()
        )

    def ensure_generation_facade(self):
        facade = self._facade
        generation_facade = getattr(facade, "generation_facade", None)
        if generation_facade is None:
            generation_facade = LlmGenerationFacade(
                response_pipeline_callback=lambda: facade.response_pipeline,
                text_only_helper_callback=(
                    self.ensure_text_only_generation_helper
                ),
            )
            facade.generation_facade = generation_facade
        return generation_facade

    def ensure_output_listener_registry(self):
        facade = self._facade
        dispatcher = getattr(facade, "event_dispatcher", None)
        registry = getattr(facade, "output_listener_registry", None)
        if registry is None or registry.dispatcher is not dispatcher:
            registry = LlmOutputListenerRegistry(dispatcher)
            facade.event_dispatcher = registry.dispatcher
            facade.output_listener_registry = registry
        return registry

    def ensure_text_only_generation_helper(self):
        facade = self._facade
        helper = getattr(facade, "text_only_generation_helper", None)
        if helper is None:
            helper = LLMTextOnlyGenerationHelper(
                current_plugin_callback=facade.get_current_plugin,
                provider_list_callback=lambda: facade.provider_list,
                find_provider_callback=facade.find_provider_by_name,
                load_provider_callback=facade.load_provider,
                is_generator_plugin_callback=(
                    lambda plugin: facade.response_pipeline.is_generator_plugin(
                        plugin
                    )
                ),
                log_callback=log_print,
            )
            facade.text_only_generation_helper = helper
        return helper

    def ensure_routed_external_response_publisher(self):
        facade = self._facade
        ui_presentation_queue = self.ensure_ui_presentation_queue()
        publisher = getattr(
            facade,
            "routed_external_response_publisher",
            None,
        )
        if publisher is None:
            publisher = RoutedExternalResponsePublisher(
                begin_generation_callback=(
                    facade.response_pipeline.begin_response_generation
                ),
                build_output_payload_callback=(
                    facade.response_pipeline.build_stream_payload
                ),
                send_output_callback=facade.send_output,
                send_full_output_callback=facade.send_full_output,
                ui_presentation_callback=ui_presentation_queue.enqueue,
                emission_capability_consumer=(
                    self._trusted_ingress_graph_callback()
                    .registry.consume_routed_response_emission_capability
                ),
            )
            facade.routed_external_response_publisher = publisher
        return publisher

    def ensure_ui_presentation_queue(self):
        facade = self._facade
        presentation_queue = getattr(
            facade,
            "routed_response_ui_presentation_queue",
            None,
        )
        if presentation_queue is None:
            presentation_queue = RoutedResponseUiPresentationQueue()
            facade.routed_response_ui_presentation_queue = presentation_queue
        return presentation_queue


__all__ = ("LlmGenerationOutputComponentGraph",)
