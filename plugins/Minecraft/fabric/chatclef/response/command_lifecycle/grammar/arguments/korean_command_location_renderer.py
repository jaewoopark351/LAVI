#20260907_kpopmodder: Render one bounded Minecraft destination noun phrase.
from __future__ import annotations


class KoreanCommandLocationRenderer:
    def render(self, descriptor: object) -> str:
        coordinate_values = getattr(descriptor, "coordinate_values", ())
        dimension = str(getattr(descriptor, "dimension", "") or "")
        if type(coordinate_values) is tuple and (coordinate_values or dimension):
            dimension_label = {
                "overworld": "오버월드",
                "nether": "네더",
                "end": "엔드",
            }.get(dimension, "")
            if len(coordinate_values) == 3:
                location = (
                    f"좌표 {coordinate_values[0]}, {coordinate_values[1]}, "
                    f"{coordinate_values[2]}"
                )
            elif len(coordinate_values) == 2:
                location = f"좌표 {coordinate_values[0]}, {coordinate_values[1]}"
            elif len(coordinate_values) == 1:
                location = f"높이 {coordinate_values[0]}"
            else:
                location = dimension_label
            if dimension_label and coordinate_values:
                return f"{dimension_label}의 {location}"
            return location
        coordinates = getattr(descriptor, "coordinates", None)
        if type(coordinates) is tuple and len(coordinates) == 3:
            return f"좌표 {coordinates[0]}, {coordinates[1]}, {coordinates[2]}"
        return "요청한 위치"


__all__ = ("KoreanCommandLocationRenderer",)
