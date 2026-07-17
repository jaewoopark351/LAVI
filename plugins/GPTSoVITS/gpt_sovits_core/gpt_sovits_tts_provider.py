#20260717_kpopmodder: Provider adapter around GPTSoVITSTTS so plugin wiring stays separate from HTTP/process details.


class GPTSoVITSTTSProvider:
    #20260717_kpopmodder: Thin TTS provider boundary used by the plugin facade and future provider selection.
    def __init__(self, runtime=None):
        if runtime is None:
            from plugins.GPTSoVITS.GPTSoVITS_TTS import GPTSoVITSTTS

            runtime = GPTSoVITSTTS()
        self.runtime = runtime
        self.initialized = False

    def init(self):
        self.runtime.init()
        self.initialized = True

    def synthesize(self, text):
        return self.runtime.synthesize_to_bytes(text)

    def synthesize_to_file(self, text, output_path):
        return self.runtime.synthesize_to_file(text, output_path)

    def stop(self):
        self.runtime.stop_server()
        self.initialized = False

    def shutdown(self):
        self.stop()

    def diagnostics(self):
        process_manager = getattr(self.runtime, "process_manager", None)
        probe = {}
        if process_manager is not None:
            probe = {
                "process_running": process_manager.is_process_running(),
                "cuda_visible_devices": process_manager.cuda_visible_devices,
            }
        return {
            "initialized": self.initialized,
            "api_url": getattr(self.runtime, "gpt_sovits_url", ""),
            "gpt_sovits_root_configured": bool(
                getattr(self.runtime, "gpt_sovits_root", "")
            ),
            **probe,
        }
