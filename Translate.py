#from queue import Queue#20260621_kpopmodder
from queue import Queue, Empty#20260621_kpopmodder
import threading

from core.event_manager import event_manager, EventType#20260621_kpopmodder
from plugin_system.interfaces import TranslationPluginInterface
import gradio as gr
from plugin_system.selection import PluginSelectionBase
from ui_core.live_textbox import LiveTextbox
import LAV_utils
from core.logger import log_print, debug_print#20260612_kpopmodder


class Translate(PluginSelectionBase):
    def __init__(self) -> None:
        super().__init__(TranslationPluginInterface)
        self.input_queue = Queue()
        self.output_event_listeners = []
        self.input_process_thread = None
        self.log_live_textbox = LiveTextbox()
        self.queue_live_textbox = LiveTextbox()
        self._shutdown = False
        self._interrupt_subscription = event_manager.subscribe(#20260621_kpopmodder
            EventType.INTERRUPT,
            self.handle_interrupt,
        )#20260621_kpopmodder

    def create_ui(self):
        with gr.Tab("Translate"):
            super().create_plugin_selection_ui()
            # translation UI
            original_text_textbox = gr.Textbox(
                label="Original Text", lines=3, render=False)
            translated_text_textbox = gr.Textbox(
                label="Translated Text", lines=3, render=False)

            gr.Interface(
                fn=self.translate_wrapper,
                inputs=[original_text_textbox],
                outputs=[translated_text_textbox],
                #allow_flagging="never",#20260615_kpopmodder
                flagging_mode="never",#20260615_kpopmodder
                examples=["My name is Wolfgang and I live in Berlin",
                          "Have you ever kept goldfish as pets? They're very cute."]
            )
            with gr.Accordion("Console", open=False):
                self.log_live_textbox.create_ui()
                self.queue_live_textbox.create_ui(
                    lines=3, max_lines=3, label="Input waiting to be processed: ")
            super().create_plugin_ui()

    def translate_wrapper(self, text):
        source_text, metadata = self.unpack_input_payload(text)#20260623_kpopmodder
        self.log_live_textbox.print(f"Input: {source_text}")
        result = self.current_plugin.translate(source_text)
        self.log_live_textbox.print(f"Translation: {result}")
        self.send_output(self.build_output_payload(result, metadata))#20260623_kpopmodder
        return result

    def receive_input(self, text):
        self.input_queue.put(text)
        self.process_input_queue()

    def process_input_queue(self):
        def translate_text():
            while (not self.input_queue.empty()):
                self.translate_wrapper(self.input_queue.get())
                self.queue_live_textbox.set(
                    LAV_utils.queue_to_list(self.input_queue))

        # Check if the current thread is alive
        if self.input_process_thread is None or not self.input_process_thread.is_alive():
            # Create and start a new thread
            self.input_process_thread = threading.Thread(target=translate_text)
            self.input_process_thread.start()

    def send_output(self, output):
        log_print(f"translation output:{output}")#20260612_kpopmodder
        for subcriber in list(self.output_event_listeners):
            subcriber(output)

    def add_output_event_listener(self, function):
        if function in self.output_event_listeners:
            return
        self.output_event_listeners.append(function)

    def remove_output_event_listener(self, function):
        removed = False
        while function in self.output_event_listeners:
            self.output_event_listeners.remove(function)
            removed = True
        return removed

    def unpack_input_payload(self, payload):#20260623_kpopmodder
        if isinstance(payload, dict) and "text" in payload:
            metadata = dict(payload)
            text = metadata.pop("text")
            return text, metadata
        return payload, None

    def build_output_payload(self, text, metadata):#20260623_kpopmodder
        if metadata is None:
            return text
        payload = dict(metadata)
        payload["text"] = text
        return payload

    def handle_interrupt(self):#20260621_kpopmodder
        while True:
            try:
                self.input_queue.get_nowait()
            except Empty:
                break
            except Exception:
                break

        self.queue_live_textbox.set(
            LAV_utils.queue_to_list(self.input_queue)
        )
        self.log_live_textbox.print("[Translate] Interrupt: cleared pending inputs.")

    def shutdown(self):
        if self._shutdown:
            return

        self._shutdown = True
        #20260623_kpopmodder: Pair every app-wide interrupt subscription with shutdown unsubscribe.
        if self._interrupt_subscription is not None:
            self._interrupt_subscription.unsubscribe()
            self._interrupt_subscription = None

        self.handle_interrupt()
        self.output_event_listeners.clear()
        if (
            self.input_process_thread is not None
            and self.input_process_thread.is_alive()
            and threading.current_thread() != self.input_process_thread
        ):
            self.input_process_thread.join(timeout=0.3)
        super().shutdown()
