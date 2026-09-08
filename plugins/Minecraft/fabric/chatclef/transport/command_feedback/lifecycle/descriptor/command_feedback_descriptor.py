#20260907_kpopmodder: Carry one immutable, admission-proven command feedback description.
from __future__ import annotations

import re
from dataclasses import dataclass

from .command_feedback_target import CommandFeedbackTarget


@dataclass(frozen=True, slots=True)
class CommandFeedbackDescriptor:
    ACQUIRE_DELTA = "acquire_delta"
    ENSURE_TOTAL = "ensure_total"
    REQUESTED_COUNT = "requested_count"
    PROFILE_SPECIFIC_UNITS = "profile_specific_units"
    UNKNOWN_QUANTITY = "unknown"

    TYPED = "typed"
    RAW_TYPED = "raw_typed"
    COMMAND_NAME_ONLY = "command_name_only"

    command_name: str
    command: str
    command_source: str
    lifecycle_kind: str
    phrase_profile_id: str
    evidence_profile_id: str
    rollout_state: str
    event_id: str
    input_source: str
    provider_id: str
    event_kind: str
    requested_family: str
    intent_kind: str = "unknown"
    target_item: str | None = None
    requested_count: int | None = None
    quantity_semantics: str = UNKNOWN_QUANTITY
    acquisition_verb_class: str = ""
    spoken_target_label: str = ""
    player_name: str = ""
    coordinates: tuple[int, int, int] | None = None
    detail_level: str = TYPED
    form_kind: str = "typed"
    response_lifecycle_kind: str = "finite_task"
    targets: tuple[CommandFeedbackTarget, ...] = ()
    coordinate_values: tuple[int, ...] = ()
    dimension: str = ""
    setting_value: str = ""
    structure_name: str = ""
    destination_id: str = ""
    operation_target: str = ""

    _EVENT_ID = re.compile(r"[0-9a-f]{32}\Z", re.ASCII)

    def __post_init__(self) -> None:
        required = (
            self.command_name,
            self.command,
            self.command_source,
            self.lifecycle_kind,
            self.phrase_profile_id,
            self.evidence_profile_id,
            self.rollout_state,
            self.input_source,
            self.provider_id,
            self.event_kind,
            self.requested_family,
        )
        if not all(type(value) is str and bool(value.strip()) for value in required):
            raise ValueError("command feedback descriptor requires bounded text fields")
        if (
            type(self.event_id) is not str
            or self._EVENT_ID.fullmatch(self.event_id) is None
        ):
            raise ValueError("command feedback event_id is invalid")
        if self.requested_count is not None and (
            type(self.requested_count) is not int or self.requested_count < 1
        ):
            raise ValueError("command feedback quantity must be a positive exact int")
        if self.coordinates is not None and (
            type(self.coordinates) is not tuple
            or len(self.coordinates) != 3
            or any(type(value) is not int for value in self.coordinates)
        ):
            raise ValueError("command feedback coordinates must be three exact ints")
        if self.detail_level not in {
            self.TYPED,
            self.RAW_TYPED,
            self.COMMAND_NAME_ONLY,
        }:
            raise ValueError("command feedback detail level is invalid")
        if (
            type(self.form_kind) is not str
            or not self.form_kind
            or self.response_lifecycle_kind
            not in {
                "finite_task",
                "persistent_task",
                "asynchronous_immediate",
                "specialized_control",
            }
        ):
            raise ValueError("command feedback lifecycle form is invalid")
        if (
            type(self.targets) is not tuple
            or any(type(target) is not CommandFeedbackTarget for target in self.targets)
            or type(self.coordinate_values) is not tuple
            or len(self.coordinate_values) > 3
            or any(type(value) is not int for value in self.coordinate_values)
            or any(
                type(value) is not str or len(value) > 128
                for value in (
                    self.dimension,
                    self.setting_value,
                    self.structure_name,
                    self.destination_id,
                    self.operation_target,
                )
            )
        ):
            raise ValueError("command feedback typed slot projection is invalid")
        if self.quantity_semantics not in {
            self.ACQUIRE_DELTA,
            self.ENSURE_TOTAL,
            self.REQUESTED_COUNT,
            self.PROFILE_SPECIFIC_UNITS,
            self.UNKNOWN_QUANTITY,
        }:
            raise ValueError("command feedback quantity semantics are invalid")

    @property
    def spoken_item_label(self) -> str:
        return self.spoken_target_label


__all__ = ("CommandFeedbackDescriptor",)
