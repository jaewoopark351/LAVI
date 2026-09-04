#20260904_kpopmodder: Coordinate the separated VTube Studio attempt, worker, sender, and close owners.
import threading
import time

from plugins.VtubeStudio.vtube_studio_core.diagnostics import (
    VTubeStudioEventLogLimiter,
)
from .vtube_studio_connection_attempt_state import (
    VTubeStudioConnectionAttemptState,
)
from .vtube_studio_connection_log import (
    bounded_reason,
    connection_error_reason,
    log_connection_event,
    safe_error_type,
)
from .vtube_studio_connection_sender import VTubeStudioConnectionSender
from .vtube_studio_connection_state import VTubeStudioConnectionState
from .vtube_studio_connection_worker import VTubeStudioConnectionWorker
from .vtube_studio_socket_closer import VTubeStudioSocketCloser
from .vtube_studio_websocket_session import VTubeStudioWebSocketSession


class VTubeStudioConnection:#20260620_kpopmodder
    def __init__(
        self,
        websocket_url,
        on_open=None,
        on_message=None,
        on_error=None,
        on_close=None,
        reset_authentication_callback=None,
        retry_delay_sec=3.0,
        join_timeout_sec=1.0,
        websocket_app_factory=None,
        thread_factory=None,
        stop_event=None,
        attempt_aware_callbacks=False,
        socket_closer=None,
        log_limiter=None,
    ):
        self.websocket_url = websocket_url
        self.on_open_callback = on_open
        self.on_message_callback = on_message
        self.on_error_callback = on_error
        self.on_close_callback = on_close
        self.reset_authentication_callback = reset_authentication_callback
        self.retry_delay_sec = float(retry_delay_sec)
        self.join_timeout_sec = float(join_timeout_sec)
        self.websocket_app_factory = (
            websocket_app_factory or VTubeStudioWebSocketSession
        )
        self.thread_factory = thread_factory or threading.Thread
        self.stop_event = stop_event or threading.Event()
        self.attempt_aware_callbacks = bool(attempt_aware_callbacks)
        self.log_limiter = log_limiter or VTubeStudioEventLogLimiter()

        self._lifecycle_lock = threading.RLock()
        self._terminal_log_lock = threading.Lock()
        self._terminal_stopped_logged = False
        self._attempt_state = VTubeStudioConnectionAttemptState(self.stop_event)
        self._state_lock = self._attempt_state.lock
        self._socket_closer = socket_closer or VTubeStudioSocketCloser(
            result_callback=self._on_socket_close_result,
        )
        self._sender = VTubeStudioConnectionSender(
            attempt_state=self._attempt_state,
            close_socket_callback=self._close_socket_once,
            reset_authentication_callback=self._reset_authentication,
            activity_finished_callback=self._on_send_activity_finished,
        )
        self._worker = VTubeStudioConnectionWorker(
            stop_event=self.stop_event,
            retry_delay_sec=self.retry_delay_sec,
            begin_attempt_callback=self._begin_attempt,
            run_attempt_callback=self._run_attempt,
            schedule_retry_callback=self._schedule_retry,
            finished_callback=self._on_worker_finished,
            thread_factory=self.thread_factory,
        )

    @property
    def state(self):
        return self._attempt_state.state

    @property
    def current_attempt_id(self):
        return self._attempt_state.current_attempt_id

    @property
    def worker_thread(self):
        return self._worker.thread

    @property
    def worker_alive(self):
        return self._worker.alive

    @property
    def cleanup_pending(self):
        # A failed send registers its exact close before publishing send-idle.
        # Read in that same hand-off order so no observer can miss both owners.
        return self._sender.cleanup_pending or self._socket_closer.cleanup_pending

    @property
    def ws(self):
        return self._attempt_state.ws

    @ws.setter
    def ws(self, value):
        self._attempt_state.set_ws_compatibility(value)

    @property
    def ws_lock(self):
        return self._sender.lock

    @ws_lock.setter
    def ws_lock(self, value):
        self._sender.lock = value

    @property
    def connected(self):
        return self._attempt_state.connected

    @connected.setter
    def connected(self, value):
        self._attempt_state.set_connected_compatibility(value)

    @property
    def websocket_thread_started(self):
        return self.worker_alive

    @websocket_thread_started.setter
    def websocket_thread_started(self, value):
        if not value:
            self.shutdown()

    @property
    def should_reconnect(self):
        return self._attempt_state.should_reconnect

    @should_reconnect.setter
    def should_reconnect(self, value):
        if value:
            self._attempt_state.enable_reconnect_if_not_started()
            return
        self.shutdown()

    def start(self, before_start_callback=None):
        # Keep Thread.start() in the same serialized transaction as shutdown.
        # A re-entrant shutdown from the embedding hook is admitted, then the
        # second state check rejects the pending launch.
        with self._lifecycle_lock:
            if self._worker.started:
                if self.log_limiter.should_log("connection_thread_start_skipped"):
                    log_connection_event(
                        "connection_thread_start_skipped",
                        self.state,
                        reason="already_started",
                        level="debug",
                    )
                return False
            if not self._attempt_state.prepare_start():
                return False

            if before_start_callback is not None:
                before_start_callback()
            if not self._attempt_state.can_launch_worker():
                return False

            try:
                launched = self._worker.launch()
            except Exception:
                self._attempt_state.restore_not_started_after_launch_failure()
                raise
            if not launched:
                return False
            log_connection_event("connection_thread_started", self.state)
            return True

    def websocket_thread(self):
        """Compatibility facade; direct calls still use the guarded start path."""
        return self.start()

    def _run_attempt(self, attempt_id):
        self._reset_authentication(None)
        ws = self._create_websocket(attempt_id)
        if ws is None:
            return
        if not self._attempt_state.commit_launch(attempt_id, ws):
            self._close_socket_once(ws)
            self._finish_attempt(attempt_id, ws)
            return

        try:
            ws.run_forever()
        except Exception as error:
            if not self.stop_event.is_set() and self._should_log_attempt(attempt_id):
                log_connection_event(
                    "connection_attempt_failed",
                    self.state,
                    attempt=attempt_id,
                    reason=connection_error_reason(error),
                    error_type=safe_error_type(error),
                    level="warning",
                )
        finally:
            self._finish_attempt(attempt_id, ws)
            # An ended attempt owns closing its exact transport. This also lifts a
            # failed low-level session abort into the outer pending-close ledger.
            self._close_socket_once(ws)

    def _begin_attempt(self):
        attempt_id = self._attempt_state.begin_attempt()
        if attempt_id is not None and self._should_log_attempt(attempt_id):
            log_connection_event(
                "connection_attempt_started",
                VTubeStudioConnectionState.CONNECTING,
                attempt=attempt_id,
                endpoint=self.websocket_url,
            )
        return attempt_id

    def _schedule_retry(self, attempt_id):
        if not self._attempt_state.enter_retry_wait():
            return False
        if self._should_log_attempt(attempt_id):
            log_connection_event(
                "retry_scheduled",
                VTubeStudioConnectionState.DISCONNECTED_WAIT,
                attempt=attempt_id,
                retry_delay_sec=self.retry_delay_sec,
            )
        return True

    def _create_websocket(self, attempt_id):
        try:
            ws = self.websocket_app_factory(
                self.websocket_url,
                on_open=lambda socket: self._handle_open(attempt_id, socket),
                on_message=lambda socket, message: self._handle_message(
                    attempt_id,
                    socket,
                    message,
                ),
                on_error=lambda socket, error: self._handle_error(
                    attempt_id,
                    socket,
                    error,
                ),
                on_close=lambda socket, status, message: self._handle_close(
                    attempt_id,
                    socket,
                    status,
                    message,
                ),
            )
        except Exception as error:
            if self._should_log_attempt(attempt_id):
                log_connection_event(
                    "connection_socket_create_failed",
                    self.state,
                    attempt=attempt_id,
                    reason=connection_error_reason(error),
                    error_type=safe_error_type(error),
                    level="warning",
                )
            self._finish_attempt(attempt_id, None)
            return None

        if not self._attempt_state.install_socket(attempt_id, ws):
            self._close_socket_once(ws)
            return None
        return ws

    def _finish_attempt(self, attempt_id, ws):
        if self._attempt_state.finish_attempt(attempt_id, ws):
            self._reset_authentication(attempt_id)

    def _handle_open(self, attempt_id, ws):
        if not self._attempt_state.mark_open(attempt_id, ws):
            return
        log_connection_event(
            "connection_established",
            VTubeStudioConnectionState.CONNECTED,
            attempt=attempt_id,
            endpoint=self.websocket_url,
        )
        self._invoke_callback(self.on_open_callback, (ws,), attempt_id)

    def _handle_message(self, attempt_id, ws, message):
        if not self._attempt_state.callback_identity(attempt_id, ws):
            return
        self._invoke_callback(self.on_message_callback, (ws, message), attempt_id)

    def _handle_error(self, attempt_id, ws, error):
        if not self._attempt_state.callback_identity(attempt_id, ws):
            return
        if self._should_log_attempt(attempt_id):
            log_connection_event(
                "connection_error",
                self.state,
                attempt=attempt_id,
                reason=connection_error_reason(error),
                error_type=safe_error_type(error),
                level="warning",
            )
        self._invoke_callback(self.on_error_callback, (ws, error), attempt_id)

    def _handle_close(self, attempt_id, ws, close_status_code, close_msg):
        was_connected = self._attempt_state.mark_closed(attempt_id, ws)
        if was_connected is None:
            return
        self._reset_authentication(attempt_id)
        if was_connected or self._should_log_attempt(attempt_id):
            log_connection_event(
                "connection_closed",
                self.state,
                attempt=attempt_id,
                close_code=close_status_code,
            )
        self._invoke_callback(
            self.on_close_callback,
            (ws, close_status_code, close_msg),
            attempt_id,
        )

    def mark_authenticated(self, attempt_id):
        if not self._attempt_state.mark_authenticated(attempt_id):
            return False
        log_connection_event(
            "authentication_succeeded",
            VTubeStudioConnectionState.AUTHENTICATED,
            attempt=attempt_id,
        )
        return True

    def mark_authenticating(self, attempt_id):
        if not self._attempt_state.mark_authenticating(attempt_id):
            return False
        log_connection_event(
            "authentication_started",
            VTubeStudioConnectionState.AUTHENTICATING,
            attempt=attempt_id,
        )
        return True

    def clear_authenticated(self, attempt_id=None):
        return self._attempt_state.clear_authenticated(attempt_id)

    def mark_authentication_rejected(self, attempt_id, reason="rejected"):
        if not self._attempt_state.mark_authentication_rejected(attempt_id):
            return False
        log_connection_event(
            "authentication_rejected",
            VTubeStudioConnectionState.CONNECTED,
            attempt=attempt_id,
            reason=bounded_reason(reason),
            level="warning",
        )
        return True

    def is_attempt_authenticated(self, attempt_id=None):
        return self._attempt_state.is_attempt_authenticated(attempt_id)

    def is_current_socket(self, ws, attempt_id=None):
        return self._attempt_state.is_current_socket(ws, attempt_id)

    def safe_send(self, message):
        return self._sender.send(message)

    def safe_send_auth(self, message, attempt_id=None):
        return self._sender.send(message, expected_attempt_id=attempt_id)

    def safe_send_control(self, message):
        return self._sender.send(message, require_authenticated=True)

    def disconnect_attempt(self, attempt_id, reason="requested"):
        accepted, ws = self._attempt_state.disconnect_attempt(attempt_id)
        if not accepted:
            return False
        self._reset_authentication(attempt_id)
        log_connection_event(
            "connection_attempt_invalidated",
            VTubeStudioConnectionState.DISCONNECTED_WAIT,
            attempt=attempt_id,
            reason=bounded_reason(reason),
            level="warning",
        )
        self._close_socket_once(ws)
        return True

    def shutdown(self):
        # Serialize detach, pending-close registration, worker join, and terminal
        # publication so a second shutdown cannot observe a half-transaction.
        with self._lifecycle_lock:
            if self.state == VTubeStudioConnectionState.STOPPED and not self.cleanup_pending:
                return False
            first_transition, ws = self._attempt_state.begin_shutdown()
            if first_transition:
                log_connection_event(
                    "shutdown_started",
                    VTubeStudioConnectionState.STOPPING,
                )

            deadline = time.monotonic() + self.join_timeout_sec
            try:
                self._reset_authentication(None)
                sockets = self._unique_sockets(
                    [ws, *self._socket_closer.pending_sockets()]
                )
                close_budget = max(0.0, self.join_timeout_sec / 2.0)
                for index, socket in enumerate(sockets):
                    remaining_count = len(sockets) - index
                    timeout = close_budget / remaining_count if remaining_count else 0.0
                    started_at = time.monotonic()
                    self._socket_closer.request_close(socket, timeout=timeout)
                    close_budget = max(
                        0.0,
                        close_budget - (time.monotonic() - started_at),
                    )
            finally:
                self._attempt_state.finish_shutdown_transaction()

            self._worker.join(max(0.0, deadline - time.monotonic()))
            if self.worker_alive:
                log_connection_event(
                    "connection_thread_stop_timeout",
                    VTubeStudioConnectionState.STOPPING,
                    thread_alive=True,
                    level="warning",
                )
            else:
                self._promote_terminal_if_clean()
            return first_transition

    stop = shutdown

    def mark_open(self, ws=None, attempt_id=None):
        expected_ws = self.ws if ws is None else ws
        expected_attempt = (
            self.current_attempt_id if attempt_id is None else attempt_id
        )
        return self._attempt_state.mark_open(expected_attempt, expected_ws)

    def mark_closed(self, ws=None, attempt_id=None):
        expected_ws = self.ws if ws is None else ws
        expected_attempt = (
            self.current_attempt_id if attempt_id is None else attempt_id
        )
        result = self._attempt_state.mark_closed(expected_attempt, expected_ws)
        if result is None:
            return False
        self._reset_authentication(expected_attempt)
        return True

    def _on_worker_finished(self):
        self._attempt_state.worker_finished()
        # The reconnect loop has no work left at this callback boundary. Treat
        # it as quiescent even though Thread.is_alive() remains true until this
        # final callback returns.
        promoted = self._promote_terminal_if_clean(worker_quiescent=True)
        if not promoted:
            log_connection_event(
                "connection_thread_stopped",
                self.state,
                thread_alive=False,
                cleanup_pending=str(self.cleanup_pending).lower(),
            )

    def _on_send_activity_finished(self):
        self._promote_terminal_if_clean()

    def _close_socket_once(self, ws):
        return self._socket_closer.request_close(ws)

    def _on_socket_close_result(self, succeeded, error):
        if succeeded:
            if (
                self.stop_event.is_set()
                and self.log_limiter.should_log("socket_closed")
            ):
                log_connection_event("socket_closed", self.state)
            self._promote_terminal_if_clean()
        elif self.log_limiter.should_log("socket_close_failed"):
            log_connection_event(
                "socket_close_failed",
                self.state,
                reason="socket_close_failed",
                error_type=safe_error_type(error),
                level="warning",
            )

    def _promote_terminal_if_clean(self, worker_quiescent=False):
        promoted = self._attempt_state.promote_stopped_if_clean(
            worker_alive=self.worker_alive and not worker_quiescent,
            cleanup_pending=self.cleanup_pending,
        )
        if not promoted:
            return False
        with self._terminal_log_lock:
            if self._terminal_stopped_logged:
                return True
            self._terminal_stopped_logged = True
        log_connection_event(
            "connection_thread_stopped",
            VTubeStudioConnectionState.STOPPED,
            thread_alive=False,
            cleanup_pending="false",
        )
        return True

    def _invoke_callback(self, callback, args, attempt_id):
        if callback is None:
            return
        if self.attempt_aware_callbacks:
            callback(*args, attempt_id)
        else:
            callback(*args)

    def _reset_authentication(self, attempt_id):
        callback = self.reset_authentication_callback
        if callback is None:
            return
        if self.attempt_aware_callbacks:
            callback(attempt_id)
        else:
            callback()

    @staticmethod
    def _unique_sockets(sockets):
        unique = []
        for socket in sockets:
            if socket is not None and not any(item is socket for item in unique):
                unique.append(socket)
        return unique

    @staticmethod
    def _should_log_attempt(attempt_id):
        if attempt_id is None or attempt_id <= 3:
            return True
        return attempt_id <= 64 and attempt_id & (attempt_id - 1) == 0
