import threading
import time

from core.logger import log_print


class AutoWatchController:#20260620_kpopmodder
    def __init__(
        self,
        capture_callback,
        difference_callback,
        change_callback,
        status_callback,
        interval_seconds=1.0,
        change_threshold=8.0,
        analysis_cooldown_seconds=10.0,
    ):
        self.capture_callback = capture_callback
        self.difference_callback = difference_callback
        self.change_callback = change_callback
        self.status_callback = status_callback
        self.interval_seconds = interval_seconds
        self.change_threshold = change_threshold
        self.analysis_cooldown_seconds = analysis_cooldown_seconds

        self._stop_event = threading.Event()
        self._thread = None

    def start(self):
        if self._thread is not None and self._thread.is_alive():
            return

        self._stop_event.clear()
        self._thread = threading.Thread(
            target=self._watch_loop,
            daemon=True,
        )
        self._thread.start()
        self.status_callback("[ScreenVision] Auto Watch started.")

    def stop(self):
        self._stop_event.set()
        self.status_callback("[ScreenVision] Auto Watch stopped.")

    #20260623_kpopmodder: Shutdown waits briefly so Auto Watch does not leave a stale background loop.
    def shutdown(self, timeout=0.5):
        self.stop()
        thread = self._thread
        if (
            thread is not None
            and thread.is_alive()
            and threading.current_thread() != thread
        ):
            try:
                thread.join(timeout=timeout)
            except KeyboardInterrupt:
                log_print("[ScreenVision shutdown] auto watch join skipped during Ctrl+C shutdown.")#20260630_kpopmodder

    def _watch_loop(self):
        previous_image = None
        last_analysis_time = 0.0

        while not self._stop_event.is_set():
            try:
                current_image = self.capture_callback()

                if previous_image is None:
                    previous_image = current_image
                    self._stop_event.wait(self.interval_seconds)
                    continue

                difference = self.difference_callback(
                    previous_image,
                    current_image,
                )
                previous_image = current_image

                cooldown_finished = (
                    time.time() - last_analysis_time
                    >= self.analysis_cooldown_seconds
                )

                if (
                    difference >= self.change_threshold
                    and cooldown_finished
                ):
                    self.status_callback(
                        f"[ScreenVision] Screen change detected: "
                        f"{difference:.2f}"
                    )
                    last_analysis_time = time.time()
                    self.change_callback(current_image, difference)

            except Exception as e:
                log_print(f"[ScreenVision Auto Watch error] {e}")
                self.status_callback(
                    f"[ScreenVision] Auto Watch error: {e}"
                )
                self._stop_event.wait(1.0)

            self._stop_event.wait(self.interval_seconds)
