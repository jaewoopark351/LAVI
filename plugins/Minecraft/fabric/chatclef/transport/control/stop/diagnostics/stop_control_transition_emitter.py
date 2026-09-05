#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from typing import Mapping

from .stop_control_transition_schema import (
    STOP_CONTROL_TRANSITION_CANONICAL_FIELDS,
)


class StopControlTransitionEmitter:
    def __init__(self, diagnostics: object, atom_encoder: object):
        self._diagnostics = diagnostics
        self._atom_encoder = atom_encoder

    def emit(self, fields: Mapping[str, object]) -> None:
        try:
            self._diagnostics.info(
                "event=stop_control_transition "
                + " ".join(
                    f"{name}={self._atom_encoder.encode(fields[name])}"
                    for name in STOP_CONTROL_TRANSITION_CANONICAL_FIELDS
                )
            )
        except Exception:
            pass


__all__ = ("StopControlTransitionEmitter",)
