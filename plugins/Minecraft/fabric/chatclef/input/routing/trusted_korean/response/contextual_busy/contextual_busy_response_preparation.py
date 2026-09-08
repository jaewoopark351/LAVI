#20260905_kpopmodder: Keep trusted Korean route responsibilities split by module.
#20260909_kpopmodder: Carry one prepared decision and optional local custody token.
from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True, slots=True)
class ContextualBusyResponsePreparation:
    decision: object = field(repr=False)
    local_handoff_token: object = field(default=None, repr=False)


__all__ = ("ContextualBusyResponsePreparation",)
