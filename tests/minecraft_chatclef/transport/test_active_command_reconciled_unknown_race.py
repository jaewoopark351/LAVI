#20260821_kpopmodder: Guard stale-active release against equality and exception races.
from __future__ import annotations

import unittest

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_active_command import (
    FabricChatClefActiveCommand,
)
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)
from plugins.Minecraft.fabric.chatclef.transport.reconciliation.reconciliation_feature_gate import (
    ReconciliationFeatureGate,
)


class ActiveCommandReconciledUnknownRaceTests(unittest.TestCase):
    def test_clear_command_if_current_requires_exact_active_object_identity(self):
        ownership, active = _active_ownership()
        equal_but_not_identical = FabricChatClefActiveCommand(
            websocket=object(),
            session_id=active.session_id,
            generation=active.generation,
            request_id=active.request_id,
            command_message_id=active.command_message_id,
            command=active.command,
            source=active.source,
            started_at_ms=active.started_at_ms,
        )

        cleared = ownership.clear_command_if_current(equal_but_not_identical)

        self.assertFalse(cleared)
        self.assertEqual(active.request_id, ownership.active_request_id)
        self.assertTrue(ownership.clear_command_if_current(active))
        self.assertIsNone(ownership.active_request_id)

    def test_accept_result_exception_preserves_previous_immutable_state(self):
        ownership, active = _active_ownership()
        ownership._reconciliation = _RaisingCoordinator()  # noqa: SLF001
        result = CommandResultDTO(
            request_id=active.request_id,
            ok=True,
            status=CommandResultStatus.RUNNING,
            message="running",
            data={},
        )

        with self.assertRaises(RuntimeError):
            ownership.accept_result_and_reconcile(
                websocket=active.websocket,
                envelope=_envelope(active, result.to_dict()),
                result=result,
                raw_payload=result.to_dict(),
            )

        snapshot = ownership.audit_snapshot()
        self.assertEqual(active.request_id, snapshot["active_request_id"])
        self.assertIsNone(snapshot["last_java_result"])
        self.assertIsNone(snapshot["local_effective_result"])
        self.assertEqual([], snapshot["tombstones"])


class _RaisingCoordinator:
    feature_gate = ReconciliationFeatureGate()

    def accept_and_apply(self, **_kwargs):
        raise RuntimeError("injected reconciliation failure")


def _active_ownership():
    websocket = object()
    ownership = FabricChatClefConnectionOwnership()
    admission = ownership.try_activate(websocket=websocket, session_id="session-1")
    if not admission.accepted:
        raise AssertionError("fixture connection was not accepted")
    active = ownership.begin_command(
        request_id="request-1",
        command_message_id="message-1",
        command="deposit diamond 2",
        source="unit-test",
    )
    if active is None:
        raise AssertionError("fixture command was not accepted")
    return ownership, active


def _envelope(
    active: FabricChatClefActiveCommand,
    payload: dict[str, object],
) -> BridgeEnvelopeDTO:
    return BridgeEnvelopeDTO(
        protocol_version=1,
        message_type=BridgeMessageType.COMMAND_RESULT,
        message_id="result-1",
        correlation_id=active.command_message_id,
        session_id=active.session_id,
        timestamp_ms=1,
        payload=payload,
    )


if __name__ == "__main__":
    unittest.main()
