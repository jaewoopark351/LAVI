#20260905_kpopmodder: Return one typed local STOP submission outcome to the router.
from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class StopControlSubmissionResult:
    accepted: bool
    reason: str
    result: dict[str, Any]


__all__ = ("StopControlSubmissionResult",)
