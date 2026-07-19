#20260621_kpopmodder: ScreenVision 관찰은 history에 저장하지 않고 사용자 정정은 최근 관찰로 보정한다.
import inspect
import threading

from core.logger import log_print
from llm_core.interaction_context import LLMInteractionContext
from llm_core.memory_bridge import LLMMemoryBridge
from llm_core.response_post_processor import LLMResponsePostProcessor


class LLMResponsePipeline:#20260621_kpopmodder
    def __init__(
        self,
        current_plugin_callback,
        send_output_callback,
        send_full_output_callback,
        history_callback,
        remember_history_callback,
        live_textbox,
        streaming_chunker,
        memory_context_builder=None,#20260621_kpopmodder
        memory_command_handler=None,#20260621_kpopmodder
        screen_question_router=None,#20260628_kpopmodder
    ):
        self.current_plugin_callback = current_plugin_callback
        self.send_output_callback = send_output_callback
        self.send_full_output_callback = send_full_output_callback
        self.history_callback = history_callback
        self.remember_history_callback = remember_history_callback
        self.live_textbox = live_textbox
        self.streaming_chunker = streaming_chunker
        self.memory_context_builder = memory_context_builder#20260621_kpopmodder
        self.memory_command_handler = memory_command_handler#20260621_kpopmodder
        self.screen_question_router = screen_question_router#20260628_kpopmodder
        #20260622_kpopmodder: 메모리 명령/프롬프트/raw event 연동을 응답 흐름에서 분리한다.
        self.memory_bridge = LLMMemoryBridge(
            memory_context_builder=self.memory_context_builder,
            memory_command_handler=self.memory_command_handler,
        )

        self.start_of_response = True
        self.LLM_output = ""
        self.interrupt_event = threading.Event()#20260621_kpopmodder
        self.response_generation = 0#20260623_kpopmodder: TTS가 이전 LLM 응답 조각을 새 응답보다 먼저 읽지 않도록 세대 번호를 붙인다.
        self.response_generation_lock = threading.Lock()#20260623_kpopmodder
        self.interaction_context = LLMInteractionContext()#20260621_kpopmodder
        self._recent_model_history = []#20260720_kpopmodder: Fallback when Gradio sends an empty history for a live turn.
        self._recent_model_history_limit = 12#20260720_kpopmodder
        self.post_processor = LLMResponsePostProcessor(#20260706_kpopmodder
            send_full_output_callback=self.send_full_output_callback,
            history_callback=self.history_callback,
            remember_history_callback=self.remember_history_callback,
            live_textbox=self.live_textbox,
            interrupt_event=self.interrupt_event,
            record_raw_event_callback=self._record_raw_event,
        )

    def is_generator(self):
        current_plugin = self.current_plugin_callback()
        return inspect.isgeneratorfunction(current_plugin.predict)

    def predict(self, message, history, system_prompt):#20260621_kpopmodder
        self.interrupt_event.clear()#20260621_kpopmodder
        response_generation = self.begin_response_generation()#20260623_kpopmodder

        # normalized_input = self.interaction_context.normalize_input(message)#20260622_kpopmodder
        # model_message = self.interaction_context.build_model_input(normalized_input)#20260622_kpopmodder
        # model_history = self.interaction_context.filter_history_for_model(history)#20260622_kpopmodder

        normalized_input = self.interaction_context.normalize_input(message)#20260622_kpopmodder

        latest_screen_observation = (#20260622_kpopmodder
            self.memory_bridge.get_latest_screen_observation()
        )#20260622_kpopmodder

        if latest_screen_observation:#20260622_kpopmodder
            self.interaction_context.add_screen_observation(
                observation=latest_screen_observation,
                source="memory_store",
            )

        screen_question_decision = self._route_screen_question(#20260628_kpopmodder
            normalized_input,
            has_latest_screen_observation=bool(latest_screen_observation),
        )
        model_message = self.interaction_context.build_model_input(#20260622_kpopmodder
            normalized_input,
            screen_question_decision=screen_question_decision,
        )
        model_history = self._model_history_with_fallback(#20260720_kpopmodder
            self.interaction_context.filter_history_for_model(history)
        )

        log_print(f"history: {model_history}")#20260612_kpopmodder
        self.start_of_response = True
        self.LLM_output = ""#20260614_kpopmodder
        self.live_textbox.print(f"Input: {normalized_input.display_text}")

        current_plugin = self.current_plugin_callback()

        memory_command_response = self._try_handle_memory_command(#20260621_kpopmodder
            normalized_input.display_text
        )

        if memory_command_response:#20260621_kpopmodder
            self._record_user_input_event(normalized_input)#20260720_kpopmodder
            yield from self.post_processor.handle_memory_command_response(#20260706_kpopmodder
                memory_command_response,
                normalized_input,
                response_generation,
                set_output_callback=self._set_llm_output,
                send_stream_output_callback=self.send_stream_output,
            )
            self._remember_model_history_turn(normalized_input, self.LLM_output)#20260720_kpopmodder
            return

        self.memory_bridge.set_memory_router_ai_callback(#20260626_kpopmodder
            self._route_memory_with_current_plugin
        )
        augmented_system_prompt = self._build_augmented_system_prompt(#20260621_kpopmodder
            system_prompt,
            query=normalized_input.display_text,#20260622_kpopmodder: 사용자가 실제로 말한 문장으로 과거를 검색한다.
            active_history=model_history,#20260720_kpopmodder: Keep short-term memory from duplicating model history.
        )

        self._record_user_input_event(normalized_input)#20260720_kpopmodder

        #result = current_plugin.predict(model_message, model_history, system_prompt)#20260621_kpopmodder
        result = current_plugin.predict(#20260621_kpopmodder
            model_message,
            model_history,
            augmented_system_prompt,
        )
        self.live_textbox.print("AI: ")#20260621_kpopmodder

        if self.interrupt_event.is_set():#20260621_kpopmodder
            log_print("[LLM] response dropped before streaming")
            return

        #if self.is_generator():#20260621_kpopmodder
        if self.is_generator_plugin(current_plugin):#20260621_kpopmodder
            yield from self._predict_generator(result, response_generation)
        else:#20260614_kpopmodder
            yield from self._predict_non_generator(result, response_generation)

        finished = self.post_processor.finish_full_response(#20260706_kpopmodder
            self.LLM_output,
            normalized_input,
            source=current_plugin.__class__.__name__,
        )
        if finished:#20260720_kpopmodder
            self._remember_model_history_turn(normalized_input, self.LLM_output)

    def _predict_generator(self, result, response_generation):
        processed_idx = 0
        for output in result:
            if self.interrupt_event.is_set():#20260621_kpopmodder
                log_print("[LLM] streaming interrupted")
                return
            if output is None:
                continue

            self.LLM_output = output
            yield output

            if self.interrupt_event.is_set():#20260621_kpopmodder
                log_print("[LLM] streaming interrupted after yield")
                return

            chunk, processed_idx = self.streaming_chunker.get_streaming_tts_chunk(
                self.LLM_output,
                processed_idx,
            )
            if self.interrupt_event.is_set():#20260621_kpopmodder
                log_print("[LLM] streaming chunk dropped after interrupt")
                return
            if chunk:
                log_print(f"[LLM streaming chunk] {chunk}")
                self.send_stream_output(chunk, response_generation)

        if self.interrupt_event.is_set():#20260621_kpopmodder
            log_print("[LLM] response dropped after interrupt")
            return

        self.LLM_output = self.LLM_output.strip()
        if not self.LLM_output:
            return

        remaining = self.LLM_output[processed_idx:].strip()
        if self.interrupt_event.is_set():#20260621_kpopmodder
            log_print("[LLM] remaining response dropped after interrupt")
            return
        if remaining:
            log_print(f"[LLM streaming remaining] {remaining}")
            self.send_stream_output(remaining, response_generation)

        log_print(f"response: {self.LLM_output}")#20260615_kpopmodder
        self.live_textbox.print(
            self.LLM_output,
            append_to_last=True,
        )#20260615_kpopmodder

    def _predict_non_generator(self, result, response_generation):
        self.LLM_output = result or ""
        self.LLM_output = self.LLM_output.strip()
        if self.interrupt_event.is_set():#20260621_kpopmodder
            log_print("[LLM] non-generator response dropped after interrupt")
            return
        if self.LLM_output:
            log_print(f"response: {self.LLM_output}")#20260614_kpopmodder
            self.send_stream_output(self.LLM_output, response_generation)
            self.live_textbox.print(
                self.LLM_output,
                append_to_last=True,
            )
            yield self.LLM_output#20260615_kpopmodder

    def request_interrupt(self):#20260621_kpopmodder
        self.interrupt_event.set()
        new_generation = self.invalidate_response_generation()#20260623_kpopmodder
        log_print(f"[LLM] interrupt requested. response_generation={new_generation}")

    def begin_response_generation(self):#20260623_kpopmodder
        with self.response_generation_lock:
            self.response_generation += 1
            return self.response_generation

    def invalidate_response_generation(self):#20260623_kpopmodder
        with self.response_generation_lock:
            self.response_generation += 1
            return self.response_generation

    def build_stream_payload(self, text, response_generation):#20260623_kpopmodder
        return {
            "text": text,
            "response_generation": response_generation,
        }

    def send_stream_output(self, text, response_generation):#20260623_kpopmodder
        self.send_output_callback(
            self.build_stream_payload(text, response_generation)
        )

    def _set_llm_output(self, value):#20260706_kpopmodder
        self.LLM_output = value

    def _model_history_with_fallback(self, model_history):#20260720_kpopmodder
        if model_history:
            self._replace_recent_model_history(model_history)
            return model_history

        fallback_history = self._copy_recent_model_history()
        if fallback_history:
            log_print(
                "[LLM] active history empty; using recent in-process "
                "model history fallback."
            )
        return fallback_history

    def _replace_recent_model_history(self, model_history):#20260720_kpopmodder
        self._recent_model_history = [
            [str(user or "").strip(), str(assistant or "").strip()]
            for user, assistant in self._iter_history_pairs(model_history)
            if str(user or "").strip() or str(assistant or "").strip()
        ][-self._recent_model_history_limit:]

    def _remember_model_history_turn(self, normalized_input, llm_output):#20260720_kpopmodder
        if (
            not getattr(normalized_input, "remember_history", True)
            or getattr(normalized_input, "kind", "") == LLMInteractionContext.SCREEN_KIND
        ):
            return

        user_text = str(getattr(normalized_input, "display_text", "") or "").strip()
        assistant_text = str(llm_output or "").strip()
        if not user_text or not assistant_text:
            return

        new_pair = [user_text, assistant_text]
        if self._recent_model_history and self._recent_model_history[-1] == new_pair:
            return

        self._recent_model_history.append(new_pair)
        self._recent_model_history = (
            self._recent_model_history[-self._recent_model_history_limit:]
        )

    def _copy_recent_model_history(self):#20260720_kpopmodder
        return [list(pair) for pair in self._recent_model_history]

    def _iter_history_pairs(self, model_history):#20260720_kpopmodder
        for entry in model_history or []:
            try:
                user, assistant = entry
            except Exception:
                continue
            yield user, assistant

    def _build_augmented_system_prompt(self, system_prompt, query=None, active_history=None):#20260622_kpopmodder: 질문별 회상 컨텍스트를 LLM 프롬프트에 붙인다.
        return self.memory_bridge.build_augmented_system_prompt(
            system_prompt,
            query=query,
            active_history=active_history,#20260720_kpopmodder
        )

    def _try_handle_memory_command(self, message):#20260621_kpopmodder
        return self.memory_bridge.try_handle_command(message)

    def _get_memory_store(self):#20260621_kpopmodder
        return self.memory_bridge.get_memory_store()

    def _record_user_input_event(self, normalized_input):#20260720_kpopmodder
        if normalized_input.kind == LLMInteractionContext.SCREEN_KIND:
            return

        self._record_raw_event(
            event_type="user_message",
            value=normalized_input.display_text,
            source="user",
            metadata={
                "kind": normalized_input.kind,
                "remember_history": normalized_input.remember_history,
            },
        )

    def _record_raw_event(
        self,
        event_type,
        value,
        source="unknown",
        metadata=None,
    ):#20260621_kpopmodder
        self.memory_bridge.record_raw_event(
            event_type=event_type,
            value=value,
            source=source,
            metadata=metadata,
        )

    def _route_memory_with_current_plugin(
        self,
        router_system_prompt,
        user_input,
        timeout_sec=None,
    ):#20260626_kpopmodder: MemoryRouter AI path reuses the selected LLM plugin with JSON-only prompt.
        current_plugin = self.current_plugin_callback()
        result = current_plugin.predict(
            str(user_input or ""),
            [],
            str(router_system_prompt or ""),
        )

        if self.is_generator_plugin(current_plugin):
            output = ""
            for chunk in result:
                if chunk is not None:
                    output = str(chunk)
            return output

        return str(result or "")

    def _route_screen_question(
        self,
        normalized_input,
        has_latest_screen_observation=False,
    ):#20260628_kpopmodder: Screen routing classifies intent only; screen text stays out of the router.
        if self.screen_question_router is None:
            return None
        if normalized_input.kind == LLMInteractionContext.SCREEN_KIND:
            return None

        try:
            return self.screen_question_router.route(
                normalized_input.text,
                has_latest_screen_observation=has_latest_screen_observation,
            )
        except Exception as e:
            log_print(f"[ScreenQuestionRouter] route failed: {e}")
            return None

    def is_generator_plugin(self, plugin):#20260621_kpopmodder
        return inspect.isgeneratorfunction(plugin.predict)

# import inspect
# import threading#20260621_kpopmodder

# from core.logger import log_print


# class LLMResponsePipeline:#20260621_kpopmodder
#     def __init__(
#         self,
#         current_plugin_callback,
#         send_output_callback,
#         send_full_output_callback,
#         history_callback,
#         remember_history_callback,
#         live_textbox,
#         streaming_chunker,
#         memory_context_builder=None,
#         memory_command_handler=None,
#     ):
#         self.current_plugin_callback = current_plugin_callback
#         self.send_output_callback = send_output_callback
#         self.send_full_output_callback = send_full_output_callback
#         self.history_callback = history_callback
#         self.remember_history_callback = remember_history_callback
#         self.live_textbox = live_textbox
#         self.streaming_chunker = streaming_chunker
        
#         self.memory_context_builder = memory_context_builder#20260621_kpopmodder
#         self.memory_command_handler = memory_command_handler#20260621_kpopmodder

#         self.start_of_response = True
#         self.LLM_output = ""

#         self.interrupt_event = threading.Event()#20260621_kpopmodder

#     #def is_generator(self):#20260621_kpopmodder
#     def is_generator_plugin(self, plugin):#20260621_kpopmodder
#         #current_plugin = self.current_plugin_callback()#20260621_kpopmodder
#         #return inspect.isgeneratorfunction(current_plugin.predict)#20260621_kpopmodder
#         return inspect.isgeneratorfunction(plugin.predict)

#     # def predict(self, message, history, system_prompt):#20260621_kpopmodder
#     #     self.interrupt_event.clear()#20260621_kpopmodder
#     #     log_print(f"history: {history}")#20260612_kpopmodder

#     #     self.start_of_response = True
#     #     self.LLM_output = ""#20260614_kpopmodder
#     #     self.live_textbox.print(f"Input: {message}")

#     #     current_plugin = self.current_plugin_callback()
#     #     result = current_plugin.predict(message, history, system_prompt)
#     #     self.live_textbox.print("AI: ")

#     #     if self.is_generator():#20260615_kpopmodder
#     #         processed_idx = 0

#     #         for output in result:
#     #             if self.interrupt_event.is_set():#20260621_kpopmodder
#     #                 log_print("[LLM] streaming interrupted")
#     #                 return
                
#     #             if output is None:
#     #                 continue

#     #             self.LLM_output = output
#     #             yield output

#     #             chunk, processed_idx = (
#     #                 self.streaming_chunker.get_streaming_tts_chunk(
#     #                     self.LLM_output,
#     #                     processed_idx
#     #                 )
#     #             )

#     #             if chunk:
#     #                 log_print(f"[LLM streaming chunk] {chunk}")
#     #                 self.send_output_callback(chunk)

#     #         if self.interrupt_event.is_set():#20260621_kpopmodder
#     #             log_print("[LLM] response dropped after interrupt")
#     #             return

#     #         self.LLM_output = self.LLM_output.strip()

#     #         if self.LLM_output:
#     #             remaining = self.LLM_output[processed_idx:].strip()

#     #             if remaining:
#     #                 log_print(f"[LLM streaming remaining] {remaining}")
#     #                 self.send_output_callback(remaining)

#     #             log_print(f"response: {self.LLM_output}")#20260615_kpopmodder
#     #             self.live_textbox.print(
#     #                 self.LLM_output,
#     #                 append_to_last=True
#     #             )#20260615_kpopmodder
#     #     else:#20260614_kpopmodder
#     #         self.LLM_output = result or ""
#     #         self.LLM_output = self.LLM_output.strip()

#     #         if self.LLM_output:
#     #             log_print(f"response: {self.LLM_output}")#20260614_kpopmodder
#     #             self.send_output_callback(self.LLM_output)
#     #             self.live_textbox.print(
#     #                 self.LLM_output,
#     #                 append_to_last=True
#     #             )
#     #             yield self.LLM_output#20260615_kpopmodder

#     #     self.send_full_output_callback(self.LLM_output)

#     #     if self.remember_history_callback():
#     #         self.history_callback().append([message, self.LLM_output])

#     def predict(self, message, history, system_prompt):#20260621_kpopmodder
#         self.interrupt_event.clear()#20260621_kpopmodder
#         log_print(f"history: {history}")#20260612_kpopmodder

#         self.start_of_response = True
#         self.LLM_output = ""#20260614_kpopmodder
#         self.live_textbox.print(f"Input: {message}")

#         # current_plugin = self.current_plugin_callback()#20260621_kpopmodder
#         # result = current_plugin.predict(message, history, system_prompt)#20260621_kpopmodder
#         # self.live_textbox.print("AI: ")#20260621_kpopmodder

#         memory_command_response = self._try_handle_memory_command(message)#20260621_kpopmodder

#         if memory_command_response:#20260621_kpopmodder
#             self.LLM_output = memory_command_response.strip()
#             log_print(f"response: {self.LLM_output}")
#             self.live_textbox.print("AI: ")
#             self.live_textbox.print(
#                 self.LLM_output,
#                 append_to_last=True,
#             )
#             self.send_output_callback(self.LLM_output)#20260621_kpopmodder
#             yield self.LLM_output#20260621_kpopmodder

#             if self.interrupt_event.is_set():#20260621_kpopmodder
#                 log_print("[LLM] memory command response dropped after interrupt")
#                 return

#             self.send_full_output_callback(self.LLM_output)#20260621_kpopmodder

#             if self.remember_history_callback():#20260621_kpopmodder
#                 self.history_callback().append([message, self.LLM_output])

#             return

#         augmented_system_prompt = self._build_augmented_system_prompt(system_prompt)#20260621_kpopmodder

#         current_plugin = self.current_plugin_callback()
#         result = current_plugin.predict(message, history, augmented_system_prompt)
#         self.live_textbox.print("AI: ")

#         if self.interrupt_event.is_set():#20260621_kpopmodder
#             log_print("[LLM] response dropped before streaming")
#             return

#         #if self.is_generator():#20260621_kpopmodder
#         if self.is_generator_plugin(current_plugin):#20260621_kpopmodder
#             processed_idx = 0

#             for output in result:
#                 if self.interrupt_event.is_set():#20260621_kpopmodder
#                     log_print("[LLM] streaming interrupted")
#                     return

#                 if output is None:
#                     continue

#                 self.LLM_output = output
#                 yield output

#                 if self.interrupt_event.is_set():#20260621_kpopmodder
#                     log_print("[LLM] streaming interrupted after yield")
#                     return

#                 chunk, processed_idx = (
#                     self.streaming_chunker.get_streaming_tts_chunk(
#                         self.LLM_output,
#                         processed_idx
#                     )
#                 )

#                 if self.interrupt_event.is_set():#20260621_kpopmodder
#                     log_print("[LLM] streaming chunk dropped after interrupt")
#                     return

#                 if chunk:
#                     log_print(f"[LLM streaming chunk] {chunk}")
#                     self.send_output_callback(chunk)

#             if self.interrupt_event.is_set():#20260621_kpopmodder
#                 log_print("[LLM] response dropped after interrupt")
#                 return

#             self.LLM_output = self.LLM_output.strip()

#             if self.LLM_output:
#                 remaining = self.LLM_output[processed_idx:].strip()

#                 if self.interrupt_event.is_set():#20260621_kpopmodder
#                     log_print("[LLM] remaining response dropped after interrupt")
#                     return

#                 if remaining:
#                     log_print(f"[LLM streaming remaining] {remaining}")
#                     self.send_output_callback(remaining)

#                 log_print(f"response: {self.LLM_output}")#20260615_kpopmodder
#                 self.live_textbox.print(
#                     self.LLM_output,
#                     append_to_last=True
#                 )#20260615_kpopmodder

#         else:#20260614_kpopmodder
#             self.LLM_output = result or ""
#             self.LLM_output = self.LLM_output.strip()

#             if self.interrupt_event.is_set():#20260621_kpopmodder
#                 log_print("[LLM] non-generator response dropped after interrupt")
#                 return

#             if self.LLM_output:
#                 log_print(f"response: {self.LLM_output}")#20260614_kpopmodder
#                 self.send_output_callback(self.LLM_output)
#                 self.live_textbox.print(
#                     self.LLM_output,
#                     append_to_last=True
#                 )
#                 yield self.LLM_output#20260615_kpopmodder

#         if self.interrupt_event.is_set():#20260621_kpopmodder
#             log_print("[LLM] full response/history dropped after interrupt")
#             return

#         self.send_full_output_callback(self.LLM_output)

#         if self.remember_history_callback():
#             self.history_callback().append([message, self.LLM_output])

#     def request_interrupt(self):#20260621_kpopmodder
#         self.interrupt_event.set()
#         log_print("[LLM] interrupt requested")

#     def _build_augmented_system_prompt(self, system_prompt):#20260621_kpopmodder
#         base_prompt = str(system_prompt or "")

#         if self.memory_context_builder is None:
#             return base_prompt

#         try:
#             memory_context = self.memory_context_builder.build_context_text()
#         except Exception as e:
#             log_print(f"[Memory] context build failed: {e}")
#             return base_prompt

#         if not memory_context:
#             return base_prompt

#         return base_prompt + memory_context


#     def _try_handle_memory_command(self, message):#20260621_kpopmodder
#         if self.memory_command_handler is None:
#             return None

#         try:
#             return self.memory_command_handler.try_handle(message)
#         except Exception as e:
#             log_print(f"[Memory] command handling failed: {e}")
#             return None
