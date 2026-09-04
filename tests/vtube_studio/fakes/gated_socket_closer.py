#20260904_kpopmodder: Expose a deterministic close-registration gate for shutdown serialization tests.
import threading


class GatedSocketCloser:
    def __init__(self):
        self.request_entered = threading.Event()
        self.request_release = threading.Event()
        self._lock = threading.Lock()
        self._pending_socket = None
        self._request_count = 0

    @property
    def cleanup_pending(self):
        with self._lock:
            return self._pending_socket is not None

    def pending_sockets(self):
        with self._lock:
            if self._pending_socket is None:
                return []
            return [self._pending_socket]

    def request_close(self, ws, timeout=0.0):
        with self._lock:
            self._pending_socket = ws
            self._request_count += 1
            request_count = self._request_count
        if request_count == 1:
            self.request_entered.set()
            self.request_release.wait()
        return False

    def complete(self):
        with self._lock:
            self._pending_socket = None
