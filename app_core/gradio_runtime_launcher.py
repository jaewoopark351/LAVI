#20260718_kpopmodder: Keep Gradio runtime launch/shutdown ownership outside AppComposer.
from app_core.gradio_launch import (
    DEFAULT_GRADIO_HOST,
    DEFAULT_GRADIO_OPEN_BROWSER,
    DEFAULT_GRADIO_PORT_MAX_ATTEMPTS,
    DEFAULT_GRADIO_SHARE,
    DEFAULT_GRADIO_START_PORT,
    find_available_port,
)
from core.logger import log_print


class GradioRuntimeLauncher:
    #20260718_kpopmodder: Owns Gradio launch details while AppComposer only requests startup.
    def __init__(self, port_finder=find_available_port, logger=log_print):
        self.port_finder = port_finder
        self.logger = logger

    def launch(
        self,
        interface,
        *,
        runtime_lifecycle=None,
        host=DEFAULT_GRADIO_HOST,
        start_port=DEFAULT_GRADIO_START_PORT,
        max_attempts=DEFAULT_GRADIO_PORT_MAX_ATTEMPTS,
        open_browser=DEFAULT_GRADIO_OPEN_BROWSER,
        share=DEFAULT_GRADIO_SHARE,
    ):
        port = self.port_finder(
            host=host,
            start_port=start_port,
            max_attempts=max_attempts,
        )
        self.logger(f"[Gradio] Starting at http://{host}:{port}/")
        try:
            interface.queue().launch(
                server_name=host,
                server_port=port,
                share=share,
                inbrowser=open_browser,
            )
        except KeyboardInterrupt:
            self.logger("[Gradio] KeyboardInterrupt received; shutting down.")
        finally:
            if runtime_lifecycle is not None:
                runtime_lifecycle.shutdown()
