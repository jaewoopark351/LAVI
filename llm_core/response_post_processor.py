#20260706_kpopmodder: LLM response post-processing is isolated from streaming generation.
from core.logger import log_print
from llm_core.interaction_context import LLMInteractionContext


class LLMResponsePostProcessor:
    #20260706_kpopmodder: Keep memory command/full response side effects in one small helper.
    def __init__(
        self,
        send_full_output_callback,
        history_callback,
        remember_history_callback,
        live_textbox,
        interrupt_event,
        record_raw_event_callback,
    ):
        self.send_full_output_callback = send_full_output_callback
        self.history_callback = history_callback
        self.remember_history_callback = remember_history_callback
        self.live_textbox = live_textbox
        self.interrupt_event = interrupt_event
        self.record_raw_event_callback = record_raw_event_callback

    def handle_memory_command_response(
        self,
        response_text,
        normalized_input,
        response_generation,
        set_output_callback,
        send_stream_output_callback,
    ):
        llm_output = str(response_text or "").strip()
        set_output_callback(llm_output)
        self.record_raw_event_callback(
            event_type="assistant_message",
            value=llm_output,
            source="memory_command",
            metadata={
                "kind": "memory_command_response",
            },
        )
        self.live_textbox.print("AI: ")
        self.live_textbox.print(
            llm_output,
            append_to_last=True,
        )
        send_stream_output_callback(llm_output, response_generation)
        yield llm_output

        if self.interrupt_event.is_set():
            log_print("[LLM] memory command response dropped after interrupt")
            return

        self.send_full_output_callback(llm_output)

        if self.remember_history_callback():
            self.history_callback().append([
                normalized_input.display_text,
                llm_output,
            ])

    def finish_full_response(self, llm_output, normalized_input, source):
        if self.interrupt_event.is_set():
            log_print("[LLM] full response/history dropped after interrupt")
            return False

        self.send_full_output_callback(llm_output)

        self.record_raw_event_callback(
            event_type="assistant_message",
            value=llm_output,
            source=source,
            metadata={
                "kind": normalized_input.kind,
                "remember_history": normalized_input.remember_history,
            },
        )

        should_remember = (
            self.remember_history_callback()
            and normalized_input.remember_history
            and normalized_input.kind != LLMInteractionContext.SCREEN_KIND
        )
        if should_remember:
            #20260621_kpopmodder: Store the real user-facing text, not hidden correction prompts.
            self.history_callback().append([
                normalized_input.display_text,
                llm_output,
            ])

        return True
