#20260904_kpopmodder: Verify bounded VTube Studio retry logs use stable, credential-safe fields.
from plugins.VtubeStudio.vtube_studio_core.connection import (
    vtube_studio_connection_log as connection_log_module,
)
from plugins.VtubeStudio.vtube_studio_core.connection.vtube_studio_connection import (
    VTubeStudioConnection,
)
from .fakes.fake_websocket import FakeWebSocket
from .fakes.recording_stop_event import RecordingStopEvent


def test_refused_connection_logs_typed_reason_and_exact_retry_delay(monkeypatch):
    secret = "credential-must-not-appear"
    log_messages = []
    stop_event = RecordingStopEvent()

    monkeypatch.setattr(
        connection_log_module,
        "log_print",
        lambda message, level="info": log_messages.append((level, message)),
    )

    def websocket_factory(url, on_open, on_message, on_error, on_close):
        return FakeWebSocket(
            url=url,
            on_open=on_open,
            on_message=on_message,
            on_error=on_error,
            on_close=on_close,
            run_behavior=lambda ws: (_ for _ in ()).throw(
                ConnectionRefusedError(secret)
            ),
        )

    connection = VTubeStudioConnection(
        "ws://localhost:8001",
        websocket_app_factory=websocket_factory,
        stop_event=stop_event,
    )
    try:
        assert connection.start() is True
        assert stop_event.wait_entered.wait(1.0)

        combined_logs = "\n".join(message for _, message in log_messages)
        assert "reason=connection_refused" in combined_logs
        assert "error_type=ConnectionRefusedError" in combined_logs
        assert "event=retry_scheduled" in combined_logs
        assert "state=DISCONNECTED_WAIT" in combined_logs
        assert "retry_delay_sec=3.0" in combined_logs
        assert secret not in combined_logs

        assert connection.shutdown() is True
        count_after_shutdown = len(log_messages)
        stop_event.release_retry_wait()
        assert connection.worker_alive is False
        assert len(log_messages) == count_after_shutdown
    finally:
        connection.shutdown()


def test_attempt_log_sampling_is_bounded():
    sampled = [
        attempt
        for attempt in range(1, 17)
        if VTubeStudioConnection._should_log_attempt(attempt)
    ]

    assert sampled == [1, 2, 3, 4, 8, 16]

    long_running_sample = [
        attempt
        for attempt in range(1, 4097)
        if VTubeStudioConnection._should_log_attempt(attempt)
    ]
    assert long_running_sample == [1, 2, 3, 4, 8, 16, 32, 64]


def test_repeated_socket_close_failures_have_a_session_hard_cap(monkeypatch):
    log_messages = []
    monkeypatch.setattr(
        connection_log_module,
        "log_print",
        lambda message, level="info": log_messages.append((level, message)),
    )
    connection = VTubeStudioConnection("ws://localhost:8001")

    for _ in range(4096):
        connection._on_socket_close_result(False, OSError("private-detail"))

    assert len(log_messages) == 8
    assert "private-detail" not in "\n".join(
        message for _, message in log_messages
    )
