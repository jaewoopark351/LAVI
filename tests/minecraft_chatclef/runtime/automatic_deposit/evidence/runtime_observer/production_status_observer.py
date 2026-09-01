#20260901_kpopmodder: Observe the actual Fabric status schema without inventing runtime artifact fields.
from __future__ import annotations

from collections.abc import Mapping

from ....preflight.runtime_bridge_snapshot import runtime_bridge_snapshot
from .production_backend_identity import PRODUCTION_FABRIC_BACKEND_ID
from .production_status_observation import (
    ProductionFabricStatusObservation,
    _create_production_status_observation,
)


_LOADER_ID = "fabric"
_SESSION_PHASE = "phase_4_tick_dispatch"


def observe_production_fabric_status(
    payload: object,
) -> tuple[ProductionFabricStatusObservation | None, str]:
    bridge, bridge_error = runtime_bridge_snapshot(payload)
    if bridge_error:
        return None, f"PRODUCTION_STATUS_BRIDGE_INVALID:{bridge_error}"
    if _text(bridge.get("backend_id")) != PRODUCTION_FABRIC_BACKEND_ID:
        return None, "PRODUCTION_STATUS_BACKEND_MISMATCH"
    if bridge.get("enabled") is not True:
        return None, "PRODUCTION_STATUS_BACKEND_DISABLED"
    if bridge.get("connected") is not True:
        return None, "PRODUCTION_STATUS_BACKEND_DISCONNECTED"
    lifecycle_state = _text(bridge.get("lifecycle_state"))
    if lifecycle_state != "connected":
        return None, "PRODUCTION_STATUS_LIFECYCLE_NOT_CONNECTED"

    details = bridge.get("details")
    if not isinstance(details, Mapping):
        return None, "PRODUCTION_STATUS_DETAILS_INVALID"
    sessions = details.get("sessions")
    commands = details.get("commands")
    if not isinstance(sessions, Mapping):
        return None, "PRODUCTION_STATUS_SESSIONS_INVALID"
    if not isinstance(commands, Mapping):
        return None, "PRODUCTION_STATUS_COMMANDS_INVALID"

    session_count = sessions.get("session_count")
    if type(session_count) is not int or session_count != 1:
        return None, "PRODUCTION_STATUS_ACTIVE_SESSION_NOT_EXCLUSIVE"
    session_id = _bounded_text(sessions.get("active_session_id"), 256)
    if not session_id:
        return None, "PRODUCTION_STATUS_ACTIVE_SESSION_ID_INVALID"
    active_session = sessions.get("active_session")
    if not isinstance(active_session, Mapping):
        return None, "PRODUCTION_STATUS_ACTIVE_SESSION_INVALID"
    if _bounded_text(active_session.get("session_id"), 256) != session_id:
        return None, "PRODUCTION_STATUS_ACTIVE_SESSION_ID_MISMATCH"
    protocol_version = active_session.get("protocol_version")
    if type(protocol_version) is not int or not 1 <= protocol_version <= 2_147_483_647:
        return None, "PRODUCTION_STATUS_PROTOCOL_VERSION_INVALID"

    metadata = active_session.get("metadata")
    if not isinstance(metadata, Mapping):
        return None, "PRODUCTION_STATUS_SESSION_METADATA_INVALID"
    session_backend = _text(metadata.get("backend"))
    session_loader = _text(metadata.get("loader"))
    session_phase = _text(metadata.get("phase"))
    if session_backend != PRODUCTION_FABRIC_BACKEND_ID:
        return None, "PRODUCTION_STATUS_SESSION_BACKEND_MISMATCH"
    if session_loader != _LOADER_ID:
        return None, "PRODUCTION_STATUS_SESSION_LOADER_MISMATCH"
    if session_phase != _SESSION_PHASE:
        return None, "PRODUCTION_STATUS_SESSION_PHASE_MISMATCH"

    if "active_request_id" not in commands:
        return None, "PRODUCTION_STATUS_ACTIVE_REQUEST_MISSING"
    if commands.get("active_request_id") is not None:
        return None, "PRODUCTION_STATUS_COMMAND_NOT_IDLE"
    if _bounded_text(commands.get("active_session_id"), 256) != session_id:
        return None, "PRODUCTION_STATUS_SESSION_OWNERSHIP_MISMATCH"
    generation = commands.get("active_generation")
    if type(generation) is not int or not 1 <= generation <= 9_223_372_036_854_775_807:
        return None, "PRODUCTION_STATUS_CONNECTION_GENERATION_INVALID"

    return (
        _create_production_status_observation(
            backend_id=PRODUCTION_FABRIC_BACKEND_ID,
            lifecycle_state=lifecycle_state,
            session_id=session_id,
            session_count=session_count,
            connection_generation=generation,
            active_request_id=None,
            protocol_version=protocol_version,
            session_backend=session_backend,
            session_loader=session_loader,
            session_phase=session_phase,
        ),
        "PRODUCTION_FABRIC_STATUS_OBSERVED",
    )


def _bounded_text(value: object, maximum: int) -> str:
    text = _text(value)
    if not text or len(text) > maximum:
        return ""
    if any(ord(character) < 0x20 or ord(character) == 0x7F for character in text):
        return ""
    return text


def _text(value: object) -> str:
    return str(value or "").strip()
