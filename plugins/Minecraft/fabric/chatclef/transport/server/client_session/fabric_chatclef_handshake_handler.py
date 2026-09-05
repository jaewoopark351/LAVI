#20260905_kpopmodder: Preserve handshake handling as a sequencing facade.
from __future__ import annotations

from typing import Any, Callable

from .handshake import FabricChatClefHandshakeComponentGraph


class FabricChatClefHandshakeHandler:
    def __init__(
        self,
        *,
        connection_ownership,
        command_lock,
        session_registry,
        diagnostics,
        envelope_transport,
        status_provider: Callable[[], dict[str, Any]],
        now_ms: Callable[[], int],
        component_graph=None,
    ) -> None:
        self._connection_ownership = connection_ownership
        self._command_lock = command_lock
        self._session_registry = session_registry
        self._diagnostics = diagnostics
        self._envelope_transport = envelope_transport
        self._status_provider = status_provider
        self._now_ms = now_ms
        self._component_graph = (
            component_graph
            or FabricChatClefHandshakeComponentGraph(
                connection_ownership=connection_ownership,
                command_lock=command_lock,
                session_registry=session_registry,
                diagnostics=diagnostics,
                envelope_transport=envelope_transport,
                status_provider=status_provider,
                now_ms=now_ms,
            )
        )
        self._session_id_factory = self._component_graph.session_id_factory
        self._admission = self._component_graph.admission
        self._persistence = self._component_graph.persistence
        self._acknowledger = self._component_graph.acknowledger
        self._handshake_diagnostics = self._component_graph.diagnostics

    async def handle(self, websocket: Any, envelope) -> tuple[bool, str, int]:
        session_id = self._session_id_for(envelope)
        admission = self._admission.inspect(
            websocket=websocket,
            session_id=session_id,
        )
        if not admission.accepted:
            await self._acknowledger.send(
                websocket,
                envelope,
                session_id,
                accepted=False,
                message=admission.reason,
                connection_generation=admission.generation,
            )
            self._handshake_diagnostics.rejected(
                session_id=session_id,
                reason=admission.reason,
            )
            return False, session_id, admission.generation

        connection_generation = admission.generation
        self._persistence.persist(
            session_id=session_id,
            envelope=envelope,
        )
        await self._acknowledger.send(
            websocket,
            envelope,
            session_id,
            accepted=True,
            connection_generation=connection_generation,
        )
        self._handshake_diagnostics.accepted(
            session_id=session_id,
            generation=connection_generation,
        )
        return True, session_id, connection_generation

    def _session_id_for(self, envelope) -> str:
        return self._session_id_factory.create(envelope)

    def _payload_dict(self, value: Any) -> dict[str, Any]:
        return self._persistence.payload_dict(value)


__all__ = ("FabricChatClefHandshakeHandler",)
