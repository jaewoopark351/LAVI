#20260801_kpopmodder: Verify Fabric ChatClef Python transport through Phase 4.
import asyncio
import json
import socket
import unittest
from urllib.parse import urlparse

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.bridge_lifecycle_state import (
    BridgeLifecycleState,
)
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)
from plugins.Minecraft.fabric.chatclef.adapter.fabric_chatclef_adapter import (
    FabricChatClefAdapter,
)
from plugins.Minecraft.fabric.chatclef.config.fabric_chatclef_config import (
    FabricChatClefConfig,
)


class MinecraftFabricChatClefTransportTests(unittest.TestCase):
    def test_enabled_adapter_starts_local_server_and_accepts_handshake(self):
        adapter = FabricChatClefAdapter(
            config=FabricChatClefConfig(
                enabled=True,
                host="127.0.0.1",
                port=0,
                startup_timeout_sec=2.0,
            )
        )
        try:
            adapter.start()
            status = adapter.get_status()

            self.assertTrue(status.enabled)
            self.assertFalse(status.connected)
            self.assertEqual(BridgeLifecycleState.DISCONNECTED, status.lifecycle_state)

            asyncio.run(self._exercise_handshake(adapter))
        finally:
            adapter.stop()

    def test_java_http_client_upgrade_with_content_length_zero_is_accepted(self):
        adapter = FabricChatClefAdapter(
            config=FabricChatClefConfig(
                enabled=True,
                host="127.0.0.1",
                port=0,
                startup_timeout_sec=2.0,
            )
        )
        try:
            adapter.start()
            endpoint = adapter.get_status().details["endpoint"]
            parsed = urlparse(endpoint)
            host = parsed.hostname or "127.0.0.1"
            port = parsed.port or 4316

            with socket.create_connection((host, port), timeout=2.0) as sock:
                sock.settimeout(2.0)
                request = (
                    "GET / HTTP/1.1\r\n"
                    "Connection: Upgrade\r\n"
                    "Content-Length: 0\r\n"
                    f"Host: {host}:{port}\r\n"
                    "Upgrade: websocket\r\n"
                    "User-Agent: Java-http-client/17.0.15\r\n"
                    "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n"
                    "Sec-WebSocket-Version: 13\r\n"
                    "\r\n"
                )
                sock.sendall(request.encode("ascii"))
                response = sock.recv(4096).decode("latin1")

            self.assertIn("101 Switching Protocols", response)
            self.assertIn("Upgrade: websocket", response)
        finally:
            adapter.stop()

    async def _exercise_handshake(self, adapter):
        import websockets

        endpoint = adapter.get_status().details["endpoint"]
        async with websockets.connect(endpoint) as websocket:
            request = BridgeEnvelopeDTO(
                protocol_version=1,
                message_type=BridgeMessageType.HANDSHAKE,
                message_id="hello-1",
                correlation_id=None,
                session_id=None,
                timestamp_ms=1,
                payload={
                    "capabilities": {
                        "chatclef": True,
                        "chatclef_command_dispatch": True,
                    },
                    "metadata": {"runtime": "test"},
                },
            )
            await websocket.send(json.dumps(request.to_dict()))
            raw_ack = await asyncio.wait_for(websocket.recv(), timeout=2.0)
            ack = json.loads(raw_ack)

            self.assertEqual(BridgeMessageType.HANDSHAKE_ACK.value, ack["message_type"])
            self.assertEqual("hello-1", ack["correlation_id"])
            self.assertTrue(ack["payload"]["accepted"])
            self.assertTrue(ack["payload"]["session_id"])

            connected_status = adapter.get_status()
            self.assertTrue(connected_status.connected)
            self.assertEqual(
                BridgeLifecycleState.CONNECTED,
                connected_status.lifecycle_state,
            )

            result = adapter.submit_command(
                CommandRequestDTO(request_id="cmd-1", command="@get dirt 1")
            )

            self.assertTrue(result.ok)
            self.assertEqual(CommandResultStatus.ACCEPTED, result.status)

            raw_command = await asyncio.wait_for(websocket.recv(), timeout=2.0)
            command = json.loads(raw_command)
            self.assertEqual(
                BridgeMessageType.COMMAND_REQUEST.value,
                command["message_type"],
            )
            self.assertEqual("cmd-1", command["payload"]["request_id"])
            self.assertEqual("@get dirt 1", command["payload"]["command"])

            busy_result = adapter.submit_command(
                CommandRequestDTO(request_id="cmd-2", command="@get stone 1")
            )
            self.assertFalse(busy_result.ok)
            self.assertEqual(CommandResultStatus.REJECTED, busy_result.status)
            self.assertEqual(BridgeErrorCode.INVALID_REQUEST, busy_result.error_code)

            command_result = BridgeEnvelopeDTO(
                protocol_version=1,
                message_type=BridgeMessageType.COMMAND_RESULT,
                message_id="result-1",
                correlation_id=command["message_id"],
                session_id=ack["payload"]["session_id"],
                timestamp_ms=2,
                payload={
                    "request_id": "cmd-1",
                    "ok": True,
                    "status": CommandResultStatus.COMPLETED.value,
                    "error_code": None,
                    "message": "done",
                    "data": {},
                },
            )
            await websocket.send(json.dumps(command_result.to_dict()))
            await asyncio.sleep(0.05)

            next_result = adapter.submit_command(
                CommandRequestDTO(request_id="cmd-3", command="@get stone 1")
            )
            self.assertTrue(next_result.ok)
            self.assertEqual(CommandResultStatus.ACCEPTED, next_result.status)

            unknown_result = BridgeEnvelopeDTO(
                protocol_version=1,
                message_type=BridgeMessageType.COMMAND_RESULT,
                message_id="result-2",
                correlation_id=command["message_id"],
                session_id=ack["payload"]["session_id"],
                timestamp_ms=3,
                payload={
                    "request_id": "cmd-3",
                    "ok": False,
                    "status": CommandResultStatus.UNKNOWN.value,
                    "error_code": None,
                    "message": "callback finished without verified goal success",
                    "data": {},
                },
            )
            await websocket.send(json.dumps(unknown_result.to_dict()))
            await asyncio.sleep(0.05)

            after_unknown_result = adapter.submit_command(
                CommandRequestDTO(request_id="cmd-4", command="@get oak_log 1")
            )
            self.assertTrue(after_unknown_result.ok)
            self.assertEqual(CommandResultStatus.ACCEPTED, after_unknown_result.status)


if __name__ == "__main__":
    unittest.main()
