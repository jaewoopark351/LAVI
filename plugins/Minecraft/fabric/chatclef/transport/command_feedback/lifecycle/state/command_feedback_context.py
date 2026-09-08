#20260907_kpopmodder: Bind immutable feedback context to one ordinary command owner.
from __future__ import annotations

from dataclasses import dataclass, field

from ..admission.command_feedback_admission_grant import (
    CommandFeedbackAdmissionGrant,
)
from ..descriptor.command_feedback_descriptor import CommandFeedbackDescriptor


@dataclass(frozen=True, slots=True)
class CommandFeedbackContext:
    websocket: object = field(compare=False, repr=False)
    owner_token: object = field(compare=False, repr=False)
    admission_grant: CommandFeedbackAdmissionGrant = field(compare=False, repr=False)
    descriptor: CommandFeedbackDescriptor
    session_id: str
    generation: int
    request_id: str
    command_message_id: str
    accepted_at_ms: int
    before_target_count: int | None = None

    def __getattr__(self, name: str):
        descriptor_names = {
            "command_name",
            "command",
            "command_source",
            "event_id",
            "input_source",
            "provider_id",
            "event_kind",
            "target_item",
            "requested_count",
            "intent_kind",
            "acquisition_verb_class",
            "spoken_item_label",
            "requested_family",
        }
        if name in descriptor_names:
            return getattr(self.descriptor, name)
        raise AttributeError(name)


__all__ = ("CommandFeedbackContext",)
