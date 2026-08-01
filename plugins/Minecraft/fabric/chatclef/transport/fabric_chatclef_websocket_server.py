#20260801_kpopmodder: Add the Fabric ChatClef-only Python WebSocket server.
from __future__ import annotations

import asyncio
import json
import threading
import time
import uuid
from typing import Any

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.dto.status_snapshot_dto import StatusSnapshotDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.bridge_lifecycle_state import (
    BridgeLifecycleState,
)
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)
from plugins.Minecraft.fabric.chatclef.config.fabric_chatclef_config import (
    FabricChatClefConfig,
)
from plugins.Minecraft.fabric.chatclef.diagnostics import FabricChatClefDiagnostics
from plugins.Minecraft.fabric.chatclef.session import FabricChatClefSessionRegistry
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_websocket_server_factory import (
    serve_fabric_chatclef_websocket,
)


class FabricChatClefWebSocketServer:
    def __init__(
        self,
        config: FabricChatClefConfig,
        session_registry: FabricChatClefSessionRegistry,
        diagnostics: FabricChatClefDiagnostics,
    ):
        self._config = config
        self._session_registry = session_registry
        self._diagnostics = diagnostics
        self._thread: threading.Thread | None = None
        self._loop: asyncio.AbstractEventLoop | None = None
        self._server: Any = None
        self._started_event = threading.Event()
        self._start_error: str | None = None
        self._last_error: str | None = None
        self._bound_host = config.host
        self._bound_port = config.port
        self._stopping = False
        self._command_lock = threading.RLock()
        self._connection_ownership = FabricChatClefConnectionOwnership()

    @property
    def endpoint(self) -> str:
        return f"ws://{self._bound_host}:{self._bound_port}"

    @property
    def last_error(self) -> str | None:
        return self._last_error

    @property
    def is_running(self) -> bool:
        return (
            self._server is not None
            and self._thread is not None
            and self._thread.is_alive()
        )

    def start(self) -> None:
        if self.is_running:
            return
        self._started_event.clear()
        self._start_error = None
        self._last_error = None
        self._stopping = False
        self._thread = threading.Thread(
            target=self._run_loop,
            name="LAVI-MinecraftFabricChatClef-WebSocket",
            daemon=True,
        )
        self._thread.start()
        if not self._started_event.wait(self._config.startup_timeout_sec):
            self._last_error = "Fabric ChatClef WebSocket startup timed out."
            raise RuntimeError(self._last_error)
        if self._start_error:
            raise RuntimeError(self._start_error)

    def stop(self) -> None:
        self._stopping = True
        loop = self._loop
        if loop is not None and loop.is_running():
            future = asyncio.run_coroutine_threadsafe(self._close_async(), loop)
            future.result(timeout=self._config.startup_timeout_sec)
            loop.call_soon_threadsafe(loop.stop)
        if self._thread is not None:
            self._thread.join(timeout=self._config.startup_timeout_sec)
        self._session_registry.clear()
        with self._command_lock:
            self._connection_ownership.clear()
        self._thread = None
        self._loop = None
        self._server = None
        self._stopping = False

    def status_snapshot(self, *, enabled: bool) -> StatusSnapshotDTO:
        with self._command_lock:
            connected = self._connection_ownership.is_connected()
        if not enabled:
            state = BridgeLifecycleState.DISABLED
            detail = "Fabric ChatClef bridge is disabled."
        elif self._last_error:
            state = BridgeLifecycleState.FAILED
            detail = "Fabric ChatClef WebSocket server failed."
        elif connected:
            state = BridgeLifecycleState.CONNECTED
            detail = "Fabric ChatClef bridge client is connected."
        elif self.is_running:
            state = BridgeLifecycleState.DISCONNECTED
            detail = "Fabric ChatClef WebSocket server is waiting for client."
        else:
            state = BridgeLifecycleState.STOPPED
            detail = "Fabric ChatClef WebSocket server is stopped."
        return StatusSnapshotDTO(
            backend_id="fabric_chatclef",
            enabled=enabled,
            connected=connected,
            lifecycle_state=state,
            detail=detail,
            details={
                "endpoint": self.endpoint,
                "host": self._bound_host,
                "port": self._bound_port,
                "sessions": self._session_registry.snapshot(),
                "commands": self._command_status(),
            },
            last_error_code=BridgeErrorCode.INTERNAL_ERROR if self._last_error else None,
            last_error_message=self._last_error,
        )

    def submit_command(self, request: CommandRequestDTO) -> CommandResultDTO:
        command_request = CommandRequestDTO.from_mapping(request)
        if not command_request.request_id:
            command_request = CommandRequestDTO(
                request_id=f"lavi-command-{uuid.uuid4().hex}",
                command=command_request.command,
                source=command_request.source,
                deadline_ms=command_request.deadline_ms,
                metadata=command_request.metadata,
            )
        with self._command_lock:
            if (
                not self._connection_ownership.is_connected()
                or self._loop is None
                or not self._loop.is_running()
            ):
                return self._command_rejection(
                    command_request,
                    BridgeErrorCode.NOT_CONNECTED,
                    "Fabric ChatClef bridge client is not connected.",
                )
            active_request_id = self._connection_ownership.active_request_id
            if active_request_id is not None:
                return self._command_rejection(
                    command_request,
                    BridgeErrorCode.INVALID_REQUEST,
                    f"Fabric ChatClef command already active: {active_request_id}",
                )
            message_id = self._new_message_id()
            command_context = self._connection_ownership.begin_command(
                request_id=command_request.request_id,
                command_message_id=message_id,
            )
            if command_context is None:
                return self._command_rejection(
                    command_request,
                    BridgeErrorCode.NOT_CONNECTED,
                    "Fabric ChatClef bridge client is not connected.",
                )
            websocket = command_context.websocket

        envelope = BridgeEnvelopeDTO(
            protocol_version=1,
            message_type=BridgeMessageType.COMMAND_REQUEST,
            message_id=message_id,
            correlation_id=command_request.request_id,
            session_id=command_context.session_id,
            timestamp_ms=self._now_ms(),
            payload=command_request.to_dict(),
        )
        future = asyncio.run_coroutine_threadsafe(
            self._send_envelope(websocket, envelope),
            self._loop,
        )
        try:
            future.result(timeout=self._config.startup_timeout_sec)
        except Exception as error:
            with self._command_lock:
                self._connection_ownership.clear_command_if_current(command_context)
            return self._command_rejection(
                command_request,
                BridgeErrorCode.INTERNAL_ERROR,
                f"Fabric ChatClef command send failed: {type(error).__name__}: {error}",
            )
        return CommandResultDTO(
            request_id=command_request.request_id,
            ok=True,
            status=CommandResultStatus.ACCEPTED,
            error_code=None,
            message="Fabric ChatClef command sent to Java bridge.",
            data={
                "session_id": command_context.session_id,
                "connection_generation": command_context.generation,
                "command_message_id": command_context.command_message_id,
            },
        )

    def _run_loop(self) -> None:
        loop = asyncio.new_event_loop()
        self._loop = loop
        asyncio.set_event_loop(loop)
        try:
            loop.run_until_complete(self._start_async())
            self._started_event.set()
            loop.run_forever()
        except Exception as error:
            self._start_error = (
                f"Fabric ChatClef WebSocket server failed: "
                f"{type(error).__name__}: {error}"
            )
            self._last_error = self._start_error
            self._started_event.set()
        finally:
            try:
                loop.run_until_complete(self._close_async())
            finally:
                loop.close()

    async def _start_async(self) -> None:
        self._server = await serve_fabric_chatclef_websocket(
            self._handle_legacy_client,
            self._config.host,
            self._config.port,
        )
        socket = self._server.sockets[0] if self._server.sockets else None
        if socket is not None:
            sockname = socket.getsockname()
            self._bound_host = str(sockname[0])
            self._bound_port = int(sockname[1])
        self._diagnostics.info(f"server started at {self.endpoint}")

    async def _close_async(self) -> None:
        server = self._server
        if server is None:
            return
        self._server = None
        server.close()
        await server.wait_closed()
        self._session_registry.clear()
        with self._command_lock:
            self._connection_ownership.clear()
        if not self._stopping:
            self._diagnostics.info("server closed")

    async def _handle_legacy_client(self, websocket: Any, _path: str = "") -> None:
        await self._handle_client(websocket)

    async def _handle_client(self, websocket: Any) -> None:
        session_id: str | None = None
        accepted_session_id: str | None = None
        connection_generation = 0
        try:
            async for raw_message in websocket:
                envelope = self._parse_envelope(raw_message)
                if envelope is None:
                    await self._send_error(
                        websocket,
                        error_code=BridgeErrorCode.INVALID_REQUEST,
                        message="Invalid Fabric ChatClef bridge JSON envelope.",
                    )
                    continue

                if envelope.protocol_version != 1:
                    await self._send_error(
                        websocket,
                        request=envelope,
                        error_code=BridgeErrorCode.UNSUPPORTED_PROTOCOL_VERSION,
                        message="Only Fabric ChatClef bridge protocol v1 is supported.",
                    )
                    continue

                if envelope.message_type == BridgeMessageType.HANDSHAKE:
                    session_id = self._session_id_for(envelope)
                    with self._command_lock:
                        admission = self._connection_ownership.try_activate(
                            websocket=websocket,
                            session_id=session_id,
                        )
                    if not admission.accepted:
                        await self._send_handshake_ack(
                            websocket,
                            envelope,
                            session_id,
                            accepted=False,
                            message=admission.reason,
                            connection_generation=admission.generation,
                        )
                        self._diagnostics.warning(
                            "rejected duplicate client handshake "
                            f"session={session_id} reason={admission.reason}"
                        )
                        continue

                    accepted_session_id = session_id
                    connection_generation = admission.generation
                    self._session_registry.upsert(
                        session_id=session_id,
                        timestamp_ms=self._now_ms(),
                        protocol_version=envelope.protocol_version,
                        capabilities=self._payload_dict(
                            envelope.payload.get("capabilities")
                        ),
                        metadata=self._payload_dict(envelope.payload.get("metadata")),
                    )
                    await self._send_handshake_ack(
                        websocket,
                        envelope,
                        session_id,
                        accepted=True,
                        connection_generation=connection_generation,
                    )
                    self._diagnostics.info(
                        "client connected "
                        f"session={session_id} generation={connection_generation}"
                    )
                    continue

                with self._command_lock:
                    is_active_websocket = (
                        self._connection_ownership.is_active_websocket(websocket)
                    )
                if not is_active_websocket:
                    await self._send_error(
                        websocket,
                        request=envelope,
                        error_code=BridgeErrorCode.NOT_CONNECTED,
                        message=(
                            "Fabric ChatClef bridge messages require an active "
                            "handshake on this websocket."
                        ),
                    )
                    self._diagnostics.warning(
                        "ignored message from inactive websocket "
                        f"type={envelope.message_type.value} "
                        f"session={envelope.session_id}"
                    )
                    continue

                if envelope.message_type == BridgeMessageType.STATUS_REQUEST:
                    await self._send_status_snapshot(websocket, envelope)
                    continue

                if envelope.message_type == BridgeMessageType.COMMAND_RESULT:
                    self._handle_command_result(websocket, envelope)
                    continue

                await self._send_error(
                    websocket,
                    request=envelope,
                    error_code=BridgeErrorCode.NOT_IMPLEMENTED,
                    message="Fabric ChatClef bridge message type is not handled.",
                )
        except Exception as error:
            if not self._stopping:
                self._last_error = f"{type(error).__name__}: {error}"
                self._diagnostics.warning(
                    f"client handler ended with {self._last_error}"
                )
        finally:
            if accepted_session_id is not None:
                with self._command_lock:
                    cleared = self._connection_ownership.clear_if_active(
                        websocket=websocket,
                        session_id=accepted_session_id,
                    )
                if cleared:
                    self._session_registry.remove(accepted_session_id)
                    self._diagnostics.info(
                        "client disconnected "
                        f"session={accepted_session_id} "
                        f"generation={connection_generation}"
                    )

    def _parse_envelope(self, raw_message: Any) -> BridgeEnvelopeDTO | None:
        try:
            payload = json.loads(raw_message)
            return BridgeEnvelopeDTO.from_mapping(payload)
        except Exception:
            return None

    async def _send_handshake_ack(
        self,
        websocket: Any,
        request: BridgeEnvelopeDTO,
        session_id: str,
        *,
        accepted: bool = True,
        message: str = "",
        connection_generation: int = 0,
    ) -> None:
        await self._send_envelope(
            websocket,
            BridgeEnvelopeDTO(
                protocol_version=1,
                message_type=BridgeMessageType.HANDSHAKE_ACK,
                message_id=self._new_message_id(),
                correlation_id=request.message_id,
                session_id=session_id,
                timestamp_ms=self._now_ms(),
                payload={
                    "accepted": accepted,
                    "session_id": session_id,
                    "connection_generation": connection_generation,
                    "message": message,
                    "status": self.status_snapshot(enabled=True).to_dict(),
                },
            ),
        )

    async def _send_status_snapshot(
        self,
        websocket: Any,
        request: BridgeEnvelopeDTO,
    ) -> None:
        await self._send_envelope(
            websocket,
            BridgeEnvelopeDTO(
                protocol_version=1,
                message_type=BridgeMessageType.STATUS_SNAPSHOT,
                message_id=self._new_message_id(),
                correlation_id=request.message_id,
                session_id=request.session_id,
                timestamp_ms=self._now_ms(),
                payload=self.status_snapshot(enabled=True).to_dict(),
            ),
        )

    async def _send_error(
        self,
        websocket: Any,
        *,
        error_code: BridgeErrorCode,
        message: str,
        request: BridgeEnvelopeDTO | None = None,
    ) -> None:
        await self._send_envelope(
            websocket,
            BridgeEnvelopeDTO(
                protocol_version=1,
                message_type=BridgeMessageType.ERROR,
                message_id=self._new_message_id(),
                correlation_id=None if request is None else request.message_id,
                session_id=None if request is None else request.session_id,
                timestamp_ms=self._now_ms(),
                payload={
                    "error_code": error_code.value,
                    "message": message,
                },
            ),
        )

    async def _send_envelope(
        self,
        websocket: Any,
        envelope: BridgeEnvelopeDTO,
    ) -> None:
        await websocket.send(
            json.dumps(
                envelope.to_dict(),
                ensure_ascii=False,
                separators=(",", ":"),
            )
        )

    def _session_id_for(self, envelope: BridgeEnvelopeDTO) -> str:
        candidate = envelope.session_id or envelope.payload.get("session_id")
        session_id = str(candidate or "").strip()
        return session_id or f"fabric-chatclef-{uuid.uuid4().hex}"

    def _payload_dict(self, value: Any) -> dict[str, Any]:
        return dict(value) if isinstance(value, dict) else {}

    def _handle_command_result(self, websocket: Any, envelope: BridgeEnvelopeDTO) -> None:
        try:
            result = CommandResultDTO.from_mapping(envelope.payload)
        except Exception as error:
            self._diagnostics.warning(
                "ignored malformed command result "
                f"error={type(error).__name__}: {error}"
            )
            return
        with self._command_lock:
            accepted, reason = self._connection_ownership.accept_result(
                websocket=websocket,
                envelope=envelope,
                result=result,
            )
        if not accepted:
            self._diagnostics.warning(
                "ignored command result "
                f"request={result.request_id} "
                f"status={result.status.value} "
                f"reason={reason} "
                f"session={envelope.session_id} "
                f"correlation={envelope.correlation_id}"
            )
            return
        self._diagnostics.info(
            "command result "
            f"request={result.request_id} status={result.status.value} ok={result.ok}"
        )

    def _command_status(self) -> dict[str, Any]:
        with self._command_lock:
            return self._connection_ownership.snapshot()

    def _command_rejection(
        self,
        request: CommandRequestDTO,
        error_code: BridgeErrorCode,
        message: str,
    ) -> CommandResultDTO:
        return CommandResultDTO(
            request_id=request.request_id,
            ok=False,
            status=CommandResultStatus.REJECTED,
            error_code=error_code,
            message=message,
            data={},
        )

    def _new_message_id(self) -> str:
        return f"lavi-{uuid.uuid4().hex}"

    def _now_ms(self) -> int:
        return int(time.time() * 1000)
