#20260904_kpopmodder: Own serialized VTube Studio sends and identity-safe failure invalidation.
import json
import threading

from .vtube_studio_connection_log import (
    connection_error_reason,
    log_connection_event,
    safe_error_type,
)


class VTubeStudioConnectionSender:
    def __init__(
        self,
        attempt_state,
        close_socket_callback,
        reset_authentication_callback,
        activity_finished_callback,
    ):
        self.attempt_state = attempt_state
        self.close_socket_callback = close_socket_callback
        self.reset_authentication_callback = reset_authentication_callback
        self.activity_finished_callback = activity_finished_callback
        self.lock = threading.Lock()
        self._activity_lock = threading.Lock()
        self._active_sends = 0

    @property
    def cleanup_pending(self):
        with self._activity_lock:
            return self._active_sends > 0

    def send(
        self,
        message,
        expected_attempt_id=None,
        require_authenticated=False,
    ):
        try:
            payload = json.dumps(message)
        except (TypeError, ValueError) as error:
            log_connection_event(
                "send_rejected",
                self.attempt_state.state,
                reason=connection_error_reason(error),
                error_type=safe_error_type(error),
                level="warning",
            )
            return False

        target = self.attempt_state.current_send_target(
            expected_attempt_id,
            authenticated=require_authenticated,
        )
        if target is None:
            return False
        ws, attempt_id = target

        send_error = None
        with self.lock:
            admitted = self.attempt_state.admit_send(
                ws,
                attempt_id,
                require_authenticated,
                self._begin_activity,
            )
            if not admitted:
                return False
            try:
                ws.send(payload)
            except Exception as error:
                send_error = error
                invalidated = self.attempt_state.invalidate_send_failure(
                    ws,
                    attempt_id,
                )
                # Register cleanup before publishing zero active sends. Even if
                # this old identity aged out of the closer's bounded history,
                # STOPPED cannot be promoted ahead of the replacement close.
                self.close_socket_callback(ws)
                if invalidated:
                    self.reset_authentication_callback(attempt_id)
            finally:
                self._finish_activity()

        if send_error is None:
            return True
        log_connection_event(
            "send_failed",
            self.attempt_state.state,
            attempt=attempt_id,
            reason=connection_error_reason(send_error),
            error_type=safe_error_type(send_error),
            level="warning",
        )
        return False

    def _begin_activity(self):
        with self._activity_lock:
            self._active_sends += 1

    def _finish_activity(self):
        with self._activity_lock:
            self._active_sends -= 1
        self.activity_finished_callback()
