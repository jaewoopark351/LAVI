#20260717_kpopmodder: Moved LLM component implementation out of the removed root module.
#20260620_kpopmodder: Queue and response handling imports moved to llm_core helper classes.
#20260905_kpopmodder: Keeps LLM as the compatibility facade over focused collaborators.
# import inspect
# from queue import Queue
from types import MappingProxyType

from plugin_system.interfaces import LLMPluginInterface
from plugin_system.selection import PluginSelectionBase
#import os#20260616_kpopmodder
from llm_core.composition import LlmCompatibilityGraphInstaller

_ROUTED_WITHOUT_RESPONSE = object()


class LLM(PluginSelectionBase):
    # history = []#20260616_kpopmodder
    # input_queue = Queue()#20260616_kpopmodder
    # input_process_thread = None#20260616_kpopmodder
    # system_prompt_text = ""#20260616_kpopmodder
    # liveTextbox = LiveTextbox()#20260616_kpopmodder
    # process_queue_live_textbox = LiveTextbox()#20260616_kpopmodder
    
    remember_history = True
    speech_style_default = "polite"#20260629_kpopmodder
    speech_style_labels = MappingProxyType({#20260629_kpopmodder: Keep config values ASCII while showing Korean UI labels.
        "polite": "존댓말",
        "casual": "반말",
    })
    speech_style_prompts = MappingProxyType({
        "polite": (
            "말투 규칙: 이 지시는 캐릭터 프롬프트의 기본 말투와 예시보다 우선합니다. "
            "사용자에게 자연스럽고 공손한 존댓말로 답하세요. "
            "친근함은 유지하되 반말은 쓰지 마세요. "
            "캐릭터 프롬프트에 반말 예시가 있어도 성격만 참고하고 문체는 존댓말로 바꾸세요. "
            "'-야', '-어', '-지', '-냐', '-해?' 같은 반말 종결을 쓰지 말고 "
            "'-요', '-습니다', '-세요', '-네요' 같은 존댓말 종결을 사용하세요."
        ),
        "casual": (
            "말투 규칙: 이 지시는 캐릭터 프롬프트의 기본 말투와 예시보다 우선합니다. "
            "사용자에게 친근한 반말로 답하세요. "
            "무례하거나 공격적으로 말하지 말고, 너무 과한 장난은 피하세요."
        ),
    })

    #20260629_kpopmodder: Keep runtime-linked abilities in the final system prompt so OpenAI providers answer as LAV, not generic OpenAI.
    runtime_ability_prompt = (
        "- ScreenVision으로 화면을 확인할 수 있습니다. 화면 관련 질문에는 \"볼 수 있다/볼 수 없다\"로 답하지 말고, \"화면을 관찰 후 말해줄 수 있습니다\"라고 답합니다.\n"
        "- 나는 준비된 노래만 할 수 있습니다. 준비 안 된 노래는 못합니다.\n"
        "- 대화와 ScreenVision 화면 관찰은 기억 컨텍스트로 저장/회상할 수 있습니다. \"기억해줘\" 요청에는 불가능하다고 답하지 말고, 확인된 내용만 기억하겠다고 짧게 답합니다."
    )

    # def __init__(self) -> None:#20260616_kpopmodder
    #     super().__init__(LLMPluginInterface)

    #     self.output_event_listeners = []
    #     self.full_output_event_listeners = []
    #     self.context_file_path = "ai_character_system_prompt.txt"
    #     self.LLM_output = ""
        
    #     self.history = []
    #     # Check if the file exists. If not, create an empty file.
    #     if not os.path.exists(self.context_file_path):
    #         with open(self.context_file_path, 'w') as file:
    #             file.write('')

    def __init__(
        self,
        memory_context_builder=None,
        memory_command_handler=None,
        screen_question_router=None,#20260628_kpopmodder
    ) -> None:#20260621_kpopmodder
        super().__init__(LLMPluginInterface)
        self._get_compatibility_graph_installer().install(
            memory_context_builder=memory_context_builder,
            memory_command_handler=memory_command_handler,
            screen_question_router=screen_question_router,
        )

    def _get_compatibility_graph_installer(self):
        installer = getattr(self, "compatibility_graph_installer", None)
        if installer is None:
            installer = LlmCompatibilityGraphInstaller(
                self,
                create_plugin_selection_ui_callback=(
                    super().create_plugin_selection_ui
                ),
                create_plugin_ui_callback=super().create_plugin_ui,
                base_shutdown_callback=super().shutdown,
            )
            self.compatibility_graph_installer = installer
        return installer

    def _get_ui_builder(self):
        return self._get_compatibility_graph_installer().ensure_ui_builder()

    def _get_chat_history_resetter(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_chat_history_resetter()
        )

    def _get_speech_style_state_controller(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_speech_style_state_controller()
        )

    def _get_speech_style_update_coordinator(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_speech_style_update_coordinator()
        )

    def _get_effective_system_prompt_builder(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_effective_system_prompt_builder()
        )

    def _get_content_repository(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_content_repository()
        )

    def _get_generation_facade(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_generation_facade()
        )

    def _get_output_listener_registry(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_output_listener_registry()
        )

    @property
    def input_queue(self):
        return self.input_queue_worker.input_queue

    @input_queue.setter
    def input_queue(self, value):
        self.input_queue_worker.input_queue = value

    @property
    def input_process_thread(self):
        return self.input_queue_worker.input_process_thread

    @input_process_thread.setter
    def input_process_thread(self, value):
        self.input_queue_worker.input_process_thread = value

    @property
    def input_queue_lock(self):
        return self.input_queue_worker.input_queue_lock

    @input_queue_lock.setter
    def input_queue_lock(self, value):
        self.input_queue_worker.input_queue_lock = value

    @property
    def output_event_listeners(self):
        return self._get_output_listener_registry().output_event_listeners

    @output_event_listeners.setter
    def output_event_listeners(self, value):
        self._get_output_listener_registry().output_event_listeners = value

    @property
    def full_output_event_listeners(self):
        return self._get_output_listener_registry().full_output_event_listeners

    @full_output_event_listeners.setter
    def full_output_event_listeners(self, value):
        self._get_output_listener_registry().full_output_event_listeners = value

    @property
    def LLM_output(self):
        return self._get_generation_facade().llm_output

    @LLM_output.setter
    def LLM_output(self, value):
        self._get_generation_facade().llm_output = value

    @property
    def start_of_response(self):
        return self._get_generation_facade().start_of_response

    @start_of_response.setter
    def start_of_response(self, value):
        self._get_generation_facade().start_of_response = value

    def create_ui(self):
        return self._get_ui_builder().build()

    def reset_chat(self):
        return self._get_chat_history_resetter().reset()

    def _get_speech_style_helper(self):#20260705_kpopmodder
        return self._get_compatibility_graph_installer().ensure_speech_style_helper()

    def normalize_speech_style(self, value):#20260629_kpopmodder
        return self._get_speech_style_state_controller().normalize(value)

    def get_speech_style_label(self):#20260629_kpopmodder
        return self._get_speech_style_state_controller().current_label()

    def update_speech_style_mode(self, value):#20260629_kpopmodder
        self._get_speech_style_update_coordinator().update(value)

    def build_effective_system_prompt(self, system_prompt=None):#20260629_kpopmodder
        return self._get_effective_system_prompt_builder().build(system_prompt)

    def is_generator(self):
        return self._get_generation_facade().is_generator()
        #20260620_kpopmodder: Generator detection moved to LLMResponsePipeline.
        # return inspect.isgeneratorfunction(self.current_plugin.predict)

    def predict_wrapper(
        self,
        message,
        history,
        system_prompt,
        *,
        trusted_ingress_evidence=None,
    ):
        yield from self._get_prediction_dispatch_coordinator().predict(
            message,
            history,
            system_prompt,
            trusted_ingress_evidence=trusted_ingress_evidence,
        )

    def _get_prediction_dispatch_coordinator(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_prediction_dispatch_coordinator()
        )

    def _get_input_event_normalizer(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_input_event_normalizer()
        )

    def _get_local_chat_input_adapter(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_local_chat_input_adapter()
        )

    def _get_trusted_ingress_graph(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_trusted_ingress_graph()
        )

    def _sync_trusted_ingress_compatibility_fields(
        self,
        graph,
        *,
        include_local_chat=False,
    ):
        return (
            self._get_compatibility_graph_installer()
            .sync_trusted_ingress_compatibility_fields(
                graph,
                include_local_chat=include_local_chat,
            )
        )

    def _get_trusted_user_input_ingress_claim_registry(self):
        return self._get_trusted_ingress_graph().registry

    def validate_trusted_consumed_ingress_evidence(self, event, evidence):
        return self._get_trusted_ingress_graph().validate_consumed_evidence(
            event,
            evidence,
        )

    def _get_trusted_ingress_producer_registrar_factory(self):
        return self._get_trusted_ingress_graph().producer_registrar_factory

    def _get_trusted_local_chat_input_dispatch_coordinator(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_trusted_local_chat_input_dispatch_coordinator()
        )

    def _get_local_chat_prediction_entrypoint(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_local_chat_prediction_entrypoint()
        )

    def _get_local_chat_interface_factory(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_local_chat_interface_factory()
        )

        #20260620_kpopmodder: Response handling moved to LLMResponsePipeline.
        # log_print(f"history: {history}")#20260612_kpopmodder
        # determine if predict function is generator and sends output to other modules
        
        # self.start_of_response = True
        # self.LLM_output = ""#20260614_kpopmodder
        # self.liveTextbox.print(f"Input: {message}")
        # result = self.current_plugin.predict(message, history, system_prompt)
        # self.liveTextbox.print(f"AI: ")
        # if self.is_generator():#20260614_kpopmodder
        #     processed_idx = 0
        #     for output in result:
        #         self.LLM_output = output
        #         if self.is_sentence_end(self.LLM_output):
        #             self.send_output(self.LLM_output[processed_idx:])
        #             self.liveTextbox.print(
        #                 self.LLM_output[processed_idx:], append_to_last=True)
        #             processed_idx = len(self.LLM_output)
        #         yield output
        #     if not processed_idx == len(self.LLM_output):
        #         # send any remaining output
        #         self.send_output(self.LLM_output[processed_idx:])
        #         self.liveTextbox.print(
        #             self.LLM_output[processed_idx:], append_to_last=True)
        # if self.is_generator():  # 20260614_kpopmodder
        #     for output in result:
        #         if output is None:
        #             continue

        #         self.LLM_output = output
        #         yield output

        #     self.LLM_output = self.LLM_output.strip()

        #     if self.LLM_output:
        #         log_print(f"response: {self.LLM_output}")  # 20260614_kpopmodder
        #         self.send_output(self.LLM_output)
        #         self.liveTextbox.print(self.LLM_output, append_to_last=True)

        # if self.is_generator():  # 20260615_kpopmodder
        #     processed_idx = 0

        #     for output in result:
        #         if output is None:
        #             continue

        #         self.LLM_output = output
        #         yield output

        #         chunk, processed_idx = self.get_streaming_tts_chunk(
        #             self.LLM_output,
        #             processed_idx
        #         )

        #         if chunk:
        #             log_print(f"[LLM streaming chunk] {chunk}")
        #             self.send_output(chunk)

        #     self.LLM_output = self.LLM_output.strip()

        #     if self.LLM_output:
        #         remaining = self.LLM_output[processed_idx:].strip()

        #         if remaining:
        #             log_print(f"[LLM streaming remaining] {remaining}")
        #             self.send_output(remaining)

        #         log_print(f"response: {self.LLM_output}")# 20260615_kpopmodder
        #         self.liveTextbox.print(self.LLM_output, append_to_last=True)# 20260615_kpopmodder
        # else:#20260614_kpopmodder
        #     self.LLM_output = result or ""
        #     self.LLM_output = self.LLM_output.strip()

        #     if self.LLM_output:
        #         log_print(f"response: {self.LLM_output}")  # 20260614_kpopmodder
        #         self.send_output(self.LLM_output)
        #         self.liveTextbox.print(self.LLM_output, append_to_last=True)
        #         yield self.LLM_output#20260615_kpopmodder

        #     #return self.LLM_output#20260615_kpopmodder
        # else:
        #     self.LLM_output = result
        #     self.send_output(result)
        #     self.liveTextbox.print(result, append_to_last=True)
        #     return result
        # self.send_full_output(self.LLM_output)
        # if self.remember_history:
        #     self.history.append([message, self.LLM_output])

        #return self.LLM_output#20260615_kpopmodder

    def _get_text_only_generation_helper(self):#20260705_kpopmodder
        return (
            self._get_compatibility_graph_installer()
            .ensure_text_only_generation_helper()
        )

    def generate_text_only(
        self,
        message,
        system_prompt,
        preferred_provider_name=None,
    ):#20260630_kpopmodder: Chess reactions reuse the selected LLM without dispatching TTS/listener events.
        #20260705_kpopmodder: Preserve public method and delegate provider/output details to llm_core.
        return self._get_generation_facade().generate_text_only(
            message,
            system_prompt,
            preferred_provider_name=preferred_provider_name,
        )

    def _collect_text_only_generator_output(self, result):#20260630_kpopmodder: Collect delta/snapshot streams safely.
        #20260705_kpopmodder: Keep compatibility wrapper for tests or plugins using this private helper.
        return self._get_generation_facade().collect_text_only_generator_output(
            result
        )

    def load_content(self):#20260617_kpopmodder
        return self._get_content_repository().load()


    def update_file(self, new_content):#20260617_kpopmodder
        self._get_content_repository().update(new_content)

    # def load_content(self):#20260617_kpopmodder
    #     with open(self.context_file_path, 'r', encoding='utf-8') as file:
    #         content = file.read()
    #         self.system_prompt_text = content
    #         return content

    # def update_file(self, new_content):#20260617_kpopmodder
    #     self.context = new_content
    #     with open(self.context_file_path, 'w', encoding='utf-8') as file:
    #         file.write(new_content)
    #     self.system_prompt_text = new_content

    def send_output(self, output):
        self._get_output_listener_registry().send_output(output)
        #20260620_kpopmodder: Output listener dispatch moved to LLMEventDispatcher.
        # for subcriber in self.output_event_listeners:
        #     subcriber(output)
    
    def send_full_output(self, output):
        self._get_output_listener_registry().send_full_output(output)
        #20260620_kpopmodder: Full-response dispatch moved to LLMEventDispatcher.
        # for subcriber in self.full_output_event_listeners:
        #     subcriber(output)

    def receive_input(self, text):#20260617_kpopmodder
        self.input_queue_worker.receive_input(text)

        #20260620_kpopmodder: Queue insertion and worker startup moved to LLMInputQueueWorker.
        # with self.input_queue_lock:
        #     self.input_queue.put(text)

        # self.update_process_queue_textbox()
        # self.process_input_queue()

    # def receive_input(self, text):#20260617_kpopmodder
    #     self.input_queue.put(text)
    #     self.process_input_queue()

    @property
    def claim_aware_input_queue_sink(self):
        return self.input_queue_worker.claim_aware_input_queue_sink

    def create_trusted_voice_input_final_enqueue_coordinator(
        self,
        input_event_adapter,
        *,
        pre_accept_observers=(),
        post_accept_observers=(),
    ):
        return self._get_trusted_ingress_graph().create_voice_final_enqueue_coordinator(
            input_event_adapter,
            pre_accept_observers=pre_accept_observers,
            post_accept_observers=post_accept_observers,
        )

    def accept_registered_input(self, delivery, history, system_prompt):
        yield from self._get_trusted_ingress_graph().accept_registered(
            delivery,
            history,
            system_prompt,
        )

    def accept_queued_input(self, queue_delivery, history, system_prompt):
        yield from self._get_trusted_ingress_graph().accept_queued(
            queue_delivery,
            history,
            system_prompt,
        )

    def _dispatch_trusted_ingress_lease(
        self,
        lease,
        history,
        system_prompt,
    ):
        yield from self._get_trusted_ingress_graph().dispatch_lease(
            lease,
            history,
            system_prompt,
        )

    def _get_routed_input_dispatch_coordinator(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_routed_input_dispatch_coordinator()
        )

    def _get_routed_external_response_publisher(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_routed_external_response_publisher()
        )

    def emit_external_response(
        self,
        text,
        *,
        source="minecraft_chatclef",
        send_output=True,
        send_full_output=False,
        remember_history=False,
        event_id=None,
        route_kind="minecraft_chatclef_external",
        response_kind="external",
    ):
        return self._get_routed_external_response_publisher().emit_external_response(
            text,
            source=source,
            send_output=send_output,
            send_full_output=send_full_output,
            remember_history=remember_history,
            event_id=event_id,
            route_kind=route_kind,
            response_kind=response_kind,
        )

    def add_output_event_listener(self, function, full_response = False):
        self._get_output_listener_registry().add(
            function,
            full_response=full_response
        )
        #20260620_kpopmodder: Listener registration moved to LLMEventDispatcher.
        # if full_response:
        #     self.full_output_event_listeners.append(function)
        # else:
        #     self.output_event_listeners.append(function)

    def remove_output_event_listener(self, function, full_response=False):
        return self._get_output_listener_registry().remove(
            function,
            full_response=full_response,
        )

    def set_input_router(self, router):#20260803_kpopmodder: Let app-owned routers intercept chat/mic commands before LLM generation.
        return self._get_compatibility_graph_installer().set_input_router(router)

    def _try_route_external_input(
        self,
        message,
        *,
        trusted_ingress_evidence=None,
    ):#20260803_kpopmodder
        outcome = self._get_routed_input_dispatch_coordinator().dispatch(
            message,
            trusted_ingress_evidence=trusted_ingress_evidence,
        )
        if not outcome.handled:
            return None
        if outcome.suppress_response:
            return _ROUTED_WITHOUT_RESPONSE
        return outcome.response

    def is_sentence_end(self, word):#20260617_kpopmodder
        return self.streaming_chunker.is_sentence_end(word)


    def get_streaming_tts_chunk(self, full_text, processed_idx):#20260617_kpopmodder
        return self.streaming_chunker.get_streaming_tts_chunk(
            full_text,
            processed_idx,
        )

    # # Check if the last character of the word is a sentence-ending punctuation for the given language#20260617_kpopmodder
    # def is_sentence_end(self, word):#20260617_kpopmodder
    #     sentence_end_punctuation = {'.', '?', '!', '。', '？', '！','\n'}
    #     if len(word) > 0:
    #         return word[-1] in sentence_end_punctuation
    #     else: return True

    # def get_streaming_tts_chunk(self, full_text, processed_idx):#20260617_kpopmodder
    #     if not full_text:
    #         return None, processed_idx

    #     sentence_end_punctuation = {'.', '?', '!', '。', '？', '！', '\n'}

    #     cut_idx = -1

    #     for i in range(processed_idx, len(full_text)):
    #         if full_text[i] in sentence_end_punctuation:
    #             cut_idx = i + 1
    #             break

    #     if cut_idx <= processed_idx:
    #         return None, processed_idx

    #     chunk = full_text[processed_idx:cut_idx].strip()

    #     if not chunk:
    #         return None, cut_idx

    #     return chunk, cut_idx

    def process_input_queue(self):#20260617_kpopmodder
        self.input_queue_worker.process_input_queue()
        return

        #20260620_kpopmodder: Input processing thread management moved to LLMInputQueueWorker.
        # if (
        #     self.input_process_thread is not None
        #     and self.input_process_thread.is_alive()
        # ):
        #     return

        # self.input_process_thread = threading.Thread(
        #     target=self.generate_response,
        # )
        # self.input_process_thread.daemon = True
        # self.input_process_thread.start()

    # def process_input_queue(self):#20260617_kpopmodder
    #     # Check if the current thread is alive
    #     if self.input_process_thread is None or not self.input_process_thread.is_alive():
    #         # Create and start a new thread
    #         self.input_process_thread = threading.Thread(
    #             target=self.generate_response)
    #         self.input_process_thread.start()

    def generate_response(self):#20260617_kpopmodder
        self.input_queue_worker.generate_response()
        return

        #20260620_kpopmodder: Sequential queue consumption moved to LLMInputQueueWorker.
        # while True:
        #     with self.input_queue_lock:
        #         if self.input_queue.empty():
        #             break

        #         next_input = self.input_queue.get()

        #     response_generator = self.predict_wrapper(
        #         next_input,
        #         self.history,
        #         self.system_prompt_text,
        #     )

        #     for _ in response_generator:
        #         pass

        #     self.update_process_queue_textbox()

    # def generate_response(self):#20260617_kpopmodder
    #     while (not self.input_queue.empty()):
    #         next_input = self.input_queue.get()
    #         response = self.predict_wrapper(
    #             next_input, self.history, self.system_prompt_text)
    #         if self.is_generator():
    #             for _ in response:
    #                 pass  # need to keep iterating the generator
    #                 self.process_queue_live_textbox.set(
    #                     LAV_utils.queue_to_list(self.input_queue))

    def _get_input_queue_display_updater(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_input_queue_display_updater()
        )

    def update_process_queue_textbox(self):#20260617_kpopmodder
        return self._get_input_queue_display_updater().update()

    def _get_runtime_lifecycle_coordinator(self):
        return (
            self._get_compatibility_graph_installer()
            .ensure_runtime_lifecycle_coordinator()
        )

    def handle_interrupt(self):#20260621_kpopmodder
        return self._get_runtime_lifecycle_coordinator().handle_interrupt()

    def shutdown(self):
        return self._get_runtime_lifecycle_coordinator().shutdown()
