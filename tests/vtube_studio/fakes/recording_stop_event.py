#20260904_kpopmodder: Record retry-delay requests while allowing tests to release waits immediately.
import threading


class RecordingStopEvent:
    def __init__(self):
        self._condition = threading.Condition()
        self._stopped = False
        self._released_waits = 0
        self._wait_calls = []
        self.wait_entered = threading.Event()

    @property
    def wait_calls(self):
        with self._condition:
            return list(self._wait_calls)

    def is_set(self):
        with self._condition:
            return self._stopped

    def clear(self):
        with self._condition:
            self._stopped = False

    def set(self):
        with self._condition:
            self._stopped = True
            self._condition.notify_all()

    def wait(self, timeout=None):
        with self._condition:
            self._wait_calls.append(timeout)
            self.wait_entered.set()
            while not self._stopped and self._released_waits == 0:
                self._condition.wait()
            if self._stopped:
                return True
            self._released_waits -= 1
            return False

    def release_retry_wait(self):
        with self._condition:
            self._released_waits += 1
            self._condition.notify_all()
