#20260904_kpopmodder: Provide one in-memory WebSocket double without opening a real port.
import threading


class FakeWebSocket:
    def __init__(
        self,
        url,
        on_open,
        on_message,
        on_error,
        on_close,
        run_behavior,
    ):
        self.url = url
        self.on_open = on_open
        self.on_message = on_message
        self.on_error = on_error
        self.on_close = on_close
        self.run_behavior = run_behavior
        self.run_forever_entered = threading.Event()
        self.run_forever_release = threading.Event()
        self._lock = threading.Lock()
        self.close_calls = 0
        self.sent_payloads = []

    def run_forever(self):
        self.run_forever_entered.set()
        self.run_behavior(self)

    def send(self, payload):
        with self._lock:
            self.sent_payloads.append(payload)

    def close(self):
        with self._lock:
            self.close_calls += 1
        self.run_forever_release.set()

    def emit_open(self):
        self.on_open(self)

    def emit_message(self, message):
        self.on_message(self, message)

    def emit_error(self, error):
        self.on_error(self, error)

    def emit_close(self, status=1006, message="closed"):
        self.on_close(self, status, message)
