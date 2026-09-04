#20260904_kpopmodder: Own the single VTube Studio reconnect worker and interruptible retry cadence.
import threading


class VTubeStudioConnectionWorker:
    def __init__(
        self,
        stop_event,
        retry_delay_sec,
        begin_attempt_callback,
        run_attempt_callback,
        schedule_retry_callback,
        finished_callback,
        thread_factory=None,
    ):
        self.stop_event = stop_event
        self.retry_delay_sec = float(retry_delay_sec)
        self.begin_attempt_callback = begin_attempt_callback
        self.run_attempt_callback = run_attempt_callback
        self.schedule_retry_callback = schedule_retry_callback
        self.finished_callback = finished_callback
        self.thread_factory = thread_factory or threading.Thread
        self._lock = threading.RLock()
        self._thread = None

    @property
    def thread(self):
        with self._lock:
            return self._thread

    @property
    def alive(self):
        thread = self.thread
        return bool(thread and thread.is_alive())

    @property
    def started(self):
        with self._lock:
            return self._thread is not None

    def launch(self):
        with self._lock:
            if self._thread is not None:
                return False
            thread = self.thread_factory(
                target=self._run,
                name="VTubeStudioConnection",
                daemon=True,
            )
            self._thread = thread
            try:
                thread.start()
            except Exception:
                self._thread = None
                raise
            return True

    def join(self, timeout):
        thread = self.thread
        if (
            thread is None
            or thread is threading.current_thread()
            or not thread.is_alive()
        ):
            return True
        thread.join(timeout=max(0.0, float(timeout)))
        return not thread.is_alive()

    def _run(self):
        try:
            while not self.stop_event.is_set():
                attempt_id = self.begin_attempt_callback()
                if attempt_id is None:
                    break
                self.run_attempt_callback(attempt_id)
                if self.stop_event.is_set():
                    break
                if not self.schedule_retry_callback(attempt_id):
                    break
                if self.stop_event.wait(self.retry_delay_sec):
                    break
        finally:
            self.finished_callback()
