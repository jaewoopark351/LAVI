#20260620_kpopmodder: VTube Studio helper modules are grouped under vtube_studio_core without changing behavior.
import threading
import time

from core.logger import log_print


class VTubeStudioMouthController:#20260620_kpopmodder
    def __init__(
        self,
        send_callback,
        connected_callback,
        authenticated_callback,
        avatar_data_callback
    ):
        self.send_callback = send_callback
        self.connected_callback = connected_callback
        self.authenticated_callback = authenticated_callback
        self.avatar_data_callback = avatar_data_callback
        self.mouth_thread_started = False

    def start_if_needed(self):
        if self.mouth_thread_started:
            return

        self.mouth_thread_started = True
        threading.Thread(target=self.mouth_data_thread, daemon=True).start()

    def set_mouth_open(self, value):
        value = max(0.0, min(1.0, float(value)))

        self.send_callback({
            "apiName": "VTubeStudioPublicAPI",
            "apiVersion": "1.0",
            "requestID": "mouth_set",
            "messageType": "InjectParameterDataRequest",
            "data": {
                "mode": "set",
                "parameterValues": [
                    {
                        "id": "MouthOpen",
                        "value": value
                    }
                ]
            }
        })

    def close_mouth_force(self):
        try:
            self.avatar_data_callback().mouth_open = 0
        except Exception:
            pass

        for _ in range(5):
            self.set_mouth_open(0)
            time.sleep(0.03)

    def mouth_data_thread(self):#20260614_kpopmodder
        while True:
            try:
                if (
                    not self.connected_callback()
                    or not self.authenticated_callback()
                ):
                    time.sleep(0.5)
                    continue

                avatar_data = self.avatar_data_callback()
                mouth_open = getattr(avatar_data, "mouth_open", 0)
                self.set_mouth_open(mouth_open)

            except Exception as e:
                log_print(f"[VtubeStudio] mouth_data_thread error ignored: {e}")

            time.sleep(0.1)
