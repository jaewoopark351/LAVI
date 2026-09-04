#20260904_kpopmodder: Keep MouthOpen message behavior separate from its polling worker.
from .vtube_studio_mouth_worker import VTubeStudioMouthWorker


class VTubeStudioMouthController:#20260620_kpopmodder
    def __init__(
        self,
        send_callback,
        connected_callback,
        authenticated_callback,
        avatar_data_callback,
        worker=None,
    ):
        self.send_callback = send_callback
        self.connected_callback = connected_callback
        self.authenticated_callback = authenticated_callback
        self.avatar_data_callback = avatar_data_callback
        self.worker = worker or VTubeStudioMouthWorker(
            ready_callback=self._is_ready,
            update_callback=self._send_current_avatar_value,
        )

    @property
    def mouth_thread_started(self):
        return self.worker.is_started

    @mouth_thread_started.setter
    def mouth_thread_started(self, value):
        if value:
            self.start_if_needed()
        else:
            self.stop()

    def start_if_needed(self):
        return self.worker.start()

    def stop(self):
        return self.worker.stop()

    def set_mouth_open(self, value):
        value = max(0.0, min(1.0, float(value)))
        return self.send_callback({
            "apiName": "VTubeStudioPublicAPI",
            "apiVersion": "1.0",
            "requestID": "mouth_set",
            "messageType": "InjectParameterDataRequest",
            "data": {
                "mode": "set",
                "parameterValues": [
                    {"id": "MouthOpen", "value": value},
                ],
            },
        })

    def close_mouth_force(self):
        try:
            self.avatar_data_callback().mouth_open = 0
        except Exception:
            pass

        for _ in range(5):
            self.set_mouth_open(0)
            if self.worker.wait(0.03):
                break

    def mouth_data_thread(self):#20260614_kpopmodder
        self.worker.run_loop()

    def _is_ready(self):
        return bool(
            self.connected_callback()
            and self.authenticated_callback()
        )

    def _send_current_avatar_value(self):
        avatar_data = self.avatar_data_callback()
        self.set_mouth_open(getattr(avatar_data, "mouth_open", 0))
