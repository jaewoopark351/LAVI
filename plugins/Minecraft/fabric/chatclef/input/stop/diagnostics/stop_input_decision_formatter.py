#20260905_kpopmodder: Format the exact canonical bounded STOP input-decision fields.
from __future__ import annotations

from .stop_input_decision_record import StopInputDecisionRecord


class StopInputDecisionFormatter:
    EVENT_NAME = "stop_input_decision"
    CANONICAL_FIELDS = (
        "event_id",
        "source",
        "provider_id",
        "event_kind",
        "final",
        "phrase_rule_id",
        "decision",
        "reason",
    )

    def format(self, record: object) -> str:
        if type(record) is not StopInputDecisionRecord:
            raise TypeError("STOP input-decision record must be exact")
        return f"event={self.EVENT_NAME} " + " ".join(
            f"{name}={_bounded_atom(getattr(record, name))}"
            for name in self.CANONICAL_FIELDS
        )


def _bounded_atom(value: object) -> str:
    if value is None or value == "":
        return "none"
    if type(value) is bool:
        return "true" if value else "false"
    if type(value) is int:
        return str(value) if -(2**63) <= value < 2**63 else "invalid"
    if type(value) is not str or len(value) > 160:
        return "invalid"
    if any(character.isspace() or character == "=" for character in value):
        return "invalid"
    return value


__all__ = ("StopInputDecisionFormatter",)
