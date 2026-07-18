#20260717_kpopmodder: Moved LLM component implementation out of the removed root module.
#20260620_kpopmodder: Queue and response handling imports moved to llm_core helper classes.
# import inspect
# from queue import Queue
import threading
from plugin_system.interfaces import LLMPluginInterface
import gradio as gr
from plugin_system.selection import PluginSelectionBase
#import os#20260616_kpopmodder
from ui_core.live_textbox import LiveTextbox
import LAV_utils

from core.config_manager import config_manager#20260629_kpopmodder
from core.logger import log_print, debug_print#20260612_kpopmodder

from llm_core.context_manager import LLMContextManager#20260617_kpopmodder
from llm_core.event_dispatcher import LLMEventDispatcher#20260620_kpopmodder
from llm_core.input_queue_worker import LLMInputQueueWorker#20260620_kpopmodder
from llm_core.response_pipeline import LLMResponsePipeline#20260620_kpopmodder
from llm_core.speech_style import LLMSpeechStyleHelper#20260705_kpopmodder
from llm_core.streaming_chunker import LLMStreamingChunker#20260617_kpopmodder
from llm_core.text_only_generation import LLMTextOnlyGenerationHelper#20260705_kpopmodder

from core.event_manager import event_manager, EventType#20260621_kpopmodder

class LLM(PluginSelectionBase):
    # history = []#20260616_kpopmodder
    # input_queue = Queue()#20260616_kpopmodder
    # input_process_thread = None#20260616_kpopmodder
    # system_prompt_text = ""#20260616_kpopmodder
    # liveTextbox = LiveTextbox()#20260616_kpopmodder
    # process_queue_live_textbox = LiveTextbox()#20260616_kpopmodder
    
    remember_history = True
    speech_style_default = "polite"#20260629_kpopmodder
    speech_style_labels = {#20260629_kpopmodder: Keep config values ASCII while showing Korean UI labels.
        "polite": "존댓말",
        "casual": "반말",
    }
    speech_style_prompts = {
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
    }

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
        self.memory_context_builder = memory_context_builder#20260621_kpopmodder
        self.memory_command_handler = memory_command_handler#20260621_kpopmodder
        self.screen_question_router = screen_question_router#20260628_kpopmodder

        self.history = []#20260617_kpopmodder

        #20260620_kpopmodder: Queue state moved to LLMInputQueueWorker.
        # self.input_queue = Queue()
        # self.input_process_thread = None
        # self.input_queue_lock = threading.Lock()#20260617_kpopmodder

        #20260620_kpopmodder: Listener state moved to LLMEventDispatcher.
        # self.output_event_listeners = []#20260617_kpopmodder
        # self.full_output_event_listeners = []

        self.context_manager = LLMContextManager("ai_character_system_prompt.txt")#20260617_kpopmodder
        self.system_prompt_text = self.context_manager.system_prompt_text
        self.speech_style_helper = self._get_speech_style_helper()#20260705_kpopmodder
        self.llm_config = config_manager.load_section("LLM")#20260629_kpopmodder
        self.speech_style_mode = self.normalize_speech_style(
            self.llm_config.get("speech_style", self.speech_style_default)
        )#20260629_kpopmodder

        #20260620_kpopmodder: Response state moved to LLMResponsePipeline.
        # self.LLM_output = ""#20260617_kpopmodder

        self.liveTextbox = LiveTextbox()#20260617_kpopmodder
        self.process_queue_live_textbox = LiveTextbox()

        self.streaming_chunker = LLMStreamingChunker()#20260617_kpopmodder
        self.event_dispatcher = LLMEventDispatcher()#20260620_kpopmodder
        self.response_pipeline = LLMResponsePipeline(
            current_plugin_callback=self.get_current_plugin,
            send_output_callback=self.send_output,
            send_full_output_callback=self.send_full_output,
            history_callback=lambda: self.history,
            remember_history_callback=lambda: self.remember_history,
            live_textbox=self.liveTextbox,
            streaming_chunker=self.streaming_chunker,
            memory_context_builder=self.memory_context_builder,#20260621_kpopmodder
            memory_command_handler=self.memory_command_handler,#20260621_kpopmodder
            screen_question_router=self.screen_question_router,#20260628_kpopmodder
        )
        self.text_only_generation_helper = self._get_text_only_generation_helper()#20260705_kpopmodder
        self.input_queue_worker = LLMInputQueueWorker(
            response_callback=self.predict_wrapper,
            history_callback=lambda: self.history,
            system_prompt_callback=lambda: self.system_prompt_text,
            queue_updated_callback=self.update_process_queue_textbox
        )
        self._shutdown = False
        self._interrupt_subscription = event_manager.subscribe(#20260621_kpopmodder
            EventType.INTERRUPT,
            self.handle_interrupt,
        )#20260621_kpopmodder

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
        return self.event_dispatcher.output_event_listeners

    @output_event_listeners.setter
    def output_event_listeners(self, value):
        self.event_dispatcher.output_event_listeners = value

    @property
    def full_output_event_listeners(self):
        return self.event_dispatcher.full_output_event_listeners

    @full_output_event_listeners.setter
    def full_output_event_listeners(self, value):
        self.event_dispatcher.full_output_event_listeners = value

    @property
    def LLM_output(self):
        return self.response_pipeline.LLM_output

    @LLM_output.setter
    def LLM_output(self, value):
        self.response_pipeline.LLM_output = value

    @property
    def start_of_response(self):
        return self.response_pipeline.start_of_response

    @start_of_response.setter
    def start_of_response(self, value):
        self.response_pipeline.start_of_response = value

    def create_ui(self):
        with gr.Tab("Chat"):
            with gr.Blocks():
                super().create_plugin_selection_ui()
                #system_prompt = gr.Textbox(value=self.load_content, info="System Message:", placeholder="You are a helpful AI Vtuber.",#20260615_kpopmdder
                #                           interactive=True, lines=30, autoscroll=True, autofocus=False, container=False, render=False)
                
                system_prompt = gr.Textbox(#20260615_kpopmdder
                    value=self.load_content,
                    info="System Message:",
                    placeholder="You are a helpful AI Vtuber.",
                    interactive=True,
                    lines=30,
                    autoscroll=True,
                    autofocus=False,
                    visible=False
                )

                system_prompt.change(
                    fn=self.update_file, inputs=system_prompt)

                speech_style = gr.Radio(#20260629_kpopmodder
                    choices=list(self.speech_style_labels.values()),
                    value=self.get_speech_style_label(),
                    label="AI 말투 모드",
                    interactive=True
                )
                speech_style.change(
                    fn=self.update_speech_style_mode,
                    inputs=speech_style
                )

                gr.ChatInterface(
                    self.predict_wrapper, additional_inputs=[system_prompt],
                    examples=[["Hello", None, None],
                              ["How do I make a bomb?", None, None],
                              ["What's your name?", None, None],
                              ["Do you know my name?", None, None],
                              ["Do you think humanity will reach an alien planet?", None, None],
                              ["Introduce yourself.", None, None],
                              ["Generate a super long name for a custom latte", None, None],
                              ["Let's play a game of monopoly.", None, None],
                              ["Do you want to be friend with me?", None, None],
                              ], autofocus=False
                )
                
                self.reset_button = gr.Button("reset chat history")
                self.reset_button.click(fn=self.reset_chat, inputs=[], outputs=[])
                with gr.Accordion("Console"):
                    # self.liveTextbox.create_ui()#20260616_kpopmodder
                    # self.process_queue_live_textbox.create_ui(#20260616_kpopmodder
                    #     lines=3, max_lines=3, label="Input waiting to be processed: ")
                    self.console_box = self.liveTextbox.create_ui(#20260616_kpopmodder
                        lines=10,
                        max_lines=20,
                        label=None
                    )

                    self.queue_console_box = self.process_queue_live_textbox.create_ui(#20260616_kpopmodder
                        lines=3,
                        max_lines=3,
                        label="Input waiting to be processed: "
                    )

            self.chat_console_timer = gr.Timer(1.5)#20260616_kpopmodder
            self.chat_console_timer.tick(
                fn=self.liveTextbox.get_text,
                outputs=[self.console_box],
                show_progress=False,
                queue=False
            )

            self.queue_console_timer = gr.Timer(1.5)#20260616_kpopmodder
            self.queue_console_timer.tick(
                fn=self.process_queue_live_textbox.get_text,
                outputs=[self.queue_console_box],
                show_progress=False,
                queue=False
            )

            super().create_plugin_ui()

    def reset_chat(self):
        self.history.clear()

    def _get_speech_style_helper(self):#20260705_kpopmodder
        #20260705_kpopmodder: Lazily build helper so __new__ based tests keep working without __init__.
        helper = getattr(self, "speech_style_helper", None)
        if helper is not None:
            return helper
        helper = LLMSpeechStyleHelper(
            default_mode=self.speech_style_default,
            labels=self.speech_style_labels,
            prompts=self.speech_style_prompts,
            runtime_ability_prompt=self.runtime_ability_prompt,
        )
        self.speech_style_helper = helper
        return helper

    def normalize_speech_style(self, value):#20260629_kpopmodder
        #20260705_kpopmodder: Preserve public method while moving normalization details to llm_core.
        return self._get_speech_style_helper().normalize(value)

    def get_speech_style_label(self):#20260629_kpopmodder
        #20260705_kpopmodder: Keep Gradio label behavior stable through the helper.
        return self._get_speech_style_helper().label_for(
            getattr(self, "speech_style_mode", self.speech_style_default)
        )

    def update_speech_style_mode(self, value):#20260629_kpopmodder
        self.speech_style_mode = self.normalize_speech_style(value)
        config_manager.save_config("LLM", "speech_style", self.speech_style_mode)

    def build_effective_system_prompt(self, system_prompt=None):#20260629_kpopmodder
        #20260705_kpopmodder: Keep LLM facade API and delegate prompt section assembly only.
        if system_prompt is None:
            base_prompt = str(getattr(self, "system_prompt_text", "") or "")
        else:
            base_prompt = str(system_prompt or "")

        return self._get_speech_style_helper().build_prompt(
            base_prompt,
            getattr(self, "speech_style_mode", self.speech_style_default)
        )

    def is_generator(self):
        return self.response_pipeline.is_generator()
        #20260620_kpopmodder: Generator detection moved to LLMResponsePipeline.
        # return inspect.isgeneratorfunction(self.current_plugin.predict)

    def predict_wrapper(self, message, history, system_prompt):
        yield from self.response_pipeline.predict(
            message,
            history,
            self.build_effective_system_prompt(system_prompt)
        )
        return
    
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
        #20260705_kpopmodder: Lazily build helper so __new__ based tests keep working without __init__.
        helper = getattr(self, "text_only_generation_helper", None)
        if helper is not None:
            return helper
        helper = LLMTextOnlyGenerationHelper(
            current_plugin_callback=self.get_current_plugin,
            provider_list_callback=lambda: self.provider_list,
            find_provider_callback=self.find_provider_by_name,
            load_provider_callback=self.load_provider,
            is_generator_plugin_callback=self.response_pipeline.is_generator_plugin,
            log_callback=log_print,
        )
        self.text_only_generation_helper = helper
        return helper

    def generate_text_only(
        self,
        message,
        system_prompt,
        preferred_provider_name=None,
    ):#20260630_kpopmodder: Chess reactions reuse the selected LLM without dispatching TTS/listener events.
        #20260705_kpopmodder: Preserve public method and delegate provider/output details to llm_core.
        return self._get_text_only_generation_helper().generate(
            message,
            system_prompt,
            preferred_provider_name=preferred_provider_name,
        )

    def _collect_text_only_generator_output(self, result):#20260630_kpopmodder: Collect delta/snapshot streams safely.
        #20260705_kpopmodder: Keep compatibility wrapper for tests or plugins using this private helper.
        return self._get_text_only_generation_helper().collect_generator_output(result)

    def load_content(self):#20260617_kpopmodder
        self.system_prompt_text = self.context_manager.load_content()
        return self.system_prompt_text


    def update_file(self, new_content):#20260617_kpopmodder
        self.context_manager.update_file(new_content)
        self.system_prompt_text = self.context_manager.system_prompt_text

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
        self.event_dispatcher.send_output(output)
        #20260620_kpopmodder: Output listener dispatch moved to LLMEventDispatcher.
        # for subcriber in self.output_event_listeners:
        #     subcriber(output)
    
    def send_full_output(self, output):
        self.event_dispatcher.send_full_output(output)
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

    def add_output_event_listener(self, function, full_response = False):
        self.event_dispatcher.add_output_event_listener(
            function,
            full_response=full_response
        )
        #20260620_kpopmodder: Listener registration moved to LLMEventDispatcher.
        # if full_response:
        #     self.full_output_event_listeners.append(function)
        # else:
        #     self.output_event_listeners.append(function)

    def remove_output_event_listener(self, function, full_response=False):
        return self.event_dispatcher.remove_output_event_listener(
            function,
            full_response=full_response,
        )

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

    def update_process_queue_textbox(self):#20260617_kpopmodder
        self.process_queue_live_textbox.set(
            LAV_utils.queue_to_list(self.input_queue)
        )

    def handle_interrupt(self):#20260621_kpopmodder
        self.input_queue_worker.clear_pending_inputs()
        self.response_pipeline.request_interrupt()
        self.liveTextbox.print("[LLM] Interrupt: cleared pending inputs.")

    def shutdown(self):
        if self._shutdown:
            return

        self._shutdown = True
        #20260623_kpopmodder: Shutdown owns the matching unsubscribe for the app-wide interrupt hook.
        if self._interrupt_subscription is not None:
            self._interrupt_subscription.unsubscribe()
            self._interrupt_subscription = None

        self.input_queue_worker.clear_pending_inputs()
        self.response_pipeline.request_interrupt()
        self.event_dispatcher.clear_listeners()

        input_thread = self.input_queue_worker.input_process_thread
        if (
            input_thread is not None
            and input_thread.is_alive()
            and threading.current_thread() != input_thread
        ):
            input_thread.join(timeout=0.3)

        super().shutdown()
