#20260904_kpopmodder: Own synchronized VTube Studio attempt, socket, authentication, and terminal state.
import threading

from .vtube_studio_connection_state import VTubeStudioConnectionState


class VTubeStudioConnectionAttemptState:
    def __init__(self, stop_event):
        self.stop_event = stop_event
        self.lock = threading.RLock()
        self._state = VTubeStudioConnectionState.NOT_STARTED
        self._ws = None
        self._connected = False
        self._should_reconnect = True
        self._attempt_counter = 0
        self._current_attempt_id = None
        self._authenticated_attempt_id = None
        self._current_attempt_opened = False
        self._launch_committed = False
        self._shutdown_transaction_active = False

    @property
    def state(self):
        with self.lock:
            return self._state

    @property
    def current_attempt_id(self):
        with self.lock:
            return self._current_attempt_id

    @property
    def ws(self):
        with self.lock:
            return self._ws

    @property
    def connected(self):
        with self.lock:
            return self._connected

    @property
    def should_reconnect(self):
        with self.lock:
            return self._should_reconnect and not self.stop_event.is_set()

    def prepare_start(self):
        with self.lock:
            if self._state in (
                VTubeStudioConnectionState.STOPPING,
                VTubeStudioConnectionState.STOPPED,
            ):
                return False
            self.stop_event.clear()
            self._should_reconnect = True
            return True

    def can_launch_worker(self):
        with self.lock:
            return (
                not self.stop_event.is_set()
                and self._state
                not in (
                    VTubeStudioConnectionState.STOPPING,
                    VTubeStudioConnectionState.STOPPED,
                )
            )

    def restore_not_started_after_launch_failure(self):
        with self.lock:
            if self._state not in (
                VTubeStudioConnectionState.STOPPING,
                VTubeStudioConnectionState.STOPPED,
            ):
                self._state = VTubeStudioConnectionState.NOT_STARTED

    def set_ws_compatibility(self, value):
        with self.lock:
            if value is not None and (
                self.stop_event.is_set()
                or self._state
                in (
                    VTubeStudioConnectionState.STOPPING,
                    VTubeStudioConnectionState.STOPPED,
                )
            ):
                return False
            if value is not None and self._ws is not None and self._ws is not value:
                raise RuntimeError("cannot replace an active VTube Studio socket")
            if value is not None and self._current_attempt_id is None:
                self._attempt_counter += 1
                self._current_attempt_id = self._attempt_counter
                self._state = VTubeStudioConnectionState.CONNECTING
            self._ws = value
            if value is None:
                self._connected = False
                self._authenticated_attempt_id = None
                self._launch_committed = False
            return True

    def set_connected_compatibility(self, value):
        with self.lock:
            requested = bool(value)
            if requested and (
                self._ws is None
                or self._current_attempt_id is None
                or self.stop_event.is_set()
            ):
                return False
            self._connected = requested
            if not requested:
                self._authenticated_attempt_id = None
            return True

    def enable_reconnect_if_not_started(self):
        with self.lock:
            if self._state != VTubeStudioConnectionState.NOT_STARTED:
                return False
            self._should_reconnect = True
            return True

    def begin_attempt(self):
        with self.lock:
            if self.stop_event.is_set() or not self._should_reconnect:
                return None
            self._attempt_counter += 1
            attempt_id = self._attempt_counter
            self._current_attempt_id = attempt_id
            self._authenticated_attempt_id = None
            self._connected = False
            self._current_attempt_opened = False
            self._launch_committed = False
            self._state = VTubeStudioConnectionState.CONNECTING
            return attempt_id

    def install_socket(self, attempt_id, ws):
        with self.lock:
            if not self._is_attempt_current_locked(attempt_id):
                return False
            self._ws = ws
            return True

    def commit_launch(self, attempt_id, ws):
        """Linearize an attempt launch before the external blocking run call."""
        with self.lock:
            if not self._accept_callback_locked(attempt_id, ws):
                return False
            self._launch_committed = True
            return True

    def finish_attempt(self, attempt_id, ws):
        with self.lock:
            if self._current_attempt_id != attempt_id:
                return False
            should_reset = bool(
                self._connected or self._authenticated_attempt_id == attempt_id
            )
            if ws is None or self._ws is ws:
                self._ws = None
            self._connected = False
            self._authenticated_attempt_id = None
            self._launch_committed = False
            return should_reset

    def enter_retry_wait(self):
        with self.lock:
            if self.stop_event.is_set() or not self._should_reconnect:
                return False
            self._state = VTubeStudioConnectionState.DISCONNECTED_WAIT
            return True

    def mark_open(self, attempt_id, ws):
        with self.lock:
            if not self._accept_callback_locked(attempt_id, ws):
                return False
            self._connected = True
            self._current_attempt_opened = True
            self._state = VTubeStudioConnectionState.CONNECTED
            return True

    def mark_closed(self, attempt_id, ws):
        with self.lock:
            if not self._accept_callback_locked(attempt_id, ws):
                return None
            was_connected = self._current_attempt_opened
            self._connected = False
            self._authenticated_attempt_id = None
            self._state = VTubeStudioConnectionState.DISCONNECTED_WAIT
            return was_connected

    def mark_authenticated(self, attempt_id):
        with self.lock:
            if not self._is_attempt_current_locked(attempt_id) or not self._connected:
                return False
            self._authenticated_attempt_id = attempt_id
            self._state = VTubeStudioConnectionState.AUTHENTICATED
            return True

    def mark_authenticating(self, attempt_id):
        with self.lock:
            if not self._is_attempt_current_locked(attempt_id) or not self._connected:
                return False
            self._state = VTubeStudioConnectionState.AUTHENTICATING
            return True

    def clear_authenticated(self, attempt_id=None):
        with self.lock:
            expected = self._current_attempt_id if attempt_id is None else attempt_id
            if expected != self._current_attempt_id:
                return False
            self._authenticated_attempt_id = None
            if self._connected:
                self._state = VTubeStudioConnectionState.CONNECTED
            return True

    def mark_authentication_rejected(self, attempt_id):
        with self.lock:
            if not self._is_attempt_current_locked(attempt_id) or not self._connected:
                return False
            self._authenticated_attempt_id = None
            self._state = VTubeStudioConnectionState.CONNECTED
            return True

    def is_attempt_authenticated(self, attempt_id=None):
        with self.lock:
            expected = self._current_attempt_id if attempt_id is None else attempt_id
            return (
                self._connected
                and expected is not None
                and self._current_attempt_id == expected
                and self._authenticated_attempt_id == expected
                and not self.stop_event.is_set()
            )

    def is_current_socket(self, ws, attempt_id=None):
        with self.lock:
            expected = self._current_attempt_id if attempt_id is None else attempt_id
            return self._accept_callback_locked(expected, ws)

    def current_send_target(self, expected_attempt_id=None, authenticated=False):
        with self.lock:
            ws = self._ws
            attempt_id = self._current_attempt_id
            if expected_attempt_id is not None and attempt_id != expected_attempt_id:
                return None
            if not self._can_send_identity_locked(ws, attempt_id, authenticated):
                return None
            return ws, attempt_id

    def admit_send(self, ws, attempt_id, authenticated, admission_callback):
        with self.lock:
            if not self._can_send_identity_locked(ws, attempt_id, authenticated):
                return False
            admission_callback()
            return True

    def invalidate_send_failure(self, ws, attempt_id):
        with self.lock:
            if self._ws is not ws or self._current_attempt_id != attempt_id:
                return False
            if self.stop_event.is_set():
                return False
            self._ws = None
            self._connected = False
            self._authenticated_attempt_id = None
            self._launch_committed = False
            self._state = VTubeStudioConnectionState.DISCONNECTED_WAIT
            return True

    def disconnect_attempt(self, attempt_id):
        with self.lock:
            if not self._is_attempt_current_locked(attempt_id):
                return False, None
            ws = self._ws
            self._ws = None
            self._connected = False
            self._authenticated_attempt_id = None
            self._launch_committed = False
            self._state = VTubeStudioConnectionState.DISCONNECTED_WAIT
            return True, ws

    def begin_shutdown(self):
        with self.lock:
            if self._state == VTubeStudioConnectionState.STOPPED:
                return False, None
            first_transition = self._state != VTubeStudioConnectionState.STOPPING
            self._state = VTubeStudioConnectionState.STOPPING
            self._should_reconnect = False
            self.stop_event.set()
            self._shutdown_transaction_active = True
            ws = self._ws
            self._ws = None
            self._connected = False
            self._authenticated_attempt_id = None
            self._current_attempt_id = None
            self._launch_committed = False
            return first_transition, ws

    def finish_shutdown_transaction(self):
        with self.lock:
            self._shutdown_transaction_active = False

    def worker_finished(self):
        with self.lock:
            self._connected = False
            self._authenticated_attempt_id = None
            self._ws = None
            self._current_attempt_id = None
            self._launch_committed = False
            self._should_reconnect = False
            if self._state != VTubeStudioConnectionState.STOPPED:
                self._state = VTubeStudioConnectionState.STOPPING

    def promote_stopped_if_clean(self, worker_alive, cleanup_pending):
        with self.lock:
            if (
                self._state != VTubeStudioConnectionState.STOPPING
                or self._shutdown_transaction_active
                or worker_alive
                or cleanup_pending
                or self._ws is not None
            ):
                return False
            self._state = VTubeStudioConnectionState.STOPPED
            self._current_attempt_id = None
            self._connected = False
            self._authenticated_attempt_id = None
            return True

    def callback_identity(self, attempt_id, ws):
        with self.lock:
            return self._accept_callback_locked(attempt_id, ws)

    def _can_send_identity_locked(self, ws, attempt_id, authenticated):
        if (
            ws is None
            or self._ws is not ws
            or attempt_id is None
            or self._current_attempt_id != attempt_id
            or not self._connected
            or self.stop_event.is_set()
        ):
            return False
        if authenticated and self._authenticated_attempt_id != attempt_id:
            return False
        return True

    def _is_attempt_current_locked(self, attempt_id):
        return (
            attempt_id is not None
            and self._current_attempt_id == attempt_id
            and not self.stop_event.is_set()
        )

    def _accept_callback_locked(self, attempt_id, ws):
        return self._is_attempt_current_locked(attempt_id) and self._ws is ws
