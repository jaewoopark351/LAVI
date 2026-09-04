#20260904_kpopmodder: Own one cancellable low-level VTube Studio WebSocket transport session.
import threading

import websocket

from .vtube_studio_websocket_launch_coordinator import (
    VTubeStudioWebSocketLaunchCoordinator,
)
from .vtube_studio_transport_endpoint import (
    resolve_vtube_studio_transport_url,
)


class VTubeStudioWebSocketSession:
    """Single-use transport with sticky, linearized cancellation."""

    def __init__(
        self,
        url,
        on_open,
        on_message,
        on_error,
        on_close,
        connect_timeout_sec=1.0,
        read_poll_sec=0.25,
        connect_factory=None,
    ):
        self.url = url
        self.transport_url = resolve_vtube_studio_transport_url(url)
        self.on_open = on_open
        self.on_message = on_message
        self.on_error = on_error
        self.on_close = on_close
        self.connect_timeout_sec = float(connect_timeout_sec)
        self.read_poll_sec = float(read_poll_sec)
        self.connect_factory = connect_factory or websocket.create_connection

        self._lock = threading.RLock()
        self._abort_lock = threading.Lock()
        self._cancel_event = threading.Event()
        self._launch_coordinator = VTubeStudioWebSocketLaunchCoordinator(
            self._cancel_event
        )
        self._client = None
        self._aborted_client = None
        self._run_started = False
        self._run_finished = False
        self._opened = False
        self._abort_failed = False

    @property
    def run_finished(self):
        with self._lock:
            return self._run_finished

    @property
    def cleanup_pending(self):
        with self._lock:
            return self._abort_failed and self._client is not None

    def run_forever(self):
        with self._lock:
            if self._run_started:
                raise RuntimeError("VTube Studio WebSocket session is single-use")
            self._run_started = True

        client = None
        close_status = None
        close_message = None
        abort_error = None
        terminal_error = None
        try:
            launched, client = self._launch_coordinator.launch(
                self._connect_and_retain_client
            )
            if not launched or self._cancel_event.is_set():
                return False

            if not self._launch_coordinator.invoke_if_active(
                self._publish_open,
                client,
            ):
                return False

            while not self._cancel_event.is_set():
                try:
                    message = client.recv()
                except websocket.WebSocketTimeoutException:
                    continue
                except websocket.WebSocketConnectionClosedException:
                    break
                except Exception as error:
                    self._launch_coordinator.invoke_if_active(
                        self.on_error,
                        self,
                        error,
                    )
                    break

                if message == "" or self._cancel_event.is_set():
                    break
                self._launch_coordinator.invoke_if_active(
                    self.on_message,
                    self,
                    message,
                )
            return True
        except Exception as error:
            terminal_error = error
            self._launch_coordinator.invoke_if_active(
                self.on_error,
                self,
                error,
            )
            raise
        finally:
            owned_client = client or self._owned_client()
            try:
                self._abort_client(owned_client)
            except Exception as error:
                abort_error = error

            with self._lock:
                opened = self._opened
                if abort_error is None:
                    self._opened = False
                self._run_finished = True

            if (
                (opened or terminal_error is not None)
                and abort_error is None
                and not self._cancel_event.is_set()
            ):
                self._launch_coordinator.invoke_if_active(
                    self.on_close,
                    self,
                    close_status,
                    close_message,
                )
            if abort_error is not None:
                raise abort_error

    def send(self, payload):
        with self._lock:
            client = self._client
            if client is None or self._cancel_event.is_set():
                raise websocket.WebSocketConnectionClosedException(
                    "VTube Studio WebSocket session is not connected"
                )
        return client.send(payload)

    def close(self):
        # This may wait for an already-admitted bounded connect call, but it can
        # never return and then allow a new connect/callback to begin.
        self._launch_coordinator.cancel_and_wait_for_admitted_work()
        client = self._owned_client()
        self._abort_client(client)
        return True

    def _connect_and_retain_client(self):
        client = self.connect_factory(
            self.transport_url,
            timeout=self.connect_timeout_sec,
            enable_multithread=True,
        )
        with self._lock:
            self._client = client
        client.settimeout(self.read_poll_sec)
        return client

    def _publish_open(self, client):
        with self._lock:
            if self._client is not client:
                return
            self._opened = True
        self.on_open(self)

    def _owned_client(self):
        with self._lock:
            return self._client

    def _abort_client(self, client):
        if client is None:
            return False
        with self._abort_lock:
            if self._aborted_client is client:
                return False
            try:
                shutdown = getattr(client, "shutdown", None)
                if callable(shutdown):
                    shutdown()
                else:
                    client.close()
            except Exception:
                with self._lock:
                    self._client = client
                    self._abort_failed = True
                raise
            with self._lock:
                if self._client is client:
                    self._client = None
                self._abort_failed = False
            self._aborted_client = client
            return True
