#20260904_kpopmodder: Own the interruptible VTube Studio mouth polling thread lifecycle.
import threading

from core.logger import log_print
from plugins.VtubeStudio.vtube_studio_core.diagnostics import (
    VTubeStudioEventLogLimiter,
)


class VTubeStudioMouthWorker:
    def __init__(
        self,
        ready_callback,
        update_callback,
        disconnected_wait_sec=0.5,
        refresh_interval_sec=0.1,
        join_timeout_sec=1.0,
        thread_factory=None,
        stop_event=None,
        log_limiter=None,
    ):
        self.ready_callback = ready_callback
        self.update_callback = update_callback
        self.disconnected_wait_sec = float(disconnected_wait_sec)
        self.refresh_interval_sec = float(refresh_interval_sec)
        self.join_timeout_sec = float(join_timeout_sec)
        self.thread_factory = thread_factory or threading.Thread
        self.stop_event = stop_event or threading.Event()
        self.log_limiter = log_limiter or VTubeStudioEventLogLimiter()
        self._lock = threading.RLock()
        self._thread = None

    @property
    def thread(self):
        with self._lock:
            return self._thread

    @property
    def is_started(self):
        return self.is_alive

    @property
    def is_alive(self):
        thread = self.thread
        return bool(thread and thread.is_alive())

    def start(self):
        with self._lock:
            if self._thread is not None and self._thread.is_alive():
                return False
            self._thread = None
            self.stop_event.clear()
            thread = self.thread_factory(
                target=self.run_loop,
                name="VTubeStudioMouthWorker",
                daemon=True,
            )
            self._thread = thread
            try:
                thread.start()
            except Exception:
                if self._thread is thread:
                    self._thread = None
                raise
            return True

    def stop(self):
        self.stop_event.set()
        thread = self.thread
        if thread and thread.is_alive() and threading.current_thread() is not thread:
            thread.join(timeout=self.join_timeout_sec)
        if thread and thread.is_alive():
            if self.log_limiter.should_log("mouth_worker_stop_timeout"):
                log_print(
                    "[VtubeStudio] mouth worker stop timeout thread_alive=true",
                    level="warning",
                )
            return False
        with self._lock:
            if self._thread is thread:
                self._thread = None
        return True

    def wait(self, timeout):
        return self.stop_event.wait(timeout)

    def run_loop(self):
        current_thread = threading.current_thread()
        try:
            while not self.stop_event.is_set():
                try:
                    if not self.ready_callback():
                        if self.stop_event.wait(self.disconnected_wait_sec):
                            break
                        continue
                    self.update_callback()
                except Exception as error:
                    if self.log_limiter.should_log("mouth_worker_update_failed"):
                        log_print(
                            "[VtubeStudio] mouth worker update failed "
                            f"error_type={type(error).__name__}",
                            level="warning",
                        )
                if self.stop_event.wait(self.refresh_interval_sec):
                    break
        finally:
            with self._lock:
                if self._thread is current_thread:
                    self._thread = None
