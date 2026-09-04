#20260904_kpopmodder: Verify bounded runtime cleanup and retryable ownership after partial failures.
import tempfile
import threading
import time
from pathlib import Path
from types import SimpleNamespace

from core.event_manager_core.event_manager import EventManager
from core.event_manager_core.event_type import EventType
from plugins.VtubeStudio.vtube_studio_core.connection.vtube_studio_connection import (
    VTubeStudioConnection,
)
from plugins.VtubeStudio.vtube_studio_core.runtime.vtube_studio_component_factory import (
    VTubeStudioComponentFactory,
)
from plugins.VtubeStudio.vtube_studio_core.runtime.vtube_studio_runtime import (
    VTubeStudioRuntime,
)

from .fakes.controlled_send_websocket import ControlledSendWebSocket
from .fakes.auth_manager_fake import AuthManagerFake
from .fakes.blocking_controller_fake import BlockingControllerFake
from .fakes.controller_fake import ControllerFake
from .fakes.failing_once_subscription import FailingOnceSubscription
from .fakes.fake_websocket import FakeWebSocket
from .fakes.mouth_controller_fake import MouthControllerFake
from .fakes.runtime_connection_fake import RuntimeConnectionFake
from .fakes.song_controller_fake import SongControllerFake
from .fakes.subscription_manager_fake import SubscriptionManagerFake
from .fakes.switchable_blocking_send_websocket import (
    SwitchableBlockingSendWebSocket,
)


def test_runtime_shutdown_completes_while_authentication_send_is_blocked():
    runtime, socket = _start_composed_runtime(ControlledSendWebSocket)
    assert socket.send_entered.wait(1.0)

    result, elapsed = _call_shutdown_with_deadline(runtime)

    assert result is True
    assert elapsed < 1.5
    assert runtime.connection.worker_alive is False
    assert socket.close_calls == 1


def test_runtime_shutdown_completes_while_interrupt_send_is_blocked():
    event_manager = EventManager()
    runtime, socket = _start_composed_runtime(
        SwitchableBlockingSendWebSocket,
        event_manager=event_manager,
    )
    assert socket.opened.wait(1.0)
    assert runtime.connection.mark_authenticated(
        runtime.connection.current_attempt_id
    ) is True

    socket.block_sends = True
    interrupt_finished = threading.Event()
    interrupt_thread = threading.Thread(
        target=lambda: (
            event_manager.trigger(EventType.INTERRUPT),
            interrupt_finished.set(),
        ),
        daemon=True,
    )
    interrupt_thread.start()
    assert socket.send_entered.wait(1.0)

    result, elapsed = _call_shutdown_with_deadline(runtime)

    assert result is True
    assert elapsed < 1.5
    assert interrupt_finished.wait(1.0)
    assert runtime.connection.worker_alive is False
    assert socket.close_calls == 1
    interrupt_thread.join(timeout=1.0)


def test_partial_avatar_startup_rolls_back_mouth_and_next_attempt_restarts_it():
    connection = RuntimeConnectionFake()
    mouth = MouthControllerFake()
    speaking = ControllerFake(fail_start_count=1)
    blink = ControllerFake()
    idle = ControllerFake()
    smile = ControllerFake()
    song = SongControllerFake()
    runtime = VTubeStudioRuntime(
        connection=connection,
        auth_manager=AuthManagerFake(),
        mouth_controller=mouth,
        song_expression_controller=song,
        blink_controller=blink,
        speaking_pose_controller=speaking,
        idle_pose_controller=idle,
        smile_controller=smile,
        event_manager=SubscriptionManagerFake(),
    )

    assert runtime.on_authenticated(1) is False
    assert connection.disconnections == [(1, "avatar_controller_start_failed")]
    assert mouth.start_calls == 1
    assert mouth.stop_calls == 1
    assert mouth.worker.thread.is_alive() is False

    connection.begin_attempt(2)
    assert runtime.on_authenticated(2) is True
    assert mouth.start_calls == 2
    assert mouth.worker.thread.is_alive() is True
    assert speaking.start_calls == 2
    assert blink.start_calls == 1
    assert idle.start_calls == 1
    assert smile.start_calls == 1

    assert runtime.shutdown() is True


def test_failed_event_unsubscribe_is_retained_and_retried_on_later_shutdown():
    subscription = FailingOnceSubscription()
    event_manager = SubscriptionManagerFake(subscription)
    connection = RuntimeConnectionFake()
    mouth = MouthControllerFake()
    runtime = VTubeStudioRuntime(
        connection=connection,
        auth_manager=AuthManagerFake(),
        mouth_controller=mouth,
        song_expression_controller=SongControllerFake(),
        blink_controller=ControllerFake(),
        speaking_pose_controller=ControllerFake(),
        idle_pose_controller=ControllerFake(),
        smile_controller=ControllerFake(),
        event_manager=event_manager,
    )

    assert runtime.initialize() is True
    assert runtime.shutdown() is False
    assert subscription.unsubscribe_calls == 1
    assert runtime._event_subscription is subscription

    assert runtime.shutdown() is True
    assert subscription.unsubscribe_calls == 2
    assert runtime._event_subscription is None
    assert connection.shutdown_calls == 2


def test_blocked_controller_start_keeps_shutdown_pending_and_rolls_back_late_start():
    connection = RuntimeConnectionFake()
    speaking = BlockingControllerFake()
    runtime = VTubeStudioRuntime(
        connection=connection,
        auth_manager=AuthManagerFake(),
        mouth_controller=MouthControllerFake(),
        song_expression_controller=SongControllerFake(),
        blink_controller=ControllerFake(),
        speaking_pose_controller=speaking,
        idle_pose_controller=ControllerFake(),
        smile_controller=ControllerFake(),
        event_manager=SubscriptionManagerFake(),
    )
    authentication_result = []
    authentication_finished = threading.Event()
    authentication_thread = threading.Thread(
        target=lambda: (
            authentication_result.append(runtime.on_authenticated(1)),
            authentication_finished.set(),
        ),
        daemon=True,
    )
    authentication_thread.start()
    assert speaking.start_entered.wait(1.0)

    started_at = time.monotonic()
    assert runtime.shutdown() is False
    assert time.monotonic() - started_at < 0.5
    assert runtime.shutting_down is True
    assert runtime._stopped is False
    assert runtime.avatar_controller_group.cleanup_pending is True
    assert runtime.start_avatar_threads() is False
    assert speaking.start_calls == 1

    speaking.release_start.set()
    assert authentication_finished.wait(1.0)
    authentication_thread.join(timeout=1.0)
    assert authentication_result == [False]
    assert speaking.thread.is_alive() is False
    assert speaking.stop_calls >= 2

    assert runtime.shutdown() is True
    assert runtime.avatar_controller_group.cleanup_pending is False


def _start_composed_runtime(socket_type, event_manager=None):
    module_directory = Path(tempfile.mkdtemp(prefix="vtube-runtime-shutdown-"))
    socket_holder = []

    def websocket_factory(url, on_open, on_message, on_error, on_close):
        socket = socket_type(
            url=url,
            on_open=on_open,
            on_message=on_message,
            on_error=on_error,
            on_close=on_close,
            run_behavior=lambda ws: (
                ws.emit_open(),
                getattr(ws, "opened", threading.Event()).set(),
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

    runtime, _ = VTubeStudioComponentFactory(
        event_manager_instance=event_manager or EventManager(),
        connection_factory=connection_factory,
    ).create(
        module_directory=str(module_directory),
        avatar_data_callback=lambda: SimpleNamespace(mouth_open=0.0),
    )
    assert runtime.initialize() is True
    assert runtime.start_connection() is True
    deadline = time.monotonic() + 1.0
    while not socket_holder and time.monotonic() < deadline:
        threading.Event().wait(0.001)
    assert socket_holder
    return runtime, socket_holder[0]


def _call_shutdown_with_deadline(runtime):
    result = []
    finished = threading.Event()
    started_at = time.monotonic()
    shutdown_thread = threading.Thread(
        target=lambda: (result.append(runtime.shutdown()), finished.set()),
        daemon=True,
    )
    shutdown_thread.start()
    try:
        assert finished.wait(1.5)
        return result[0], time.monotonic() - started_at
    finally:
        runtime.shutdown()
        shutdown_thread.join(timeout=1.0)
