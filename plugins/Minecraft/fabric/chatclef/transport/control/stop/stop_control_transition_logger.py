#20260905_kpopmodder: Preserve the legacy STOP transition logger as a thin facade.
from __future__ import annotations

from typing import Mapping

from .diagnostics import (
    STOP_CONTROL_TRANSITION_CANONICAL_FIELDS,
    STOP_CONTROL_TRANSITION_WIRE_DATA_FIELDS,
    StopControlTransitionAtomEncoder,
    StopControlTransitionEmitter,
    StopControlTransitionProjector,
)


class StopControlTransitionLogger:
    _WIRE_DATA_FIELDS = STOP_CONTROL_TRANSITION_WIRE_DATA_FIELDS
    CANONICAL_FIELDS = STOP_CONTROL_TRANSITION_CANONICAL_FIELDS

    def __init__(self, diagnostics: object):
        self._projector = StopControlTransitionProjector()
        self._emitter = StopControlTransitionEmitter(
            diagnostics,
            StopControlTransitionAtomEncoder(),
        )

    def log(
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
    ) -> None:
        fields = self._projector.project(
            tracker=tracker,
            wire_data=wire_data,
            control_status=control_status,
            control_outcome=control_outcome,
            control_reason=control_reason,
            diagnostic_disposition=diagnostic_disposition,
            control_result_delivery=control_result_delivery,
            python_stop_barrier_state=python_stop_barrier_state,
            ordinary_owner_gate_state=ordinary_owner_gate_state,
            quarantine_active=quarantine_active,
            retirement_evidence_kind=retirement_evidence_kind,
            frozen_python_owner_exact_match=frozen_python_owner_exact_match,
        )
        self._emitter.emit(fields)


__all__ = ("StopControlTransitionLogger",)
