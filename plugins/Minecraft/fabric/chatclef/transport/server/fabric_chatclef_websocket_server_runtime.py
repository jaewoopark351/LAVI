#20260818_kpopmodder: Compose the focused Fabric ChatClef WebSocket server runtime.
from __future__ import annotations

import asyncio
import threading
import time
import uuid
from typing import Any

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.dto.status_snapshot_dto import StatusSnapshotDTO
from plugins.Minecraft.fabric.chatclef.config.fabric_chatclef_config import (
    FabricChatClefConfig,
)
from plugins.Minecraft.fabric.chatclef.diagnostics import FabricChatClefDiagnostics
from plugins.Minecraft.fabric.chatclef.session import FabricChatClefSessionRegistry

from ..command_submission import (
    FabricChatClefCommandResultHandler,
    FabricChatClefCommandSubmitter,
)
from ..fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)
from ..fabric_chatclef_websocket_server_factory import (
    serve_fabric_chatclef_websocket,
)
from .fabric_chatclef_client_handler import FabricChatClefClientHandler
from .fabric_chatclef_envelope_transport import FabricChatClefEnvelopeTransport
from .fabric_chatclef_status_snapshot_builder import (
    FabricChatClefStatusSnapshotBuilder,
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
        self._connection_ownership = FabricChatClefConnectionOwnership(
            reconcile_stale_deposit_to_unknown_requested=(
                config.reconcile_stale_deposit_to_unknown_enabled
            ),
        )
        envelope_transport = FabricChatClefEnvelopeTransport(
            message_id_factory=self._new_message_id,
            now_ms=self._now_ms,
        )
        result_handler = FabricChatClefCommandResultHandler(
            connection_ownership=self._connection_ownership,
            command_lock=self._command_lock,
            diagnostics=diagnostics,
        )
        self._status_builder = FabricChatClefStatusSnapshotBuilder(
            connection_ownership=self._connection_ownership,
            command_lock=self._command_lock,
            session_registry=session_registry,
        )
        self._client_handler = FabricChatClefClientHandler(
            connection_ownership=self._connection_ownership,
            command_lock=self._command_lock,
            session_registry=session_registry,
            diagnostics=diagnostics,
            envelope_transport=envelope_transport,
            command_result_handler=result_handler,
            status_provider=lambda: self.wire_status_snapshot(
                enabled=True
            ).to_dict(),
            stopping_provider=lambda: self._stopping,
            last_error_reporter=self._record_client_error,
            now_ms=self._now_ms,
        )
        self._command_submitter = FabricChatClefCommandSubmitter(
            connection_ownership=self._connection_ownership,
            command_lock=self._command_lock,
            diagnostics=diagnostics,
            loop_provider=lambda: self._loop,
            envelope_transport=envelope_transport,
            send_timeout_sec=config.startup_timeout_sec,
            message_id_factory=self._new_message_id,
            now_ms=self._now_ms,
        )

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
        return self.local_status_snapshot(enabled=enabled)

    def local_status_snapshot(self, *, enabled: bool) -> StatusSnapshotDTO:
        return self._status_builder.build(
            enabled=enabled,
            last_error=self._last_error,
            is_running=self.is_running,
            endpoint=self.endpoint,
            bound_host=self._bound_host,
            bound_port=self._bound_port,
            commands_view="local",
        )

    def wire_status_snapshot(self, *, enabled: bool) -> StatusSnapshotDTO:
        return self._status_builder.build(
            enabled=enabled,
            last_error=self._last_error,
            is_running=self.is_running,
            endpoint=self.endpoint,
            bound_host=self._bound_host,
            bound_port=self._bound_port,
            commands_view="wire",
        )

    def submit_command(self, request: CommandRequestDTO) -> CommandResultDTO:
        return self._command_submitter.submit(request)

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
                "Fabric ChatClef WebSocket server failed: "
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
            self._client_handler.handle_legacy_client,
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

    def _record_client_error(self, message: str) -> None:
        self._last_error = message

    def _new_message_id(self) -> str:
        return f"lavi-{uuid.uuid4().hex}"

    def _now_ms(self) -> int:
        return int(time.time() * 1000)
