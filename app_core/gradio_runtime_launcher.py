#20260718_kpopmodder: Keep Gradio runtime launch/shutdown ownership outside AppComposer.
from app_core.gradio_launch import find_available_port
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
        host="127.0.0.1",
        start_port=7860,
        share=False,
    ):
        port = self.port_finder(host=host, start_port=start_port)
        self.logger(f"[Gradio] Starting at http://{host}:{port}/")
        try:
            interface.queue().launch(
                server_name=host,
                server_port=port,
                share=share,
            )
        except KeyboardInterrupt:
            self.logger("[Gradio] KeyboardInterrupt received; shutting down.")
        finally:
            if runtime_lifecycle is not None:
                runtime_lifecycle.shutdown()
