#20260904_kpopmodder: Verify blocked socket cleanup stays bounded and truthfully pending.
import threading
import time

from plugins.VtubeStudio.vtube_studio_core.connection.vtube_studio_connection import (
    VTubeStudioConnection,
)
from plugins.VtubeStudio.vtube_studio_core.connection.vtube_studio_connection_state import (
    VTubeStudioConnectionState,
)
from .fakes.blocking_close_websocket import BlockingCloseWebSocket
from .fakes.gated_socket_closer import GatedSocketCloser


def test_blocking_close_does_not_extend_shutdown_past_join_timeout():
    join_timeout_sec = 0.2
    socket = BlockingCloseWebSocket()
    connection = VTubeStudioConnection(
        "ws://localhost:8001",
        join_timeout_sec=join_timeout_sec,
    )
    connection.ws = socket

    started_at = time.monotonic()
    try:
        assert connection.shutdown() is True
        elapsed = time.monotonic() - started_at

        assert socket.close_entered.is_set()
        assert elapsed < join_timeout_sec
        assert connection.state == VTubeStudioConnectionState.STOPPING
        assert connection.cleanup_pending is True
    finally:
        socket.close_release.set()

    assert socket.close_finished.wait(1.0)
    deadline = time.monotonic() + 1.0
    while connection.cleanup_pending and time.monotonic() < deadline:
        time.sleep(0.005)

    assert connection.cleanup_pending is False
    assert connection.state == VTubeStudioConnectionState.STOPPED
    assert connection.shutdown() is False
    assert connection.state == VTubeStudioConnectionState.STOPPED
    assert socket.close_calls == 1


def test_shutdown_preserves_pending_old_socket_while_closing_active_socket():
    old_socket = BlockingCloseWebSocket()
    active_socket = BlockingCloseWebSocket()
    active_socket.close_release.set()
    connection = VTubeStudioConnection(
        "ws://localhost:8001",
        join_timeout_sec=0.2,
    )
    connection.ws = old_socket
    attempt_id = connection.current_attempt_id
    assert connection.disconnect_attempt(attempt_id, "test_rotation") is True
    assert old_socket.close_entered.wait(1.0)
    connection.ws = active_socket

    try:
        assert connection.shutdown() is True
        assert active_socket.close_finished.wait(1.0)
        assert old_socket.close_calls == 1
        assert active_socket.close_calls == 1
        assert connection.cleanup_pending is True
        assert connection.state == VTubeStudioConnectionState.STOPPING
    finally:
        old_socket.close_release.set()

    assert old_socket.close_finished.wait(1.0)
    deadline = time.monotonic() + 1.0
    while connection.cleanup_pending and time.monotonic() < deadline:
        time.sleep(0.005)

    assert connection.shutdown() is False
    assert connection.state == VTubeStudioConnectionState.STOPPED
    assert old_socket.close_calls == 1
    assert active_socket.close_calls == 1


def test_concurrent_shutdown_cannot_publish_stopped_before_close_registration():
    socket = BlockingCloseWebSocket()
    closer = GatedSocketCloser()
    connection = VTubeStudioConnection(
        "ws://localhost:8001",
        socket_closer=closer,
        join_timeout_sec=0.1,
    )
    connection.ws = socket
    results = []
    first_finished = threading.Event()
    second_finished = threading.Event()

    first = threading.Thread(
        target=lambda: (results.append(connection.shutdown()), first_finished.set()),
        daemon=True,
    )
    second = threading.Thread(
        target=lambda: (results.append(connection.shutdown()), second_finished.set()),
        daemon=True,
    )

    first.start()
    assert closer.request_entered.wait(1.0)
    second.start()
    assert second_finished.wait(0.05) is False

    closer.request_release.set()
    first.join(timeout=1.0)
    second.join(timeout=1.0)

    assert first_finished.is_set()
    assert second_finished.is_set()
    assert sorted(results) == [False, True]
    assert connection.cleanup_pending is True
    assert connection.state == VTubeStudioConnectionState.STOPPING

    closer.complete()
    connection._on_socket_close_result(True, None)

    assert connection.cleanup_pending is False
    assert connection.state == VTubeStudioConnectionState.STOPPED
