#20260905_kpopmodder: Project a trusted STOP classification without retaining input text.
from __future__ import annotations

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent

from ..stop_input_decision import StopInputDecision
from ..stop_input_decision_kind import StopInputDecisionKind
from .stop_input_decision_record import StopInputDecisionRecord


class StopInputDecisionProjector:
    def project(
        self,
        *,
        event: object,
        decision: object,
    ) -> StopInputDecisionRecord:
        if type(event) is not LaviInputEvent:
            raise TypeError("STOP diagnostic event must be exact")
        if type(decision) is not StopInputDecision:
            raise TypeError("STOP diagnostic decision must be exact")
        if type(decision.kind) is not StopInputDecisionKind:
            raise TypeError("STOP diagnostic decision kind must be exact")
        return StopInputDecisionRecord(
            event_id=event.event_id,
            source=event.source,
            provider_id=event.provider_id,
            event_kind=event.event_kind,
            final=event.final,
            phrase_rule_id=decision.phrase_rule_id,
            decision=decision.kind.value,
            reason=decision.reason,
        )


__all__ = ("StopInputDecisionProjector",)
