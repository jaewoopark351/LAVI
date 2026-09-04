#20260904_kpopmodder: Provide a socket whose close completion is controlled by a test.
import threading


class BlockingCloseWebSocket:
    def __init__(self):
        self.close_entered = threading.Event()
        self.close_release = threading.Event()
        self.close_finished = threading.Event()
        self._lock = threading.Lock()
        self.close_calls = 0

    def close(self):
        with self._lock:
            self.close_calls += 1
        self.close_entered.set()
        self.close_release.wait()
        self.close_finished.set()
