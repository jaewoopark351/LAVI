#20260904_kpopmodder: Own only the VTube Studio Gradio authentication controls and notices.
import gradio as gr

from core.logger import log_print


class VTubeStudioUI:
    def __init__(self, token_store):
        self.token_store = token_store

    def create(self, authenticate_callback):
        with gr.Accordion(label="Vtube Studio Options", open=False):
            button = gr.Button("Authenticate")
            button.click(authenticate_callback)
        return button

    def show_authentication_info(self):
        try:
            if not self.token_store.has_nonempty_token():
                gr.Info("Acquiring token, please continue in VTube Studio...")
            else:
                gr.Info("Token found, attempting to authenticate...")
            return True
        except Exception as error:
            log_print(
                "[VtubeStudio] authentication notice unavailable "
                f"error_type={type(error).__name__}",
                level="warning",
            )
            return False
