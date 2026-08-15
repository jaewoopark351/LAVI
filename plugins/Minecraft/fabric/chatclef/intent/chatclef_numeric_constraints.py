#20260816_kpopmodder: Centralize strict numeric bounds for Korean ChatClef intent slots.
from __future__ import annotations

from typing import Any


JAVA_INT_MIN = -2147483648
JAVA_INT_MAX = 2147483647


class ChatClefNumericConstraints:
    @staticmethod
    def optional_exact_int(value: Any, field_name: str) -> int | None:
        if value is None:
            return None
        return ChatClefNumericConstraints.required_exact_int(value, field_name)

    @staticmethod
    def required_exact_int(value: Any, field_name: str) -> int:
        if type(value) is not int:
            raise TypeError(f"{field_name}_must_be_exact_int")
        return value

    @staticmethod
    def positive_java_int(value: Any, field_name: str) -> int:
        number = ChatClefNumericConstraints.required_exact_int(value, field_name)
        if number < 1 or number > JAVA_INT_MAX:
            raise ValueError(f"invalid_{field_name}")
        return number

    @staticmethod
    def java_int(value: Any, field_name: str) -> int:
        number = ChatClefNumericConstraints.required_exact_int(value, field_name)
        if number < JAVA_INT_MIN or number > JAVA_INT_MAX:
            raise ValueError(f"invalid_{field_name}")
        return number
