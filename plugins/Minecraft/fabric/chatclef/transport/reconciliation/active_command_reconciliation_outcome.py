#20260821_kpopmodder: Return one immutable result for accepted result handling and audit logging.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class ActiveCommandReconciliationOutcome:
    accepted: bool
    reason: str
    before_snapshot: dict[str, Any]
    after_snapshot: dict[str, Any]
    audit: dict[str, Any] = field(default_factory=dict)

