#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from typing import Mapping

from .stop_control_transition_schema import (
    STOP_CONTROL_TRANSITION_CANONICAL_FIELDS,
    STOP_CONTROL_TRANSITION_WIRE_DATA_FIELDS,
)


class StopControlTransitionProjector:
    def project(
        self,
        *,
        tracker: object,
        wire_data: Mapping[str, object] | None = None,
        control_status: object = None,
        control_outcome: object = None,
        control_reason: object = None,
        diagnostic_disposition: object = None,
        control_result_delivery: object = None,
        python_stop_barrier_state: object,
        ordinary_owner_gate_state: object,
        quarantine_active: object,
        retirement_evidence_kind: object = None,
        frozen_python_owner_exact_match: object = None,
    ) -> dict[str, object]:
        fields = {
            name: None for name in STOP_CONTROL_TRANSITION_CANONICAL_FIELDS
        }
        identity = getattr(tracker, "identity", None)
        fields.update(
            {
                "control_request_id": getattr(identity, "request_id", None),
                "control_message_id": getattr(identity, "message_id", None),
                "session_id": getattr(identity, "session_id", None),
                "server_connection_generation": getattr(
                    identity,
                    "server_connection_generation",
                    None,
                ),
                "target_scope": getattr(tracker, "target_scope", None),
                "control_status": control_status,
                "control_outcome": control_outcome,
                "control_reason": control_reason,
                "diagnostic_disposition": diagnostic_disposition,
                "control_result_delivery": control_result_delivery,
                "python_stop_barrier_state": python_stop_barrier_state,
                "java_stop_barrier_state": "unknown",
                "ordinary_owner_gate_state": ordinary_owner_gate_state,
                "frozen_python_owner_exact_match": (
                    frozen_python_owner_exact_match
                ),
                "quarantine_active": quarantine_active,
                "retirement_evidence_kind": retirement_evidence_kind,
            }
        )
        target = getattr(tracker, "target", None)
        if target is not None:
            fields.update(
                {
                    "requested_target_request_id": getattr(
                        target,
                        "request_id",
                        None,
                    ),
                    "requested_target_command_message_id": getattr(
                        target,
                        "command_message_id",
                        None,
                    ),
                    "requested_target_session_id": getattr(
                        target,
                        "session_id",
                        None,
                    ),
                    "requested_target_server_connection_generation": getattr(
                        target,
                        "server_connection_generation",
                        None,
                    ),
                }
            )
        if type(wire_data) is dict:
            for name in STOP_CONTROL_TRANSITION_WIRE_DATA_FIELDS:
                fields[name] = wire_data.get(name)
        return fields


__all__ = ("StopControlTransitionProjector",)
