#20260905_kpopmodder: Retain only canonical bounded STOP decision diagnostics.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class StopInputDecisionRecord:
    event_id: object
    source: object
    provider_id: object
    event_kind: object
    final: object
    phrase_rule_id: object
    decision: object
    reason: object


__all__ = ("StopInputDecisionRecord",)
