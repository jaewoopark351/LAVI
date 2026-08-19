#20260819_kpopmodder: Normalize bounded gameplay timing inputs without accepting bool or non-finite values.
from __future__ import annotations

import math


def finite_positive_seconds(value: object) -> float | None:
    if isinstance(value, bool):
        return None
    try:
        parsed = float(value)
    except (TypeError, ValueError):
        return None
    return parsed if math.isfinite(parsed) and parsed > 0 else None
