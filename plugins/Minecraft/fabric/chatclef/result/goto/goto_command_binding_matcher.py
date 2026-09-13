#20260913_kpopmodder: Match decoded GOTO ownership against original admission facts.
from __future__ import annotations

from .goto_command_binding import GotoCommandBinding


class GotoCommandBindingMatcher:
    _RAW_XYZ = frozenset({
        "xyz", "xyz_dimension", "parenthesized_xyz", "parenthesized_xyz_dimension",
    })

    def matches_context(self, binding: object, context: object) -> bool:
        if type(binding) is not GotoCommandBinding:
            return False
        descriptor = getattr(context, "descriptor", None)
        if getattr(descriptor, "command_name", None) != "goto":
            return False
        if (
            binding.request_id != getattr(context, "request_id", None)
            or binding.command_message_id != getattr(context, "command_message_id", None)
            or binding.session_id != getattr(context, "session_id", None)
            or type(getattr(context, "generation", None)) is not int
            or binding.server_connection_generation != context.generation
        ):
            return False
        detail = getattr(descriptor, "detail_level", None)
        form = getattr(descriptor, "form_kind", None)
        if detail == "typed" and form in {"trusted_translation", "typed"}:
            coordinates = getattr(descriptor, "coordinates", None)
        elif detail == "raw_typed" and form in self._RAW_XYZ:
            coordinates = getattr(descriptor, "coordinate_values", None)
        else:
            return False
        if (
            type(coordinates) is not tuple or len(coordinates) != 3
            or any(type(value) is not int for value in coordinates)
            or coordinates != (binding.target_x, binding.target_y, binding.target_z)
        ):
            return False
        dimension = getattr(descriptor, "dimension", "")
        if type(dimension) is not str:
            return False
        if detail == "typed":
            expected_command = f"goto {coordinates[0]} {coordinates[1]} {coordinates[2]}"
            if dimension:
                expected_command += f" {dimension}"
            if getattr(descriptor, "command", None) != expected_command:
                return False
        return binding.requested_dimension == (dimension or None)
