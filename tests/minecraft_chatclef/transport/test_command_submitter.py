#20260818_kpopmodder: Lock ambiguous scheduled-send outcomes behind active ownership.
from __future__ import annotations

import threading
import unittest
from concurrent.futures import TimeoutError as FutureTimeoutError

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_submitter import (
    FabricChatClefCommandSubmitter,
)
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)


class FabricChatClefCommandSubmitterTests(unittest.TestCase):
    def test_scheduled_send_timeout_is_unknown_and_keeps_active_guard(self):
        ownership = _connected_ownership()
        submitter = _submitter(
            ownership,
            scheduler=lambda coroutine, _loop: _scheduled_timeout(coroutine),
        )

        result = submitter.submit(
            CommandRequestDTO(request_id="cmd-1", command="get stone 1")
        )
        second = submitter.submit(
            CommandRequestDTO(request_id="cmd-2", command="get coal 1")
        )

        self.assertEqual(CommandResultStatus.UNKNOWN, result.status)
        self.assertTrue(result.data["reconciliation_required"])
        self.assertEqual("cmd-1", ownership.active_request_id)
        self.assertEqual(CommandResultStatus.REJECTED, second.status)

    def test_pre_schedule_failure_is_rejected_and_releases_active_guard(self):
        ownership = _connected_ownership()

        def fail_before_schedule(coroutine, _loop):
            coroutine.close()
            raise RuntimeError("loop rejected scheduling")

        submitter = _submitter(ownership, scheduler=fail_before_schedule)

        result = submitter.submit(
            CommandRequestDTO(request_id="cmd-1", command="get stone 1")
        )

        self.assertEqual(CommandResultStatus.REJECTED, result.status)
        self.assertIsNone(ownership.active_request_id)


class _Loop:
    def is_running(self):
        return True


class _EnvelopeTransport:
    async def send(self, _websocket, _envelope):
        return None


class _Diagnostics:
    def info(self, _message):
        return None


class _TimedOutFuture:
    def result(self, timeout):
        raise FutureTimeoutError(f"timed out after {timeout}")


def _scheduled_timeout(coroutine):
    coroutine.close()
    return _TimedOutFuture()


def _connected_ownership():
    ownership = FabricChatClefConnectionOwnership()
    admission = ownership.try_activate(websocket=object(), session_id="session-1")
    if not admission.accepted:
        raise AssertionError("fixture connection was not accepted")
    return ownership


def _submitter(ownership, *, scheduler):
    lock = threading.RLock()
    return FabricChatClefCommandSubmitter(
        connection_ownership=ownership,
        command_lock=lock,
        diagnostics=_Diagnostics(),
        loop_provider=lambda: _Loop(),
        envelope_transport=_EnvelopeTransport(),
        send_timeout_sec=0.01,
        future_scheduler=scheduler,
        message_id_factory=lambda: "message-1",
        now_ms=lambda: 1,
    )


if __name__ == "__main__":
    unittest.main()
