#20260904_kpopmodder: Model a VTube Studio socket whose control sends can be paused by a test.
import threading

from .fake_websocket import FakeWebSocket


class SwitchableBlockingSendWebSocket(FakeWebSocket):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.opened = threading.Event()
        self.send_entered = threading.Event()
        self.send_release = threading.Event()
        self.block_sends = False

    def send(self, payload):
        if self.block_sends:
            self.send_entered.set()
            self.send_release.wait()
        super().send(payload)

    def close(self):
        self.send_release.set()
        super().close()
