#20260907_kpopmodder: Project validated raw form units without guessing derived Java values.
from __future__ import annotations

from .command_feedback_raw_slot_projection import CommandFeedbackRawSlotProjection


class CommandFeedbackRawSlotProjector:
    _ITEM_COMMANDS = frozenset({"get", "deposit", "deposit_all", "equip"})

    def project(self, raw_form: object) -> CommandFeedbackRawSlotProjection:
        command_name = getattr(raw_form, "command_name", "")
        form_kind = getattr(raw_form, "form_kind", "")
        arguments = getattr(raw_form, "argument_units", ())
        if type(arguments) is not tuple:
            return CommandFeedbackRawSlotProjection()
        if "unprojected" in form_kind:
            return CommandFeedbackRawSlotProjection(slots_recoverable=False)
        if command_name in self._ITEM_COMMANDS:
            if form_kind == "equipment_material_set":
                return CommandFeedbackRawSlotProjection(
                    operation_target=arguments[0].lower()
                )
            if "item" in form_kind:
                return CommandFeedbackRawSlotProjection(
                    target_entries=self._item_entries(arguments)
                )
        if command_name == "give":
            return self._give(form_kind, arguments)
        if command_name == "follow" and form_kind == "explicit_player":
            return CommandFeedbackRawSlotProjection(player_name=arguments[0])
        if command_name == "attack":
            count = int(arguments[1]) if form_kind == "target_count" else 1
            return CommandFeedbackRawSlotProjection(
                requested_count=count,
                operation_target=arguments[0],
            )
        if command_name in {"food", "meat"}:
            return CommandFeedbackRawSlotProjection(
                requested_count=int(arguments[0])
            )
        if command_name == "goto":
            return self._goto(arguments)
        if command_name in {"chatclef", "overlay"}:
            return CommandFeedbackRawSlotProjection(
                setting_value=arguments[0].lower()
            )
        if command_name == "gamma":
            return CommandFeedbackRawSlotProjection(
                setting_value="1.0" if form_kind == "default_value" else arguments[0]
            )
        if command_name == "scan":
            return CommandFeedbackRawSlotProjection(
                operation_target=(
                    "dirt" if form_kind == "default_target" else arguments[0].lower()
                )
            )
        if command_name == "locate_structure":
            return CommandFeedbackRawSlotProjection(
                structure_name=arguments[0].lower()
            )
        if command_name == "auto_deposit_untrust" and form_kind == "explicit_destination":
            return CommandFeedbackRawSlotProjection(destination_id=arguments[0])
        return CommandFeedbackRawSlotProjection()

    @staticmethod
    def _item_entries(arguments: tuple[str, ...]) -> tuple[tuple[str, int], ...]:
        value = " ".join(arguments)
        if value.startswith("[") and value.endswith("]"):
            parts = tuple(part.strip() for part in value[1:-1].split(","))
        else:
            parts = (value,)
        projected = []
        for part in parts:
            units = tuple(unit for unit in part.split(" ") if unit)
            projected.append((units[0], int(units[1]) if len(units) == 2 else 1))
        return tuple(projected)

    @staticmethod
    def _give(
        form_kind: str,
        arguments: tuple[str, ...],
    ) -> CommandFeedbackRawSlotProjection:
        if form_kind == "butler_item_default_count":
            return CommandFeedbackRawSlotProjection(
                target_entries=((arguments[0], 1),)
            )
        if form_kind == "butler_item_count":
            return CommandFeedbackRawSlotProjection(
                target_entries=((arguments[0], int(arguments[1])),)
            )
        return CommandFeedbackRawSlotProjection(
            target_entries=((arguments[1], int(arguments[2])),),
            player_name=arguments[0],
        )

    @staticmethod
    def _goto(arguments: tuple[str, ...]) -> CommandFeedbackRawSlotProjection:
        values = list(arguments)
        if values[0].startswith("("):
            values[0] = values[0][1:]
            values[-1] = values[-1][:-1]
        dimension = ""
        if values[-1].lower() in {"overworld", "nether", "end"}:
            dimension = values.pop().lower()
        return CommandFeedbackRawSlotProjection(
            coordinate_values=tuple(int(value) for value in values),
            dimension=dimension,
        )


__all__ = ("CommandFeedbackRawSlotProjector",)
