#20260904_kpopmodder: Model a blocked or delayed failing send independently from basic socket behavior.
import threading

from .fake_websocket import FakeWebSocket


class ControlledSendWebSocket(FakeWebSocket):
    def __init__(self, *args, raise_on_release=False, **kwargs):
        super().__init__(*args, **kwargs)
        self.raise_on_release = bool(raise_on_release)
        self.send_entered = threading.Event()
        self.send_release = threading.Event()

    def send(self, payload):
        self.send_entered.set()
        self.send_release.wait()
        if self.raise_on_release:
            raise OSError("controlled send failure")
        super().send(payload)

    def close(self):
        self.send_release.set()
        super().close()
