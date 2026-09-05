#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations


class StopControlTransitionAtomEncoder:
    def encode(self, value: object) -> str:
        if value is None:
            return "none"
        if type(value) is bool:
            return "true" if value else "false"
        if type(value) is int:
            return str(value) if -(2**63) <= value < 2**63 else "invalid"
        if type(value) is not str or not value or len(value) > 160:
            return "invalid"
        if any(character.isspace() or character in "=\r\n" for character in value):
            return "invalid"
        return value


__all__ = ("StopControlTransitionAtomEncoder",)
