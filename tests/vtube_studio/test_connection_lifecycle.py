#20260904_kpopmodder: Verify VTube Studio reconnect and shutdown ownership without a live endpoint.
import threading
import time

from plugins.VtubeStudio.vtube_studio_core.connection.vtube_studio_connection import (
    VTubeStudioConnection,
)
from plugins.VtubeStudio.vtube_studio_core.connection.vtube_studio_connection_state import (
    VTubeStudioConnectionState,
)
from plugins.VtubeStudio.vtube_studio_core.connection.vtube_studio_websocket_session import (
    VTubeStudioWebSocketSession,
)
from .fakes.controlled_send_websocket import ControlledSendWebSocket
from .fakes.evictable_send_failure_websocket import (
    EvictableSendFailureWebSocket,
)
from .fakes.fake_websocket import FakeWebSocket
from .fakes.fake_low_level_websocket import FakeLowLevelWebSocket
from .fakes.non_releasing_send_websocket import NonReleasingSendWebSocket
from .fakes.recording_stop_event import RecordingStopEvent


def test_immediate_attempt_retries_after_three_seconds_and_later_connects():
    stop_event = RecordingStopEvent()
    first_created = threading.Event()
    second_created = threading.Event()
    second_opened = threading.Event()
    sockets = []

    def refuse_connection(ws):
        raise ConnectionRefusedError("VTube Studio is not running")

    behaviors = [
        refuse_connection,
        lambda ws: (ws.emit_open(), second_opened.set(), ws.run_forever_release.wait()),
    ]

    def websocket_factory(url, on_open, on_message, on_error, on_close):
        socket = FakeWebSocket(
            url=url,
            on_open=on_open,
            on_message=on_message,
            on_error=on_error,
            on_close=on_close,
            run_behavior=behaviors[len(sockets)],
        )
        sockets.append(socket)
        (first_created if len(sockets) == 1 else second_created).set()
        return socket

    connection = VTubeStudioConnection(
        "ws://localhost:8001",
        websocket_app_factory=websocket_factory,
        retry_delay_sec=3.0,
        stop_event=stop_event,
    )

    try:
        assert connection.start() is True
        assert connection.start() is False
        assert first_created.wait(1.0)
        assert sockets[0].run_forever_entered.wait(1.0)
        assert stop_event.wait_entered.wait(1.0)
        assert stop_event.wait_calls == [3.0]
        assert connection.state == VTubeStudioConnectionState.DISCONNECTED_WAIT

        stop_event.release_retry_wait()

        assert second_created.wait(1.0)
        assert second_opened.wait(1.0)
        assert connection.connected is True
        assert connection.state == VTubeStudioConnectionState.CONNECTED

        assert connection.shutdown() is True
        assert connection.shutdown() is False
        assert connection.worker_alive is False
        assert connection.state == VTubeStudioConnectionState.STOPPED
        assert sockets[1].close_calls == 1
        assert len(sockets) == 2
    finally:
        connection.shutdown()


def test_late_callbacks_from_an_old_socket_cannot_replace_current_state():
    stop_event = RecordingStopEvent()
    first_created = threading.Event()
    second_created = threading.Event()
    second_opened = threading.Event()
    sockets = []
    reset_attempts = []
    close_callbacks = []
    error_callbacks = []
    message_callbacks = []
    open_callbacks = []

    def first_behavior(socket):
        socket.emit_open()

    def second_behavior(socket):
        socket.emit_open()
        second_opened.set()
        socket.run_forever_release.wait()

    behaviors = [first_behavior, second_behavior]

    def websocket_factory(url, on_open, on_message, on_error, on_close):
        socket = FakeWebSocket(
            url=url,
            on_open=on_open,
            on_message=on_message,
            on_error=on_error,
            on_close=on_close,
            run_behavior=behaviors[len(sockets)],
        )
        sockets.append(socket)
        (first_created if len(sockets) == 1 else second_created).set()
        return socket

    connection = VTubeStudioConnection(
        "ws://localhost:8001",
        websocket_app_factory=websocket_factory,
        stop_event=stop_event,
        attempt_aware_callbacks=True,
        reset_authentication_callback=reset_attempts.append,
        on_open=lambda ws, attempt: open_callbacks.append(attempt),
        on_close=lambda ws, status, message, attempt: close_callbacks.append(attempt),
        on_error=lambda ws, error, attempt: error_callbacks.append(attempt),
        on_message=lambda ws, message, attempt: message_callbacks.append(attempt),
    )

    try:
        assert connection.start() is True
        assert first_created.wait(1.0)
        assert stop_event.wait_entered.wait(1.0)

        stop_event.release_retry_wait()

        assert second_created.wait(1.0)
        assert second_opened.wait(1.0)
        assert connection.current_attempt_id == 2
        assert connection.connected is True

        reset_attempts.clear()
        open_callbacks.clear()
        sockets[0].emit_open()
        sockets[0].emit_close()
        sockets[0].emit_error(RuntimeError("late error"))
        sockets[0].emit_message("late message")

        assert connection.current_attempt_id == 2
        assert connection.connected is True
        assert connection.state == VTubeStudioConnectionState.CONNECTED
        assert reset_attempts == []
        assert open_callbacks == []
        assert close_callbacks == []
        assert error_callbacks == []
        assert message_callbacks == []
    finally:
        connection.shutdown()

    assert connection.worker_alive is False
    assert sockets[1].close_calls == 1
    assert len(sockets) == 2


def test_shutdown_closes_without_waiting_for_a_blocked_send_lock():
    socket_created = threading.Event()
    opened = threading.Event()
    socket_holder = []

    def websocket_factory(url, on_open, on_message, on_error, on_close):
        socket = ControlledSendWebSocket(
            url=url,
            on_open=on_open,
            on_message=on_message,
            on_error=on_error,
            on_close=on_close,
            run_behavior=lambda ws: (
                ws.emit_open(),
                opened.set(),
                ws.run_forever_release.wait(),
            ),
        )
        socket_holder.append(socket)
        socket_created.set()
        return socket

    connection = VTubeStudioConnection(
        "ws://localhost:8001",
        websocket_app_factory=websocket_factory,
    )
    assert connection.start() is True
    assert socket_created.wait(1.0)
    assert opened.wait(1.0)
    socket = socket_holder[0]
    assert connection.mark_authenticated(connection.current_attempt_id) is True

    sender = threading.Thread(
        target=lambda: connection.safe_send_control({"messageType": "test"}),
        daemon=True,
    )
    sender.start()
    assert socket.send_entered.wait(1.0)

    shutdown_complete = threading.Event()
    shutdown_thread = threading.Thread(
        target=lambda: (connection.shutdown(), shutdown_complete.set()),
        daemon=True,
    )
    shutdown_thread.start()
    try:
        assert shutdown_complete.wait(1.5)
        assert socket.close_calls == 1
        assert connection.worker_alive is False
    finally:
        socket.send_release.set()
        connection.shutdown()
        sender.join(timeout=1.0)
        shutdown_thread.join(timeout=1.0)


def test_old_send_failure_cannot_clear_a_new_socket_generation():
    stop_event = RecordingStopEvent()
    first_run_release = threading.Event()
    second_opened = threading.Event()
    sockets = []

    def websocket_factory(url, on_open, on_message, on_error, on_close):
        if not sockets:
            socket = ControlledSendWebSocket(
                url=url,
                on_open=on_open,
                on_message=on_message,
                on_error=on_error,
                on_close=on_close,
                run_behavior=lambda ws: (
                    ws.emit_open(),
                    first_run_release.wait(),
                ),
                raise_on_release=True,
            )
        else:
            socket = FakeWebSocket(
                url=url,
                on_open=on_open,
                on_message=on_message,
                on_error=on_error,
                on_close=on_close,
                run_behavior=lambda ws: (
                    ws.emit_open(),
                    second_opened.set(),
                    ws.run_forever_release.wait(),
                ),
            )
        sockets.append(socket)
        return socket

    connection = VTubeStudioConnection(
        "ws://localhost:8001",
        websocket_app_factory=websocket_factory,
        stop_event=stop_event,
    )
    assert connection.start() is True
    while not sockets:
        assert connection.worker_alive
    first = sockets[0]
    assert first.run_forever_entered.wait(1.0)

    sender = threading.Thread(
        target=lambda: connection.safe_send({"messageType": "test"}),
        daemon=True,
    )
    sender.start()
    assert first.send_entered.wait(1.0)
    first_run_release.set()
    assert stop_event.wait_entered.wait(1.0)
    stop_event.release_retry_wait()
    assert second_opened.wait(1.0)
    second = sockets[1]

    first.send_release.set()
    sender.join(timeout=1.0)
    try:
        assert sender.is_alive() is False
        assert connection.ws is second
        assert connection.current_attempt_id == 2
        assert connection.connected is True
    finally:
        connection.shutdown()


def test_concurrent_start_calls_create_exactly_one_worker():
    socket_created = threading.Event()
    sockets = []

    def websocket_factory(url, on_open, on_message, on_error, on_close):
        socket = FakeWebSocket(
            url=url,
            on_open=on_open,
            on_message=on_message,
            on_error=on_error,
            on_close=on_close,
            run_behavior=lambda ws: ws.run_forever_release.wait(),
        )
        sockets.append(socket)
        socket_created.set()
        return socket

    connection = VTubeStudioConnection(
        "ws://localhost:8001",
        websocket_app_factory=websocket_factory,
    )
    barrier = threading.Barrier(9)
    results = []
    result_lock = threading.Lock()

    def start_connection():
        barrier.wait()
        result = connection.start()
        with result_lock:
            results.append(result)

    callers = [
        threading.Thread(target=start_connection, daemon=True)
        for _ in range(8)
    ]
    for caller in callers:
        caller.start()
    barrier.wait()
    for caller in callers:
        caller.join(timeout=1.0)

    try:
        assert socket_created.wait(1.0)
        assert results.count(True) == 1
        assert results.count(False) == 7
        assert len(sockets) == 1
    finally:
        connection.shutdown()


def test_shutdown_interrupts_retry_wait_without_creating_another_attempt():
    stop_event = RecordingStopEvent()
    sockets = []

    def websocket_factory(url, on_open, on_message, on_error, on_close):
        socket = FakeWebSocket(
            url=url,
            on_open=on_open,
            on_message=on_message,
            on_error=on_error,
            on_close=on_close,
            run_behavior=lambda ws: None,
        )
        sockets.append(socket)
        return socket

    connection = VTubeStudioConnection(
        "ws://localhost:8001",
        websocket_app_factory=websocket_factory,
        stop_event=stop_event,
    )
    assert connection.start() is True
    assert stop_event.wait_entered.wait(1.0)
    assert stop_event.wait_calls == [3.0]

    assert connection.shutdown() is True
    assert connection.worker_alive is False
    assert connection.state == VTubeStudioConnectionState.STOPPED
    assert len(sockets) == 1


def test_stopped_connection_rejects_late_socket_assignment():
    connection = VTubeStudioConnection("ws://localhost:8001")
    late_socket = object()

    assert connection.shutdown() is True
    connection.ws = late_socket

    assert connection.state == VTubeStudioConnectionState.STOPPED
    assert connection.current_attempt_id is None
    assert connection.ws is None
    assert connection.worker_alive is False


def test_shutdown_inside_start_admission_prevents_worker_launch():
    factory_calls = []
    connection = VTubeStudioConnection(
        "ws://localhost:8001",
        websocket_app_factory=lambda *args, **kwargs: factory_calls.append(
            (args, kwargs)
        ),
    )

    assert connection.start(before_start_callback=connection.shutdown) is False

    assert connection.state == VTubeStudioConnectionState.STOPPED
    assert connection.worker_thread is None
    assert connection.worker_alive is False
    assert factory_calls == []


def test_blocked_send_keeps_terminal_state_pending_until_send_finishes():
    socket = NonReleasingSendWebSocket(
        url="ws://localhost:8001",
        on_open=lambda ws: None,
        on_message=lambda ws, message: None,
        on_error=lambda ws, error: None,
        on_close=lambda ws, status, message: None,
        run_behavior=lambda ws: None,
    )
    connection = VTubeStudioConnection("ws://localhost:8001")
    connection.ws = socket
    connection.connected = True

    sender = threading.Thread(
        target=lambda: connection.safe_send({"messageType": "test"}),
        daemon=True,
    )
    sender.start()
    assert socket.send_entered.wait(1.0)

    assert connection.shutdown() is True
    assert connection.state == VTubeStudioConnectionState.STOPPING
    assert connection.cleanup_pending is True

    socket.send_release.set()
    sender.join(timeout=1.0)
    assert sender.is_alive() is False
    assert connection.cleanup_pending is False
    assert connection.state == VTubeStudioConnectionState.STOPPED


def test_stopped_is_delayed_until_admitted_connect_is_cancelled_without_callbacks():
    connect_entered = threading.Event()
    connect_release = threading.Event()
    callbacks = []
    connect_calls = []
    client = FakeLowLevelWebSocket([""])

    def connect_factory(*args, **kwargs):
        connect_calls.append((args, kwargs))
        connect_entered.set()
        connect_release.wait()
        return client

    def session_factory(url, on_open, on_message, on_error, on_close):
        return VTubeStudioWebSocketSession(
            url,
            on_open=on_open,
            on_message=on_message,
            on_error=on_error,
            on_close=on_close,
            connect_factory=connect_factory,
        )

    connection = VTubeStudioConnection(
        "ws://localhost:8001",
        websocket_app_factory=session_factory,
        on_open=lambda ws: callbacks.append("open"),
        on_message=lambda ws, message: callbacks.append("message"),
        on_error=lambda ws, error: callbacks.append("error"),
        on_close=lambda ws, status, message: callbacks.append("close"),
        join_timeout_sec=0.1,
    )
    assert connection.start() is True
    assert connect_entered.wait(1.0)

    assert connection.shutdown() is True
    assert connection.state == VTubeStudioConnectionState.STOPPING
    assert connection.cleanup_pending is True

    connect_release.set()
    deadline = time.monotonic() + 1.0
    while connection.state != VTubeStudioConnectionState.STOPPED:
        assert time.monotonic() < deadline
        time.sleep(0.005)

    assert connection.worker_alive is False
    assert connection.cleanup_pending is False
    assert callbacks == []
    assert len(connect_calls) == 1


def test_old_send_failure_registers_reclose_before_terminal_promotion_after_eviction():
    socket = EvictableSendFailureWebSocket()
    connection = VTubeStudioConnection("ws://localhost:8001")
    connection.ws = socket
    connection.connected = True
    sender = threading.Thread(
        target=lambda: connection.safe_send({"messageType": "test"}),
        daemon=True,
    )
    sender.start()
    assert socket.send_entered.wait(1.0)

    # Rotate away from this attempt and age its first successful close out of
    # the closer's deliberately bounded identity history.
    connection.ws = None
    connection._close_socket_once(socket)
    assert socket.first_close_finished.wait(1.0)
    deadline = time.monotonic() + 1.0
    while connection._socket_closer.cleanup_pending:
        assert time.monotonic() < deadline
        time.sleep(0.005)
    for _ in range(33):
        replacement = NonReleasingSendWebSocket(
            url="ws://localhost:8001",
            on_open=lambda ws: None,
            on_message=lambda ws, message: None,
            on_error=lambda ws, error: None,
            on_close=lambda ws, status, message: None,
            run_behavior=lambda ws: None,
        )
        replacement.send_release.set()
        connection._close_socket_once(replacement)
    deadline = time.monotonic() + 1.0
    while connection._socket_closer.cleanup_pending:
        assert time.monotonic() < deadline
        time.sleep(0.005)

    assert connection.shutdown() is True
    assert connection.state == VTubeStudioConnectionState.STOPPING

    socket.send_release.set()
    assert socket.second_close_entered.wait(1.0)
    sender.join(timeout=1.0)

    assert sender.is_alive() is False
    assert connection.cleanup_pending is True
    assert connection.state == VTubeStudioConnectionState.STOPPING

    socket.second_close_release.set()
    deadline = time.monotonic() + 1.0
    while connection.state != VTubeStudioConnectionState.STOPPED:
        assert time.monotonic() < deadline
        time.sleep(0.005)
    assert connection.cleanup_pending is False
