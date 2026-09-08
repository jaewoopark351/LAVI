#20260907_kpopmodder: Resolve one registered command's immutable quantity meaning.
from .command_feedback_descriptor import CommandFeedbackDescriptor


def command_feedback_quantity_semantics(command_name: str) -> str:
    if command_name == "get":
        return CommandFeedbackDescriptor.ACQUIRE_DELTA
    if command_name in {"deposit", "give"}:
        return CommandFeedbackDescriptor.REQUESTED_COUNT
    if command_name in {"food", "meat"}:
        return CommandFeedbackDescriptor.PROFILE_SPECIFIC_UNITS
    return CommandFeedbackDescriptor.UNKNOWN_QUANTITY


__all__ = ("command_feedback_quantity_semantics",)
