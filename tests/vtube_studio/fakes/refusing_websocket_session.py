#20260904_kpopmodder: Model an unavailable VTube Studio endpoint without probing a real port.
import threading


class RefusingWebSocketSession:
    attempts = 0
    attempted = threading.Event()

    def __init__(self, url, on_open, on_message, on_error, on_close):
        self.url = url

    def run_forever(self):
        type(self).attempts += 1
        type(self).attempted.set()
        raise ConnectionRefusedError("VTube Studio endpoint unavailable")

    def close(self):
        return True
