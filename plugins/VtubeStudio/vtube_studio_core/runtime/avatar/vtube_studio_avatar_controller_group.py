#20260904_kpopmodder: Own coordinated avatar-controller startup, rollback, and shutdown.
import threading

from core.logger import log_print


class VTubeStudioAvatarControllerGroup:
    def __init__(
        self,
        mouth_controller,
        song_expression_controller,
        blink_controller,
        speaking_pose_controller,
        idle_pose_controller,
        smile_controller,
    ):
        self.mouth_controller = mouth_controller
        self.song_expression_controller = song_expression_controller
        self.blink_controller = blink_controller
        self.speaking_pose_controller = speaking_pose_controller
        self.idle_pose_controller = idle_pose_controller
        self.smile_controller = smile_controller

        self._lock = threading.RLock()
        self._shutdown_requested = False
        self._start_in_progress = False
        self._cleanup_in_progress = False
        self._cleanup_started = False
        self._cleanup_failures = set()

    @property
    def cleanup_pending(self):
        with self._lock:
            operation_pending = self._start_in_progress or self._cleanup_in_progress
            cleanup_failed = bool(self._cleanup_failures)
        return operation_pending or cleanup_failed or self.threads_alive

    @property
    def threads_alive(self):
        for _, _, thread_owner in self._controller_specs():
            if thread_owner is not None and self._thread_is_alive(thread_owner):
                return True
        return False

    def request_shutdown(self):
        with self._lock:
            first_request = not self._shutdown_requested
            self._shutdown_requested = True
            return first_request

    def start_if_needed(self):
        with self._lock:
            if self._shutdown_requested or self._start_in_progress:
                return None
            self._start_in_progress = True

        started = []
        try:
            for name, controller, thread_owner in self._startup_specs():
                if self._is_shutdown_requested():
                    self._rollback(started)
                    return None

                was_alive = self._thread_is_alive(thread_owner)
                if not was_alive:
                    started.append((name, controller))
                controller.start_if_needed()

                if self._is_shutdown_requested():
                    self._rollback(started)
                    return None
            return True
        except Exception as error:
            log_print(
                "[VtubeStudio] avatar controller startup failed "
                f"error_type={type(error).__name__}",
                level="warning",
            )
            self._rollback(started)
            if self._is_shutdown_requested():
                return None
            return False
        finally:
            with self._lock:
                self._start_in_progress = False

    def shutdown(self):
        self.request_shutdown()
        with self._lock:
            if self._cleanup_in_progress:
                return False
            self._cleanup_in_progress = True
            first_cleanup = not self._cleanup_started
            self._cleanup_started = True

        try:
            for name, callback, thread_owner in self._controller_specs():
                if not first_cleanup and not self._needs_retry(name, thread_owner):
                    continue
                self._cleanup_component(name, callback)
        finally:
            with self._lock:
                self._cleanup_in_progress = False
        return not self.cleanup_pending

    def _startup_specs(self):
        return (
            ("mouth", self.mouth_controller, self.mouth_controller.worker),
            (
                "speaking_pose",
                self.speaking_pose_controller,
                self.speaking_pose_controller,
            ),
            ("blink", self.blink_controller, self.blink_controller),
            ("idle_pose", self.idle_pose_controller, self.idle_pose_controller),
            ("smile", self.smile_controller, self.smile_controller),
        )

    def _controller_specs(self):
        return (
            ("mouth", self.mouth_controller.stop, self.mouth_controller.worker),
            ("song_expression", self.song_expression_controller.reset, None),
            ("blink", self.blink_controller.stop, self.blink_controller),
            (
                "speaking_pose",
                self.speaking_pose_controller.stop,
                self.speaking_pose_controller,
            ),
            ("idle_pose", self.idle_pose_controller.stop, self.idle_pose_controller),
            ("smile", self.smile_controller.stop, self.smile_controller),
        )

    def _rollback(self, started):
        for name, controller in reversed(started):
            self._cleanup_component(name, controller.stop)

    def _cleanup_component(self, name, callback):
        try:
            callback()
        except Exception as error:
            with self._lock:
                self._cleanup_failures.add(name)
            log_print(
                f"[VtubeStudio] shutdown component failed component={name} "
                f"error_type={type(error).__name__}",
                level="warning",
            )
            return False
        with self._lock:
            self._cleanup_failures.discard(name)
        return True

    def _needs_retry(self, name, thread_owner):
        with self._lock:
            cleanup_failed = name in self._cleanup_failures
        return cleanup_failed or (
            thread_owner is not None and self._thread_is_alive(thread_owner)
        )

    def _is_shutdown_requested(self):
        with self._lock:
            return self._shutdown_requested

    @staticmethod
    def _thread_is_alive(thread_owner):
        thread = getattr(thread_owner, "thread", None)
        return bool(thread is not None and thread.is_alive())
