#20260904_kpopmodder: Verify the production VtubeStudio facade owns legacy callback signatures over the new runtime.
import json
import tempfile
import threading
from pathlib import Path
from types import SimpleNamespace

from core.event_manager_core.event_manager import EventManager
from plugins.VtubeStudio.VtubeStudio import VtubeStudio
from plugins.VtubeStudio.vtube_studio_core.connection.vtube_studio_connection import (
    VTubeStudioConnection,
)
from plugins.VtubeStudio.vtube_studio_core.connection.vtube_studio_connection_state import (
    VTubeStudioConnectionState,
)
from plugins.VtubeStudio.vtube_studio_core.connection.vtube_studio_websocket_session import (
    VTubeStudioWebSocketSession,
)
from plugins.VtubeStudio.vtube_studio_core.runtime.vtube_studio_component_factory import (
    VTubeStudioComponentFactory,
)

from .fakes.fake_websocket import FakeWebSocket
from .fakes.recording_stop_event import RecordingStopEvent


def _bind_facade_runtime(facade, runtime, token_store):
    facade.runtime = runtime
    facade.token_store = token_store
    facade.auth_manager = runtime.auth_manager
    facade.connection = runtime.connection
    facade.mouth_controller = runtime.mouth_controller
    facade.song_expression_controller = runtime.song_expression_controller
    facade.blink_controller = runtime.blink_controller
    facade.speaking_pose_controller = runtime.speaking_pose_controller
    facade.idle_pose_controller = runtime.idle_pose_controller
    facade.smile_controller = runtime.smile_controller


def test_facade_callbacks_connect_authenticate_close_and_keep_legacy_arg_counts():
    module_directory = Path(tempfile.mkdtemp(prefix="vtube-facade-callback-"))
    socket_holder = []
    opened = threading.Event()

    def websocket_factory(url, on_open, on_message, on_error, on_close):
        socket = FakeWebSocket(
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
        return socket

    def connection_factory(**kwargs):
        return VTubeStudioConnection(
            **kwargs,
            websocket_app_factory=websocket_factory,
        )

    facade = object.__new__(VtubeStudio)
    facade.avatar_data = SimpleNamespace(mouth_open=0.0)
    runtime, token_store = VTubeStudioComponentFactory(
        event_manager_instance=EventManager(),
        connection_factory=connection_factory,
    ).create(
        module_directory=str(module_directory),
        avatar_data_callback=lambda: facade.avatar_data,
        callback_owner=facade,
    )
    _bind_facade_runtime(facade, runtime, token_store)

    callback_arg_counts = {"open": [], "message": [], "close": []}
    original_open = facade.on_open
    original_message = facade.on_message
    original_close = facade.on_close

    def record_open(*args):
        callback_arg_counts["open"].append(len(args))
        return original_open(*args)

    def record_message(*args):
        callback_arg_counts["message"].append(len(args))
        return original_message(*args)

    def record_close(*args):
        callback_arg_counts["close"].append(len(args))
        return original_close(*args)

    facade.on_open = record_open
    facade.on_message = record_message
    facade.on_close = record_close

    try:
        assert runtime.initialize() is True
        assert runtime.start_connection() is True
        assert opened.wait(1.0)
        socket = socket_holder[0]
        assert json.loads(socket.sent_payloads[0])["messageType"] == (
            "AuthenticationTokenRequest"
        )

        socket.emit_message(json.dumps({
            "messageType": "AuthenticationTokenResponse",
            "data": {"authenticationToken": "facade-test-token"},
        }))
        assert json.loads(socket.sent_payloads[1])["messageType"] == (
            "AuthenticationRequest"
        )
        socket.emit_message(json.dumps({
            "messageType": "AuthenticationResponse",
            "data": {"authenticated": True},
        }))

        assert runtime.connection.state == VTubeStudioConnectionState.AUTHENTICATED
        assert facade.isAuthenticated is True
        assert runtime.mouth_controller.worker.is_alive is True

        socket.emit_close()
        assert facade.isAuthenticated is False
        assert runtime.connection.state == VTubeStudioConnectionState.DISCONNECTED_WAIT
        assert callback_arg_counts == {
            "open": [1],
            "message": [2, 2],
            "close": [3],
        }
    finally:
        runtime.shutdown()

    assert runtime.connection.state == VTubeStudioConnectionState.STOPPED
    assert runtime.mouth_controller.worker.is_alive is False


def test_facade_connect_failure_keeps_legacy_error_then_close_signatures():
    module_directory = Path(tempfile.mkdtemp(prefix="vtube-facade-refusal-"))
    stop_event = RecordingStopEvent()

    def refuse_connection(*args, **kwargs):
        raise ConnectionRefusedError("private-detail")

    def websocket_factory(url, on_open, on_message, on_error, on_close):
        return VTubeStudioWebSocketSession(
            url,
            on_open=on_open,
            on_message=on_message,
            on_error=on_error,
            on_close=on_close,
            connect_factory=refuse_connection,
        )

    def connection_factory(**kwargs):
        return VTubeStudioConnection(
            **kwargs,
            websocket_app_factory=websocket_factory,
            stop_event=stop_event,
        )

    facade = object.__new__(VtubeStudio)
    facade.avatar_data = SimpleNamespace(mouth_open=0.0)
    runtime, token_store = VTubeStudioComponentFactory(
        event_manager_instance=EventManager(),
        connection_factory=connection_factory,
    ).create(
        module_directory=str(module_directory),
        avatar_data_callback=lambda: facade.avatar_data,
        callback_owner=facade,
    )
    _bind_facade_runtime(facade, runtime, token_store)
    callback_order = []
    original_error = facade.on_error
    original_close = facade.on_close

    def record_error(*args):
        callback_order.append(("error", len(args)))
        return original_error(*args)

    def record_close(*args):
        callback_order.append(("close", len(args)))
        return original_close(*args)

    facade.on_error = record_error
    facade.on_close = record_close

    try:
        assert runtime.initialize() is True
        assert runtime.start_connection() is True
        assert stop_event.wait_entered.wait(1.0)
        assert callback_order == [("error", 2), ("close", 3)]
        assert runtime.connection.state == VTubeStudioConnectionState.DISCONNECTED_WAIT
    finally:
        runtime.shutdown()

    assert runtime.connection.state == VTubeStudioConnectionState.STOPPED
