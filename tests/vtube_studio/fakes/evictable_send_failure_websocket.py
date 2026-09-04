#20260904_kpopmodder: Expose an old-send failure whose replacement close can be gated.
import threading


class EvictableSendFailureWebSocket:
    def __init__(self):
        self.send_entered = threading.Event()
        self.send_release = threading.Event()
        self.first_close_finished = threading.Event()
        self.second_close_entered = threading.Event()
        self.second_close_release = threading.Event()
        self._lock = threading.Lock()
        self.close_calls = 0

    def send(self, payload):
        self.send_entered.set()
        self.send_release.wait()
        raise OSError("controlled old-send failure")

    def close(self):
        with self._lock:
            self.close_calls += 1
            close_number = self.close_calls
        if close_number == 1:
            self.first_close_finished.set()
            return
        self.second_close_entered.set()
        self.second_close_release.wait()
