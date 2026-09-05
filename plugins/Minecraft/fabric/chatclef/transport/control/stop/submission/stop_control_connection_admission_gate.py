#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations


class StopControlConnectionAdmissionGate:
    def __init__(
        self,
        *,
        connection_ownership: object,
        session_registry: object,
        tracker_registry: object,
        admission_barrier: object,
        loop_provider: object,
    ):
        self._connection_ownership = connection_ownership
        self._session_registry = session_registry
        self._tracker_registry = tracker_registry
        self._admission_barrier = admission_barrier
        self._loop_provider = loop_provider

    def inspect(self) -> tuple[str, object, object, object, object]:
        if (
            self._tracker_registry.current() is not None
            or self._admission_barrier.closed
        ):
            return "stop_control_in_flight", None, None, None, None
        loop = self._loop_provider()
        if (
            not self._connection_ownership.is_connected()
            or loop is None
            or not loop.is_running()
        ):
            return "bridge_disconnected", None, None, None, None
        session = self._session_registry.active_session()
        session_id = self._connection_ownership.active_session_id
        generation = self._connection_ownership.active_generation
        websocket = self._connection_ownership.active_websocket
        if (
            session is None
            or session.session_id != session_id
            or websocket is None
            or self._connection_ownership.is_active_websocket(websocket) is not True
            or session.capabilities.get("chatclef_stop_control_v1") is not True
        ):
            return (
                "stop_control_capability_unavailable",
                None,
                None,
                None,
                None,
            )
        return "", loop, session_id, generation, websocket


__all__ = ("StopControlConnectionAdmissionGate",)
