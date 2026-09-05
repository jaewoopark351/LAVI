#20260905_kpopmodder: Verify transport facades preserve behavior while delegating focused responsibilities.
from __future__ import annotations

import asyncio
import threading
import unittest

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.fabric.chatclef.config.fabric_chatclef_config import (
    FabricChatClefConfig,
)
from plugins.Minecraft.fabric.chatclef.diagnostics import FabricChatClefDiagnostics
from plugins.Minecraft.fabric.chatclef.session import FabricChatClefSessionRegistry
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)
from plugins.Minecraft.fabric.chatclef.transport.server.fabric_chatclef_client_handler import (
    FabricChatClefClientHandler,
)
from plugins.Minecraft.fabric.chatclef.transport.server.fabric_chatclef_websocket_server_runtime import (
    FabricChatClefWebSocketServer,
)


class TransportFacadeDelegationTests(unittest.TestCase):
    def test_connection_facade_separates_session_command_and_snapshot_owners(self):
        ownership = FabricChatClefConnectionOwnership()

        self.assertEqual(
            "FabricChatClefConnectionSession",
            type(ownership._connection_session).__name__,  # noqa: SLF001
        )
        self.assertEqual(
            "FabricChatClefOrdinaryCommandOwner",
            type(ownership._command_owner).__name__,  # noqa: SLF001
        )
        self.assertEqual(
            "FabricChatClefOwnershipSnapshotBuilder",
            type(ownership._snapshot_builder).__name__,  # noqa: SLF001
        )
        self.assertIsInstance(ownership.TERMINAL_STATUSES, frozenset)
        with self.assertRaises(AttributeError):
            ownership.TERMINAL_STATUSES.add("mutate")

    def test_client_facade_delegates_handshake_status_and_disconnect(self):
        transport = _RecordingEnvelopeTransport()
        diagnostics_messages: list[str] = []
        ownership = FabricChatClefConnectionOwnership()
        sessions = FabricChatClefSessionRegistry()
        handler = FabricChatClefClientHandler(
            connection_ownership=ownership,
            command_lock=threading.RLock(),
            session_registry=sessions,
            diagnostics=FabricChatClefDiagnostics(diagnostics_messages.append),
            envelope_transport=transport,
            command_result_handler=_RecordingResultHandler(),
            status_provider=lambda: {"status": "ready"},
            stopping_provider=lambda: False,
            last_error_reporter=lambda _message: None,
            now_ms=lambda: 10,
        )
        websocket = _message_stream(
            _envelope(BridgeMessageType.HANDSHAKE),
            _envelope(BridgeMessageType.STATUS_REQUEST),
        )

        asyncio.run(handler.handle_client(websocket))

        self.assertEqual(1, len(transport.handshake_acks))
        self.assertEqual(1, len(transport.status_snapshots))
        self.assertFalse(ownership.is_connected())
        self.assertEqual(0, sessions.count())
        self.assertTrue(any("client connected" in item for item in diagnostics_messages))
        self.assertTrue(
            any("client disconnected" in item for item in diagnostics_messages)
        )

    def test_client_dispatches_stop_terminal_before_ordinary_result(self):
        transport = _RecordingEnvelopeTransport()
        ownership = FabricChatClefConnectionOwnership()
        ordinary_results = _RecordingResultHandler()
        stop_results = _RecordingStopResultDemultiplexer()
        handler = FabricChatClefClientHandler(
            connection_ownership=ownership,
            command_lock=threading.RLock(),
            session_registry=FabricChatClefSessionRegistry(),
            diagnostics=FabricChatClefDiagnostics(lambda _message: None),
            envelope_transport=transport,
            command_result_handler=ordinary_results,
            stop_control_result_demultiplexer=stop_results,
            status_provider=dict,
            stopping_provider=lambda: False,
            last_error_reporter=lambda _message: None,
            now_ms=lambda: 10,
        )
        websocket = _message_stream(
            _envelope(BridgeMessageType.HANDSHAKE),
            _envelope(BridgeMessageType.COMMAND_RESULT),
        )

        asyncio.run(handler.handle_client(websocket))

        self.assertEqual(1, stop_results.count)
        self.assertEqual(0, ordinary_results.count)

    def test_server_facade_exposes_legacy_aliases_for_focused_graph(self):
        server = FabricChatClefWebSocketServer(
            FabricChatClefConfig(host="127.0.0.1", port=4316),
            FabricChatClefSessionRegistry(),
            FabricChatClefDiagnostics(lambda _message: None),
        )

        self.assertIs(
            server._connection_ownership,  # noqa: SLF001
            server._components.connection_ownership,  # noqa: SLF001
        )
        self.assertIs(
            server._stop_control_submitter,  # noqa: SLF001
            server._components.stop_control_submitter,  # noqa: SLF001
        )
        self.assertIs(
            server.get_stop_control_claim_registry(),
            server._components.stop_control_claim_registry,  # noqa: SLF001
        )
        self.assertEqual("ws://127.0.0.1:4316", server.endpoint)
        self.assertEqual(
            "fabric_chatclef",
            server.local_status_snapshot(enabled=True).backend_id,
        )


class _RecordingEnvelopeTransport:
    def __init__(self) -> None:
        self.handshake_acks: list[tuple[tuple, dict]] = []
        self.status_snapshots: list[tuple[tuple, dict]] = []
        self.errors: list[tuple[tuple, dict]] = []

    @staticmethod
    def parse(raw_message):
        return raw_message

    async def send_handshake_ack(self, *args, **kwargs) -> None:
        self.handshake_acks.append((args, kwargs))

    async def send_status_snapshot(self, *args, **kwargs) -> None:
        self.status_snapshots.append((args, kwargs))

    async def send_error(self, *args, **kwargs) -> None:
        self.errors.append((args, kwargs))


class _RecordingResultHandler:
    def __init__(self) -> None:
        self.count = 0

    def handle(self, _websocket, _envelope) -> None:
        self.count += 1


class _RecordingStopResultDemultiplexer:
    def __init__(self) -> None:
        self.count = 0

    def handle(self, _websocket, _envelope) -> bool:
        self.count += 1
        return True


async def _message_stream(*messages):
    for message in messages:
        yield message


def _envelope(message_type: BridgeMessageType) -> BridgeEnvelopeDTO:
    return BridgeEnvelopeDTO(
        protocol_version=1,
        message_type=message_type,
        message_id=f"message-{message_type.value}",
        correlation_id="",
        session_id="session-1",
        timestamp_ms=1,
        payload={"capabilities": {}, "metadata": {}},
    )


if __name__ == "__main__":
    unittest.main()
