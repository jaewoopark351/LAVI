#20260904_kpopmodder: Verify the mouth worker owns one interruptible and idempotent thread lifecycle.
import threading

from plugins.VtubeStudio.vtube_studio_core.controllers.mouth.vtube_studio_mouth_worker import (
    VTubeStudioMouthWorker,
)
from plugins.VtubeStudio.vtube_studio_core.controllers.mouth import (
    vtube_studio_mouth_worker as mouth_worker_module,
)


def test_mouth_worker_starts_once_and_stop_interrupts_long_wait():
    updated = threading.Event()
    worker = VTubeStudioMouthWorker(
        ready_callback=lambda: True,
        update_callback=updated.set,
        refresh_interval_sec=60.0,
        join_timeout_sec=1.0,
    )

    assert worker.start() is True
    assert worker.start() is False
    assert updated.wait(1.0)
    assert worker.is_alive is True

    assert worker.stop() is True
    assert worker.stop() is True
    assert worker.is_alive is False


def test_mouth_worker_update_failures_have_a_session_hard_cap(monkeypatch):
    log_messages = []
    stop_event = threading.Event()
    update_count = 0

    def fail_update():
        nonlocal update_count
        update_count += 1
        if update_count == 4096:
            stop_event.set()
        raise RuntimeError("private-detail")

    monkeypatch.setattr(
        mouth_worker_module,
        "log_print",
        lambda message, level="info": log_messages.append((level, message)),
    )
    worker = VTubeStudioMouthWorker(
        ready_callback=lambda: True,
        update_callback=fail_update,
        refresh_interval_sec=0.0,
        stop_event=stop_event,
    )

    worker.run_loop()

    assert update_count == 4096
    assert len(log_messages) == 8
    assert "private-detail" not in "\n".join(
        message for _, message in log_messages
    )
