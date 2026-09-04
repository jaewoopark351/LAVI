#20260904_kpopmodder: Orchestrate VTube Studio authentication, connection, and owned lifecycle collaborators.
import threading

from core.logger import log_print

from .avatar import VTubeStudioAvatarControllerGroup
from .events import VTubeStudioInterruptSubscription


class VTubeStudioRuntime:
    def __init__(
        self,
        connection,
        auth_manager,
        mouth_controller,
        song_expression_controller,
        blink_controller,
        speaking_pose_controller,
        idle_pose_controller,
        smile_controller,
        event_manager,
    ):
        self.connection = connection
        self.auth_manager = auth_manager
        self.mouth_controller = mouth_controller
        self.song_expression_controller = song_expression_controller
        self.blink_controller = blink_controller
        self.speaking_pose_controller = speaking_pose_controller
        self.idle_pose_controller = idle_pose_controller
        self.smile_controller = smile_controller
        self.event_manager = event_manager

        self.avatar_controller_group = VTubeStudioAvatarControllerGroup(
            mouth_controller=mouth_controller,
            song_expression_controller=song_expression_controller,
            blink_controller=blink_controller,
            speaking_pose_controller=speaking_pose_controller,
            idle_pose_controller=idle_pose_controller,
            smile_controller=smile_controller,
        )
        self.interrupt_subscription = VTubeStudioInterruptSubscription(
            event_manager=event_manager,
            callback=self.handle_interrupt,
        )
        self._lock = threading.RLock()
        self._initialized = False
        self._shutting_down = False
        self._stopped = False

    @property
    def shutting_down(self):
        with self._lock:
            return self._shutting_down

    @property
    def _event_subscription(self):
        return self.interrupt_subscription.subscription

    def initialize(self):
        with self._lock:
            if self._initialized or self._shutting_down:
                return False
            if not self.interrupt_subscription.subscribe():
                return False
            self._initialized = True
        log_print("[VtubeStudio] provider initialized state=RUNNING")
        return True

    def start_connection(self):
        with self._lock:
            if self._shutting_down:
                return False
        return self.connection.start()

    def on_open(self, ws, attempt_id):
        with self._lock:
            if self._shutting_down:
                return False
        if not self.connection.mark_authenticating(attempt_id):
            return False
        return self.auth_manager.on_open(attempt_id)

    def on_message(self, ws, message, attempt_id):
        with self._lock:
            if self._shutting_down:
                return False
        return self.auth_manager.on_message(message, attempt_id)

    def on_error(self, ws, error, attempt_id):
        return None

    def on_close(self, ws, close_status_code, close_msg, attempt_id):
        return None

    def reset_authentication(self, attempt_id=None):
        return self.auth_manager.reset_authentication(attempt_id)

    def on_authenticated(self, attempt_id):
        with self._lock:
            if self._shutting_down:
                return False
        if not self.connection.mark_authenticated(attempt_id):
            return False
        with self._lock:
            if self._shutting_down:
                return False
        if not self.connection.is_attempt_authenticated(attempt_id):
            return False
        return self._start_avatar_threads(attempt_id)

    def on_authentication_rejected(self, attempt_id, reason):
        with self._lock:
            if self._shutting_down:
                return False
        return self.connection.mark_authentication_rejected(attempt_id, reason)

    def start_avatar_threads(self):
        attempt_id = self.connection.current_attempt_id
        with self._lock:
            if self._shutting_down:
                return False
        if attempt_id is not None and not self.connection.mark_authenticated(attempt_id):
            return False
        with self._lock:
            if self._shutting_down:
                return False
        if attempt_id is not None and not self.connection.is_attempt_authenticated(
            attempt_id
        ):
            return False
        return self._start_avatar_threads(attempt_id)

    def handle_interrupt(self):
        with self._lock:
            if self._shutting_down:
                return
        log_print("[VtubeStudio] INTERRUPT received. Closing mouth.")
        self.mouth_controller.close_mouth_force()

    def shutdown(self):
        #20260628_kpopmodder: Stop expression and pose controllers owned by VTube Studio.
        with self._lock:
            if self._stopped:
                return False
            first_shutdown = not self._shutting_down
            self._shutting_down = True
            self.avatar_controller_group.request_shutdown()
            self.interrupt_subscription.request_shutdown()

        if first_shutdown:
            log_print("[VtubeStudio] shutdown started")
            self.auth_manager.stop()

        self.connection.shutdown()
        self.avatar_controller_group.shutdown()
        self.interrupt_subscription.unsubscribe()

        connection_thread_alive = self.connection.worker_alive
        connection_cleanup_pending = self.connection.cleanup_pending
        avatar_cleanup_pending = self.avatar_controller_group.cleanup_pending
        subscription_cleanup_pending = self.interrupt_subscription.cleanup_pending
        cleanup_incomplete = (
            connection_thread_alive
            or connection_cleanup_pending
            or avatar_cleanup_pending
            or subscription_cleanup_pending
        )
        with self._lock:
            self._stopped = not cleanup_incomplete
        if cleanup_incomplete:
            log_print(
                "[VtubeStudio] state=STOPPING "
                f"thread_alive={str(connection_thread_alive or self.avatar_controller_group.threads_alive).lower()} "
                f"connection_thread_alive={str(connection_thread_alive).lower()} "
                f"connection_cleanup_pending={str(connection_cleanup_pending).lower()} "
                f"avatar_cleanup_pending={str(avatar_cleanup_pending).lower()} "
                f"subscription_cleanup_pending={str(subscription_cleanup_pending).lower()}",
                level="warning",
            )
            return False
        log_print("[VtubeStudio] state=STOPPED thread_alive=false")
        return True

    def _start_avatar_threads(self, attempt_id):
        start_result = self.avatar_controller_group.start_if_needed()
        if start_result is True:
            return True
        if start_result is None:
            return False

        with self._lock:
            shutting_down = self._shutting_down
        if not shutting_down:
            if attempt_id is not None:
                self.connection.disconnect_attempt(
                    attempt_id,
                    "avatar_controller_start_failed",
                )
            else:
                self.auth_manager.reset_authentication()
        return False
