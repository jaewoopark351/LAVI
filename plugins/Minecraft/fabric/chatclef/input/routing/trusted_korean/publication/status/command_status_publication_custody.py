#20260905_kpopmodder: Preserve trusted-route publication custody in a focused module.
#20260908_kpopmodder: Capture the original STATUS decision and acknowledgement identities.
from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True, slots=True)
class CommandStatusPublicationCustody:
    decision: object = field(repr=False)
    acknowledgement: object = field(repr=False)
    diagnostic_custody: object = field(repr=False)
    decision_type: type = field(repr=False)
    route_kind: str
    response_kind: str
    emergency: bool = False


__all__ = ("CommandStatusPublicationCustody",)
