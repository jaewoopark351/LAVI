#20260620_kpopmodder: VoiceInput helper modules are grouped under voice_input_core without changing behavior.
import gradio as gr

from .languages import LANGUAGES


class VoiceInputUiController:#20260618_kpopmodder
    def __init__(
        self,
        liveTextbox,
        get_input_language_callback,
        start_listening_callback,
        stop_listening_callback,
        on_language_change_callback
    ):
        self.liveTextbox = liveTextbox
        self.get_input_language_callback = get_input_language_callback
        self.start_listening_callback = start_listening_callback
        self.stop_listening_callback = stop_listening_callback
        self.on_language_change_callback = on_language_change_callback

        self.start_listening_button = None
        self.stop_listening_button = None
        self.language_dropdown = None
        self.console_box = None
        self.console_timer = None

    def create_ui(self):
        with gr.Accordion("Voice Input", open=False):
            with gr.Row():
                self.start_listening_button = gr.Button("start Listening")
                self.stop_listening_button = gr.Button("stop Listening")

            with gr.Row():
                language_list = list(LANGUAGES.values())
                language_list.insert(0, "auto")

                self.language_dropdown = gr.Dropdown(
                    language_list,
                    value=self.get_input_language_callback(),
                    label="Input languages"
                )

            with gr.Accordion("Console"):
                self.console_box = self.liveTextbox.create_ui(
                    lines=10,
                    max_lines=20,
                    label=None
                )

        self.start_listening_button.click(
            self.start_listening_callback,
            outputs=[self.console_box]
        )

        self.stop_listening_button.click(
            self.stop_listening_callback,
            outputs=[self.console_box]
        )

        self.language_dropdown.input(
            self.on_language_change_callback,
            inputs=[self.language_dropdown],
            outputs=[self.console_box]
        )

        self.console_timer = gr.Timer(1.5)
        self.console_timer.tick(
            fn=self.liveTextbox.get_text,
            outputs=[self.console_box],
            show_progress=False,
            queue=False
        )
