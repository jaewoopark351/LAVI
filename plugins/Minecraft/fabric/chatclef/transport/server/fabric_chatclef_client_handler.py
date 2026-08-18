#20260818_kpopmodder: Own one Fabric ChatClef WebSocket client protocol session.
from __future__ import annotations

import json
import uuid
from typing import Any, Callable

from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType


class FabricChatClefClientHandler:
    def __init__(
        self,
        *,
        connection_ownership,
        command_lock,
        session_registry,
        diagnostics,
        envelope_transport,
        command_result_handler,
        status_provider: Callable[[], dict[str, Any]],
        stopping_provider: Callable[[], bool],
        last_error_reporter: Callable[[str], None],
        now_ms: Callable[[], int],
    ):
        self._connection_ownership = connection_ownership
        self._command_lock = command_lock
        self._session_registry = session_registry
        self._diagnostics = diagnostics
        self._envelope_transport = envelope_transport
        self._command_result_handler = command_result_handler
        self._status_provider = status_provider
        self._stopping_provider = stopping_provider
        self._last_error_reporter = last_error_reporter
        self._now_ms = now_ms

    async def handle_legacy_client(self, websocket: Any, _path: str = "") -> None:
        await self.handle_client(websocket)

    async def handle_client(self, websocket: Any) -> None:
        accepted_session_id: str | None = None
        connection_generation = 0
        try:
            async for raw_message in websocket:
                envelope = self._envelope_transport.parse(raw_message)
                if envelope is None:
                    await self._envelope_transport.send_error(
                        websocket,
                        error_code=BridgeErrorCode.INVALID_REQUEST,
                        message="Invalid Fabric ChatClef bridge JSON envelope.",
                    )
                    continue

                if envelope.protocol_version != 1:
                    await self._envelope_transport.send_error(
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
                        await self._envelope_transport.send_handshake_ack(
                            websocket,
                            envelope,
                            session_id,
                            self._status_provider(),
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
                        metadata=self._payload_dict(
                            envelope.payload.get("metadata")
                        ),
                    )
                    await self._envelope_transport.send_handshake_ack(
                        websocket,
                        envelope,
                        session_id,
                        self._status_provider(),
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
                    await self._envelope_transport.send_error(
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
                    await self._envelope_transport.send_status_snapshot(
                        websocket,
                        envelope,
                        self._status_provider(),
                    )
                    continue

                if envelope.message_type == BridgeMessageType.COMMAND_RESULT:
                    self._command_result_handler.handle(websocket, envelope)
                    continue

                await self._envelope_transport.send_error(
                    websocket,
                    request=envelope,
                    error_code=BridgeErrorCode.NOT_IMPLEMENTED,
                    message="Fabric ChatClef bridge message type is not handled.",
                )
        except Exception as error:
            if not self._stopping_provider():
                message = f"{type(error).__name__}: {error}"
                self._last_error_reporter(message)
                self._diagnostics.warning(
                    f"client handler ended with {message}"
                )
        finally:
            if accepted_session_id is not None:
                with self._command_lock:
                    before_status = self._connection_ownership.snapshot()
                    cleared = self._connection_ownership.clear_if_active(
                        websocket=websocket,
                        session_id=accepted_session_id,
                    )
                    after_status = self._connection_ownership.snapshot()
                if cleared:
                    self._session_registry.remove(accepted_session_id)
                    self._diagnostics.info(
                        "client disconnected "
                        f"session={accepted_session_id} "
                        f"generation={connection_generation} "
                        f"before={_compact_json(before_status)} "
                        f"after={_compact_json(after_status)}"
                    )

    def _session_id_for(self, envelope) -> str:
        candidate = envelope.session_id or envelope.payload.get("session_id")
        session_id = str(candidate or "").strip()
        return session_id or f"fabric-chatclef-{uuid.uuid4().hex}"

    def _payload_dict(self, value: Any) -> dict[str, Any]:
        return dict(value) if isinstance(value, dict) else {}


def _compact_json(payload: Any) -> str:
    try:
        return json.dumps(
            payload,
            ensure_ascii=False,
            sort_keys=True,
            separators=(",", ":"),
        )
    except Exception as error:
        return f"<json failed {type(error).__name__}: {error}>"
