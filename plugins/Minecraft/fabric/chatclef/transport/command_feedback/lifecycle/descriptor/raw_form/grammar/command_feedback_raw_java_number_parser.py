#20260907_kpopmodder: Parse only Java-compatible bounded numeric raw slots.
from __future__ import annotations

import math
import re


class CommandFeedbackRawJavaNumberParser:
    _INTEGER = re.compile(r"[+-]?[0-9]+\Z", re.ASCII)
    _DECIMAL_DOUBLE = re.compile(
        r"[+-]?(?:(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)(?:[eE][+-]?[0-9]+)?)[fFdD]?\Z",
        re.ASCII,
    )
    _HEX_DOUBLE = re.compile(
        r"[+-]?0[xX](?:[0-9a-fA-F]+(?:\.[0-9a-fA-F]*)?|\.[0-9a-fA-F]+)[pP][+-]?[0-9]+[fFdD]?\Z",
        re.ASCII,
    )
    _JAVA_INT_MIN = -(2**31)
    _JAVA_INT_MAX = 2**31 - 1

    def integer(self, value: str, *, positive: bool) -> int | None:
        if self._INTEGER.fullmatch(value) is None:
            return None
        parsed = int(value, 10)
        if not self._JAVA_INT_MIN <= parsed <= self._JAVA_INT_MAX:
            return None
        if positive and parsed < 1:
            return None
        return parsed

    def is_finite_double(self, value: str) -> bool:
        candidate = value[:-1] if value[-1:] in {"f", "F", "d", "D"} else value
        try:
            if self._DECIMAL_DOUBLE.fullmatch(value):
                parsed = float(candidate)
            elif self._HEX_DOUBLE.fullmatch(value):
                parsed = float.fromhex(candidate)
            else:
                return False
        except (OverflowError, ValueError):
            return False
        return math.isfinite(parsed)


__all__ = ("CommandFeedbackRawJavaNumberParser",)
