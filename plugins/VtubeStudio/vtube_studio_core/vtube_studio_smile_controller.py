#20260628_kpopmodder: Keep a light smile parameter separate from TTS mouth-open sync.
import threading

from core.logger import log_print


class VTubeStudioSmileController:#20260628_kpopmodder
    def __init__(
        self,
        send_callback,
        connected_callback,
        authenticated_callback,
        smile_value=1.0,
        reset_value=1.0,#20260628_kpopmodder
        refresh_interval_sec=0.05,#20260628_kpopmodder
        override_callback=None,
        join_timeout=0.3,
    ):
        self.send_callback = send_callback
        self.connected_callback = connected_callback
        self.authenticated_callback = authenticated_callback
        self.smile_value = smile_value
        self.reset_value = reset_value
        self.refresh_interval_sec = float(refresh_interval_sec)
        self.override_callback = override_callback or (lambda: False)
        self.join_timeout = join_timeout

        self.thread = None
        self.stop_event = threading.Event()

    def start_if_needed(self):
        if self.thread and self.thread.is_alive():
            return

        self.stop_event.clear()
        self.thread = threading.Thread(target=self.smile_loop, daemon=True)
        self.thread.start()

    def stop(self):
        self.stop_event.set()

        try:
            if (
                self.thread
                and self.thread.is_alive()
                and threading.current_thread() != self.thread
            ):
                self.thread.join(timeout=self.join_timeout)
        except Exception as e:
            log_print(f"[VtubeStudio] smile stop error ignored: {e}")

        try:
            self.set_mouth_smile(self.reset_value)
        except Exception as e:
            log_print(f"[VtubeStudio] smile reset error ignored: {e}")

    def smile_loop(self):
        while not self.stop_event.is_set():
            try:
                self.apply_smile_once()

            except Exception as e:
                log_print(f"[VtubeStudio] smile loop error ignored: {e}")

            if self.stop_event.wait(self.refresh_interval_sec):
                break

    def apply_smile_once(self):
        if (
            not self.connected_callback()
            or not self.authenticated_callback()
            or self.is_overridden()
        ):
            return False

        self.set_mouth_smile(self.smile_value)
        return True

    def is_overridden(self):
        try:
            return bool(self.override_callback())
        except Exception as e:
            log_print(f"[VtubeStudio] smile override check error ignored: {e}")
            return False

    def set_mouth_smile(self, value):
        value = max(0.0, min(1.0, float(value)))

        self.send_callback({
            "apiName": "VTubeStudioPublicAPI",
            "apiVersion": "1.0",
            "requestID": "smile_set",
            "messageType": "InjectParameterDataRequest",
            "data": {
                "mode": "set",
                "parameterValues": [
                    {
                        "id": "MouthSmile",
                        "value": value
                    }
                ]
            }
        })
