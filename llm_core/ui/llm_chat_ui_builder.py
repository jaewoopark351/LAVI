#20260905_kpopmodder: Owns construction and callback binding for the LLM Chat tab.
from __future__ import annotations

import gradio as gr

from llm_core.routed_response.presentation.ui import (
    RoutedResponseUiPresentationDrain,
)


class LlmChatUiBuilder:
    def __init__(
        self,
        *,
        host,
        create_plugin_selection_ui_callback,
        create_plugin_ui_callback,
        local_chat_interface_factory_callback,
        load_content_callback,
        update_content_callback,
        speech_style_labels_callback,
        speech_style_label_callback,
        update_speech_style_callback,
        reset_history_callback,
        live_textbox,
        queue_live_textbox,
        ui_presentation_queue_callback=None,
    ) -> None:
        callbacks = (
            create_plugin_selection_ui_callback,
            create_plugin_ui_callback,
            local_chat_interface_factory_callback,
            load_content_callback,
            update_content_callback,
            speech_style_labels_callback,
            speech_style_label_callback,
            update_speech_style_callback,
            reset_history_callback,
        )
        if not all(callable(callback) for callback in callbacks):
            raise TypeError("LLM UI callbacks must be callable")
        self._host = host
        self._create_plugin_selection_ui_callback = (
            create_plugin_selection_ui_callback
        )
        self._create_plugin_ui_callback = create_plugin_ui_callback
        self._local_chat_interface_factory_callback = (
            local_chat_interface_factory_callback
        )
        self._load_content_callback = load_content_callback
        self._update_content_callback = update_content_callback
        self._speech_style_labels_callback = speech_style_labels_callback
        self._speech_style_label_callback = speech_style_label_callback
        self._update_speech_style_callback = update_speech_style_callback
        self._reset_history_callback = reset_history_callback
        self._live_textbox = live_textbox
        self._queue_live_textbox = queue_live_textbox
        if (
            ui_presentation_queue_callback is not None
            and not callable(ui_presentation_queue_callback)
        ):
            raise TypeError("ui_presentation_queue_callback must be callable")
        self._ui_presentation_queue_callback = (
            ui_presentation_queue_callback
        )

    def build(self) -> None:
        with gr.Tab("Chat"):
            with gr.Blocks():
                self._create_plugin_selection_ui_callback()
                system_prompt = gr.Textbox(
                    value=self._load_content_callback,
                    info="System Message:",
                    placeholder="You are a helpful AI Vtuber.",
                    interactive=True,
                    lines=30,
                    autoscroll=True,
                    autofocus=False,
                    visible=False,
                )
                system_prompt.change(
                    fn=self._update_content_callback,
                    inputs=system_prompt,
                )
                speech_style = gr.Radio(
                    choices=list(self._speech_style_labels_callback().values()),
                    value=self._speech_style_label_callback(),
                    label="AI 말투 모드",
                    interactive=True,
                )
                speech_style.change(
                    fn=self._update_speech_style_callback,
                    inputs=speech_style,
                )
                chat_interface = self._local_chat_interface_factory_callback().create(
                    system_prompt=system_prompt,
                    examples=[
                        ["Hello", None, None],
                        ["How do I make a bomb?", None, None],
                        ["What's your name?", None, None],
                        ["Do you know my name?", None, None],
                        [
                            "Do you think humanity will reach an alien planet?",
                            None,
                            None,
                        ],
                        ["Introduce yourself.", None, None],
                        [
                            "Generate a super long name for a custom latte",
                            None,
                            None,
                        ],
                        ["Let's play a game of monopoly.", None, None],
                        ["Do you want to be friend with me?", None, None],
                    ],
                    autofocus=False,
                )
                self._bind_async_presentations(chat_interface)
                self._host.reset_button = gr.Button("reset chat history")
                self._host.reset_button.click(
                    fn=self._reset_history_callback,
                    inputs=[],
                    outputs=[],
                )
                with gr.Accordion("Console"):
                    self._host.console_box = self._live_textbox.create_ui(
                        lines=10,
                        max_lines=20,
                        label=None,
                    )
                    self._host.queue_console_box = self._queue_live_textbox.create_ui(
                        lines=3,
                        max_lines=3,
                        label="Input waiting to be processed: ",
                    )
            self._host.chat_console_timer = gr.Timer(1.5)
            self._host.chat_console_timer.tick(
                fn=self._live_textbox.get_text,
                outputs=[self._host.console_box],
                show_progress=False,
                queue=False,
            )
            self._host.queue_console_timer = gr.Timer(1.5)
            self._host.queue_console_timer.tick(
                fn=self._queue_live_textbox.get_text,
                outputs=[self._host.queue_console_box],
                show_progress=False,
                queue=False,
            )
            self._create_plugin_ui_callback()

    def _bind_async_presentations(self, chat_interface) -> None:
        if self._ui_presentation_queue_callback is None:
            return
        chatbot = getattr(chat_interface, "chatbot", None)
        if chatbot is None:
            return
        presentation_queue = self._ui_presentation_queue_callback()
        drain = RoutedResponseUiPresentationDrain(presentation_queue)
        self._host.routed_response_ui_presentation_drain = drain
        self._host.routed_response_ui_presentation_timer = gr.Timer(0.25)
        self._host.routed_response_ui_presentation_timer.tick(
            fn=drain.append_to_history,
            inputs=[chatbot],
            outputs=[chatbot],
            show_progress=False,
            queue=False,
        )


__all__ = ("LlmChatUiBuilder",)
