#20260904_kpopmodder: Verify sticky cancellation at the low-level WebSocket session boundary.
import threading

import pytest

from plugins.VtubeStudio.vtube_studio_core.connection.vtube_studio_websocket_session import (
    VTubeStudioWebSocketSession,
)
from .fakes.close_only_client import CloseOnlyClient
from .fakes.fail_first_abort_client import FailFirstAbortClient
from .fakes.fake_low_level_websocket import FakeLowLevelWebSocket


def _make_session(connect_factory, event_log):
    return VTubeStudioWebSocketSession(
        "ws://localhost:8001",
        on_open=lambda ws: event_log.append("on_open"),
        on_message=lambda ws, message: event_log.append("on_message"),
        on_error=lambda ws, error: event_log.append("on_error"),
        on_close=lambda ws, status, message: event_log.append("on_close"),
        connect_factory=connect_factory,
    )


def test_cancel_before_run_does_not_call_connect_factory():
    connect_calls = []
    event_log = []

    def connect_factory(*args, **kwargs):
        connect_calls.append((args, kwargs))
        raise AssertionError("connect must not run after cancellation")

    session = _make_session(connect_factory, event_log)

    assert session.close() is True
    assert session.run_forever() is False
    assert session.run_finished is True
    assert connect_calls == []
    assert event_log == []


def test_cancel_during_connect_closes_returned_client_before_any_callback():
    connect_entered = threading.Event()
    connect_release = threading.Event()
    event_log = []
    client = CloseOnlyClient(event_log)

    def connect_factory(*args, **kwargs):
        connect_entered.set()
        connect_release.wait()
        event_log.append("connect_returned")
        return client

    session = _make_session(connect_factory, event_log)
    runner = threading.Thread(target=session.run_forever, daemon=True)
    runner.start()

    assert connect_entered.wait(1.0)
    close_results = []
    close_finished = threading.Event()
    closer = threading.Thread(
        target=lambda: (close_results.append(session.close()), close_finished.set()),
        daemon=True,
    )
    closer.start()
    assert close_finished.wait(0.05) is False
    connect_release.set()
    runner.join(timeout=1.0)
    closer.join(timeout=1.0)

    assert runner.is_alive() is False
    assert closer.is_alive() is False
    assert close_results == [True]
    assert session.run_finished is True
    assert client.close_calls == 1
    assert event_log[0] == "connect_returned"
    assert "client_closed" in event_log
    assert "on_open" not in event_log
    assert "on_message" not in event_log
    assert "on_error" not in event_log
    assert "on_close" not in event_log


def test_connected_session_delivers_messages_sends_and_aborts_transport_once():
    event_log = []
    client = FakeLowLevelWebSocket(['{"messageType":"AuthenticationResponse"}', ""])
    connect_calls = []
    session_holder = []

    def connect_factory(*args, **kwargs):
        connect_calls.append((args, kwargs))
        return client

    def on_open(session):
        session_holder.append(session)
        event_log.append("on_open")
        session.send("auth-request")

    session = VTubeStudioWebSocketSession(
        "ws://localhost:8001",
        on_open=on_open,
        on_message=lambda ws, message: event_log.append(("on_message", message)),
        on_error=lambda ws, error: event_log.append("on_error"),
        on_close=lambda ws, status, message: event_log.append("on_close"),
        connect_factory=connect_factory,
    )

    assert session.run_forever() is True

    assert connect_calls == [
        (("ws://127.0.0.1:8001",), {"timeout": 1.0, "enable_multithread": True})
    ]
    assert session.url == "ws://localhost:8001"
    assert session.transport_url == "ws://127.0.0.1:8001"
    assert session_holder == [session]
    assert client.timeouts == [0.25]
    assert client.sent_payloads == ["auth-request"]
    assert event_log == [
        "on_open",
        ("on_message", '{"messageType":"AuthenticationResponse"}'),
        "on_close",
    ]
    assert client.shutdown_calls == 1
    assert session.run_finished is True


def test_connect_failure_preserves_error_then_close_callback_order():
    callbacks = []

    def refuse_connection(*args, **kwargs):
        raise ConnectionRefusedError("private-detail")

    session = VTubeStudioWebSocketSession(
        "ws://localhost:8001",
        on_open=lambda ws: callbacks.append(("open",)),
        on_message=lambda ws, message: callbacks.append(("message", message)),
        on_error=lambda ws, error: callbacks.append(
            ("error", type(error).__name__)
        ),
        on_close=lambda ws, status, message: callbacks.append(
            ("close", status, message)
        ),
        connect_factory=refuse_connection,
    )

    with pytest.raises(ConnectionRefusedError):
        session.run_forever()

    assert callbacks == [
        ("error", "ConnectionRefusedError"),
        ("close", None, None),
    ]
    assert session.run_finished is True


def test_failed_abort_retains_exact_client_until_close_retry_succeeds():
    event_log = []
    client = FailFirstAbortClient(event_log)
    session = _make_session(lambda *args, **kwargs: client, event_log)

    try:
        session.run_forever()
    except OSError as error:
        assert str(error) == "controlled abort failure"
    else:
        raise AssertionError("the first abort must fail")

    assert session.run_finished is True
    assert session.cleanup_pending is True
    assert session.close() is True
    assert session.cleanup_pending is False
    assert client.shutdown_calls == 2
    assert "on_close" not in event_log
