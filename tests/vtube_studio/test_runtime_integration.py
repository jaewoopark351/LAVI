#20260904_kpopmodder: Verify composed VTube Studio runtime authentication and shutdown without a live service.
import json
import tempfile
import threading
from pathlib import Path
from types import SimpleNamespace

from core.event_manager_core.event_manager import EventManager
from core.event_manager_core.event_type import EventType
from plugins.VtubeStudio.vtube_studio_core.connection.vtube_studio_connection import (
    VTubeStudioConnection,
)
from plugins.VtubeStudio.vtube_studio_core.connection.vtube_studio_connection_state import (
    VTubeStudioConnectionState,
)
from plugins.VtubeStudio.vtube_studio_core.runtime.vtube_studio_component_factory import (
    VTubeStudioComponentFactory,
)
from .fakes.fake_websocket import FakeWebSocket
from .fakes.recording_stop_event import RecordingStopEvent


def test_runtime_connects_authenticates_and_stops_all_owned_resources():
    module_directory = Path(tempfile.mkdtemp(prefix="vtube-runtime-"))
    event_manager = EventManager()
    socket_holder = []
    socket_opened = threading.Event()

    def websocket_factory(url, on_open, on_message, on_error, on_close):
        socket = FakeWebSocket(
            url=url,
            on_open=on_open,
            on_message=on_message,
            on_error=on_error,
            on_close=on_close,
            run_behavior=lambda ws: (
                ws.emit_open(),
                socket_opened.set(),
                ws.run_forever_release.wait(),
            ),
        )
        socket_holder.append(socket)
        return socket

    def connection_factory(**kwargs):
        return VTubeStudioConnection(
            **kwargs,
            websocket_app_factory=websocket_factory,
        )

    runtime, token_store = VTubeStudioComponentFactory(
        event_manager_instance=event_manager,
        connection_factory=connection_factory,
    ).create(
        module_directory=str(module_directory),
        avatar_data_callback=lambda: SimpleNamespace(mouth_open=0.0),
    )

    assert runtime.initialize() is True
    assert event_manager.subscriber_count(EventType.INTERRUPT) == 1
    assert runtime.start_connection() is True

    while not socket_holder:
        assert runtime.connection.worker_alive
    socket = socket_holder[0]
    assert socket.run_forever_entered.wait(1.0)
    assert socket_opened.wait(1.0)
    assert runtime.connection.state == VTubeStudioConnectionState.AUTHENTICATING
    assert json.loads(socket.sent_payloads[0])["messageType"] == (
        "AuthenticationTokenRequest"
    )

    socket.emit_message(json.dumps({
        "messageType": "AuthenticationTokenResponse",
        "data": {"authenticationToken": "runtime-test-token"},
    }))
    socket.emit_message(json.dumps({
        "messageType": "AuthenticationResponse",
        "data": {"authenticated": True, "reason": "ok"},
    }))

    assert token_store.read() == "runtime-test-token"
    assert runtime.auth_manager.is_authenticated is True
    assert runtime.connection.state == VTubeStudioConnectionState.AUTHENTICATED
    assert runtime.mouth_controller.worker.is_alive is True

    assert runtime.shutdown() is True
    assert runtime.shutdown() is False
    assert event_manager.subscriber_count(EventType.INTERRUPT) == 0
    assert runtime.connection.state == VTubeStudioConnectionState.STOPPED
    assert runtime.connection.worker_alive is False
    assert runtime.mouth_controller.worker.is_alive is False
    assert socket.close_calls == 1

    socket.emit_message(json.dumps({
        "messageType": "AuthenticationResponse",
        "data": {"authenticated": True, "reason": "late"},
    }))
    assert runtime.auth_manager.is_authenticated is False
    assert runtime.mouth_controller.worker.is_alive is False


def test_same_runtime_reauthenticates_after_disconnect_and_three_second_retry():
    module_directory = Path(tempfile.mkdtemp(prefix="vtube-runtime-reconnect-"))
    stop_event = RecordingStopEvent()
    event_manager = EventManager()
    sockets = []
    opened_events = [threading.Event(), threading.Event()]

    def websocket_factory(url, on_open, on_message, on_error, on_close):
        socket_index = len(sockets)
        socket = FakeWebSocket(
            url=url,
            on_open=on_open,
            on_message=on_message,
            on_error=on_error,
            on_close=on_close,
            run_behavior=lambda ws: (
                ws.emit_open(),
                opened_events[socket_index].set(),
                ws.run_forever_release.wait(),
            ),
        )
        sockets.append(socket)
        return socket

    def connection_factory(**kwargs):
        return VTubeStudioConnection(
            **kwargs,
            websocket_app_factory=websocket_factory,
            stop_event=stop_event,
        )

    runtime, _ = VTubeStudioComponentFactory(
        event_manager_instance=event_manager,
        connection_factory=connection_factory,
    ).create(
        module_directory=str(module_directory),
        avatar_data_callback=lambda: SimpleNamespace(mouth_open=0.0),
    )

    try:
        assert runtime.initialize() is True
        assert runtime.start_connection() is True
        assert opened_events[0].wait(1.0)
        first = sockets[0]

        first.emit_message(json.dumps({
            "messageType": "AuthenticationTokenResponse",
            "data": {"authenticationToken": "reconnect-test-token"},
        }))
        first.emit_message(json.dumps({
            "messageType": "AuthenticationResponse",
            "data": {"authenticated": True},
        }))
        first_mouth_thread = runtime.mouth_controller.worker.thread
        assert first_mouth_thread is not None
        assert first_mouth_thread.is_alive()
        assert runtime.connection.state == VTubeStudioConnectionState.AUTHENTICATED

        first.emit_close()
        assert runtime.auth_manager.is_authenticated is False
        assert runtime.connection.safe_send_control({"messageType": "blocked"}) is False
        first.run_forever_release.set()
        assert stop_event.wait_entered.wait(1.0)
        assert stop_event.wait_calls == [3.0]

        stop_event.release_retry_wait()
        assert opened_events[1].wait(1.0)
        second = sockets[1]
        assert json.loads(second.sent_payloads[0])["messageType"] == (
            "AuthenticationRequest"
        )
        second.emit_message(json.dumps({
            "messageType": "AuthenticationResponse",
            "data": {"authenticated": True},
        }))

        assert runtime.connection.state == VTubeStudioConnectionState.AUTHENTICATED
        assert runtime.auth_manager.is_authenticated is True
        assert runtime.mouth_controller.worker.thread is first_mouth_thread
        assert first_mouth_thread.is_alive()
        assert len(sockets) == 2
    finally:
        runtime.shutdown()

    assert runtime.connection.state == VTubeStudioConnectionState.STOPPED
    assert runtime.mouth_controller.worker.is_alive is False
