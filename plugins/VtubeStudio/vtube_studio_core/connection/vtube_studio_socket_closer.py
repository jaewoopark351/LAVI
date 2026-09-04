#20260904_kpopmodder: Own bounded, identity-safe closure of VTube Studio socket objects.
import threading


class VTubeStudioSocketCloser:
    def __init__(self, thread_factory=None, result_callback=None):
        self.thread_factory = thread_factory or threading.Thread
        self.result_callback = result_callback
        self._lock = threading.RLock()
        self._pending = []
        self._closed = []

    @property
    def cleanup_pending(self):
        with self._lock:
            return bool(self._pending)

    def pending_sockets(self):
        with self._lock:
            return [entry["socket"] for entry in self._pending]

    def request_close(self, ws, timeout=0.0):
        if ws is None:
            return False

        start_error = None
        with self._lock:
            if self._contains_identity(self._closed, ws):
                return False
            entry = self._find_pending_locked(ws)
            if entry is None or (entry["finished"].is_set() and entry["error"]):
                finished = threading.Event()
                entry = {
                    "socket": ws,
                    "finished": finished,
                    "error": None,
                    "thread": None,
                }
                thread = self.thread_factory(
                    target=lambda: self._close_entry(entry),
                    name="VTubeStudioSocketClose",
                    daemon=True,
                )
                entry["thread"] = thread
                if self._find_pending_locked(ws) is None:
                    self._pending.append(entry)
                else:
                    old_entry = self._find_pending_locked(ws)
                    self._pending.remove(old_entry)
                    self._pending.append(entry)
                try:
                    thread.start()
                except Exception as error:
                    entry["error"] = error
                    entry["finished"].set()
                    start_error = error
            else:
                thread = entry["thread"]

        if start_error is not None:
            self._notify(False, start_error)
            return False
        if timeout and thread is not threading.current_thread():
            thread.join(timeout=max(0.0, float(timeout)))
        with self._lock:
            return bool(entry["finished"].is_set() and entry["error"] is None)

    def _close_entry(self, entry):
        error = None
        try:
            entry["socket"].close()
        except Exception as caught:
            error = caught

        with self._lock:
            entry["error"] = error
            entry["finished"].set()
            if error is None:
                if entry in self._pending:
                    self._pending.remove(entry)
                self._closed.append(entry["socket"])
                if len(self._closed) > 32:
                    self._closed.pop(0)
            # Keep the pending-ledger transition and its owner notification in
            # one observable transaction. External cleanup_pending readers
            # cannot see an empty ledger before terminal promotion completes.
            self._notify(error is None, error)

    def _find_pending_locked(self, ws):
        for entry in self._pending:
            if entry["socket"] is ws:
                return entry
        return None

    def _notify(self, succeeded, error):
        if self.result_callback is not None:
            self.result_callback(succeeded, error)

    @staticmethod
    def _contains_identity(items, target):
        return any(item is target for item in items)
