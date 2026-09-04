#20260904_kpopmodder: Keep an in-flight send blocked after transport close for terminal-state tests.
from .controlled_send_websocket import ControlledSendWebSocket


class NonReleasingSendWebSocket(ControlledSendWebSocket):
    def close(self):
        with self._lock:
            self.close_calls += 1
        self.run_forever_release.set()
