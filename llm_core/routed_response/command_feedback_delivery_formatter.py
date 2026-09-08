#20260905_kpopmodder: Format the exact canonical bounded feedback-delivery fields.
from __future__ import annotations

from .command_feedback_delivery_record import CommandFeedbackDeliveryRecord
from .diagnostics.command_feedback_delivery_schema import (
    CommandFeedbackDeliverySchema,
)


class CommandFeedbackDeliveryFormatter:
    EVENT_NAME = "command_feedback_delivery"
    CANONICAL_FIELDS = CommandFeedbackDeliverySchema.CANONICAL_FIELDS

    def __init__(self, schema=None) -> None:
        self._schema = schema or CommandFeedbackDeliverySchema()

    def format(self, record: object) -> str:
        if type(record) is not CommandFeedbackDeliveryRecord:
            raise TypeError("feedback delivery record must be exact")
        fields = self._schema.normalize(record)
        return f"event={self.EVENT_NAME} " + " ".join(
            f"{name}={_bounded_atom(fields[name])}" for name in self.CANONICAL_FIELDS
        )


def _bounded_atom(value: object) -> str:
    if value is None or value == "":
        return "none"
    if type(value) is bool:
        return "true" if value else "false"
    if type(value) is int:
        return str(value) if 0 <= value < 2**63 else "invalid"
    if type(value) is not str or len(value) > 160:
        return "invalid"
    if any(character.isspace() or character == "=" for character in value):
        return "invalid"
    return value


__all__ = ("CommandFeedbackDeliveryFormatter",)
