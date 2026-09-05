#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from .stop_control_terminal_profile_schema import STOP_VERIFY_MAX_LATER_TICKS


class StopControlTerminalTickValidator:
    MAX_LATER_TICKS = STOP_VERIFY_MAX_LATER_TICKS

    def nullable(self, value: object) -> bool:
        return value is None or self.exact(value)

    def exact(self, value: object) -> bool:
        return type(value) is int and value >= 0

    def same(self, executed: object, verified: object) -> bool:
        return self.exact(executed) and verified == executed

    def ordered(self, executed: object, verified: object) -> bool:
        return (
            self.exact(executed)
            and self.exact(verified)
            and executed <= verified
            and verified - executed <= self.MAX_LATER_TICKS
        )

    def exact_timeout_boundary(self, executed: object, verified: object) -> bool:
        return (
            self.exact(executed)
            and self.exact(verified)
            and verified == executed + self.MAX_LATER_TICKS
        )


__all__ = ("StopControlTerminalTickValidator",)
