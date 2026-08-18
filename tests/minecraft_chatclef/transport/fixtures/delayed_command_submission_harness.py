#20260819_kpopmodder: Compose production submission with isolated delayed test collaborators.
from __future__ import annotations

import itertools
import threading

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_submitter import (
    FabricChatClefCommandSubmitter,
)
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)
from plugins.Minecraft.fabric.chatclef.transport.server.fabric_chatclef_envelope_transport import (
    FabricChatClefEnvelopeTransport,
)

from .asyncio_event_loop_thread import AsyncioEventLoopThread
from .delayed_recording_websocket import DelayedRecordingWebSocket
from .scheduled_future_tracker import ScheduledFutureTracker


class DelayedCommandSubmissionHarness:
    def __init__(self):
        self._loop_thread = AsyncioEventLoopThread()
        self._future_tracker = ScheduledFutureTracker()
        self._websocket = DelayedRecordingWebSocket()
        self._ownership = FabricChatClefConnectionOwnership()
        self._message_numbers = itertools.count(1)
        self._submitter: FabricChatClefCommandSubmitter | None = None

    @property
    def active_request_id(self) -> str | None:
        return self._ownership.active_request_id

    @property
    def scheduled_future_count(self) -> int:
        return self._future_tracker.count

    @property
    def wire_messages(self) -> list[str]:
        return self._websocket.wire_messages

    def __enter__(self) -> DelayedCommandSubmissionHarness:
        self._loop_thread.start()
        admission = self._ownership.try_activate(
            websocket=self._websocket,
            session_id="session-1",
        )
        if not admission.accepted:
            raise AssertionError("fixture connection was not accepted")

        envelope_transport = FabricChatClefEnvelopeTransport(
            message_id_factory=self._next_message_id,
            now_ms=lambda: 1,
        )
        self._submitter = FabricChatClefCommandSubmitter(
            connection_ownership=self._ownership,
            command_lock=threading.RLock(),
            diagnostics=_SilentDiagnostics(),
            loop_provider=lambda: self._loop_thread.loop,
            envelope_transport=envelope_transport,
            send_timeout_sec=0.01,
            future_scheduler=self._future_tracker.schedule,
            message_id_factory=self._next_message_id,
            now_ms=lambda: 1,
        )
        return self

    def __exit__(self, _exc_type, _exc_value, _traceback) -> None:
        try:
            if self._future_tracker.has_pending:
                self._websocket.wait_until_started()
                self._websocket.release()
            self._future_tracker.wait_for_all()
        finally:
            self._loop_thread.close()

    def submit(self, request_id: str, command: str):
        if self._submitter is None:
            raise AssertionError("submission harness has not been entered")
        return self._submitter.submit(
            CommandRequestDTO(request_id=request_id, command=command)
        )

    def wait_until_send_started(self) -> None:
        self._websocket.wait_until_started()

    def release_delayed_send(self) -> None:
        self._websocket.release()

    def wait_for_scheduled_futures(self) -> None:
        self._future_tracker.wait_for_all()

    def pending_task_count(self) -> int:
        return self._loop_thread.pending_task_count()

    def _next_message_id(self) -> str:
        return f"message-{next(self._message_numbers)}"


class _SilentDiagnostics:
    def info(self, _message) -> None:
        return None
