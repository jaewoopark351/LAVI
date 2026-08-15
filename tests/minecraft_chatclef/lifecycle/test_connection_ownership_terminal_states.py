#20260815_kpopmodder: Lock command ownership clearing to terminal bridge results.
from __future__ import annotations

import unittest

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)


class FabricChatClefConnectionOwnershipTerminalStateTests(unittest.TestCase):
    def test_nonterminal_results_do_not_clear_active_command(self):
        for status in [CommandResultStatus.ACCEPTED, CommandResultStatus.RUNNING]:
            with self.subTest(status=status.value):
                ownership, websocket = self._active_ownership()

                accepted, reason = ownership.accept_result(
                    websocket=websocket,
                    envelope=self._envelope(),
                    result=self._result(status),
                )

                self.assertTrue(accepted)
                self.assertEqual("accepted", reason)
                snapshot = ownership.snapshot()
                self.assertEqual("request-1", snapshot["active_request_id"])
                self.assertEqual("get stone 1", snapshot["active_command"])
                self.assertEqual(status.value, snapshot["last_result"]["status"])

    def test_terminal_results_clear_active_command(self):
        terminal_statuses = [
            CommandResultStatus.COMPLETED,
            CommandResultStatus.REJECTED,
            CommandResultStatus.FAILED,
            CommandResultStatus.CANCELLED,
            CommandResultStatus.DEADLINE_EXCEEDED,
            CommandResultStatus.UNKNOWN,
        ]

        for status in terminal_statuses:
            with self.subTest(status=status.value):
                ownership, websocket = self._active_ownership()

                accepted, reason = ownership.accept_result(
                    websocket=websocket,
                    envelope=self._envelope(),
                    result=self._result(status),
                )

                self.assertTrue(accepted)
                self.assertEqual("accepted", reason)
                snapshot = ownership.snapshot()
                self.assertIsNone(snapshot["active_request_id"])
                self.assertIsNone(snapshot["active_command"])
                self.assertEqual(status.value, snapshot["last_result"]["status"])

    def test_second_command_cannot_begin_until_terminal_result_arrives(self):
        ownership, websocket = self._active_ownership()

        blocked = ownership.begin_command(
            request_id="request-2",
            command_message_id="message-2",
            command="get diamond 1",
            source="unit-test",
        )

        self.assertIsNone(blocked)
        self.assertEqual("request-1", ownership.snapshot()["active_request_id"])

        ownership.accept_result(
            websocket=websocket,
            envelope=self._envelope(),
            result=self._result(CommandResultStatus.COMPLETED),
        )
        next_command = ownership.begin_command(
            request_id="request-2",
            command_message_id="message-2",
            command="get diamond 1",
            source="unit-test",
        )

        self.assertIsNotNone(next_command)
        self.assertEqual("request-2", ownership.snapshot()["active_request_id"])

    def test_stale_terminal_result_cannot_clear_active_command(self):
        ownership, websocket = self._active_ownership()

        accepted, reason = ownership.accept_result(
            websocket=websocket,
            envelope=self._envelope(correlation_id="wrong-message"),
            result=self._result(CommandResultStatus.COMPLETED),
        )

        self.assertFalse(accepted)
        self.assertEqual("result_correlation_mismatch", reason)
        snapshot = ownership.snapshot()
        self.assertEqual("request-1", snapshot["active_request_id"])
        self.assertIsNone(snapshot["last_result"])

    def _active_ownership(self):
        websocket = object()
        ownership = FabricChatClefConnectionOwnership()
        admission = ownership.try_activate(
            websocket=websocket,
            session_id="session-1",
        )
        self.assertTrue(admission.accepted)
        command = ownership.begin_command(
            request_id="request-1",
            command_message_id="message-1",
            command="get stone 1",
            source="unit-test",
        )
        self.assertIsNotNone(command)
        return ownership, websocket

    def _envelope(self, *, correlation_id: str = "message-1") -> BridgeEnvelopeDTO:
        return BridgeEnvelopeDTO(
            protocol_version=1,
            message_type=BridgeMessageType.COMMAND_RESULT,
            message_id="result-1",
            correlation_id=correlation_id,
            session_id="session-1",
            timestamp_ms=1,
            payload={},
        )

    def _result(self, status: CommandResultStatus) -> CommandResultDTO:
        return CommandResultDTO(
            request_id="request-1",
            ok=status.ok,
            status=status,
            error_code=None,
            message=f"{status.value} result",
            data={},
        )


if __name__ == "__main__":
    unittest.main()

