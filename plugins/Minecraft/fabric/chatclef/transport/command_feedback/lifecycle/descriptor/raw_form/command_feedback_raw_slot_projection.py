#20260907_kpopmodder: Carry only bounded slots recovered by the already-validated raw form.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class CommandFeedbackRawSlotProjection:
    slots_recoverable: bool = True
    target_entries: tuple[tuple[str, int], ...] = ()
    requested_count: int | None = None
    player_name: str = ""
    coordinate_values: tuple[int, ...] = ()
    dimension: str = ""
    setting_value: str = ""
    structure_name: str = ""
    destination_id: str = ""
    operation_target: str = ""


__all__ = ("CommandFeedbackRawSlotProjection",)
