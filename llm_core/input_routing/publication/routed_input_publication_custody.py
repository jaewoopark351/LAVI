#20260905_kpopmodder: Carry one backend-neutral publication claim across dispatcher stages.
from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True, slots=True)
class RoutedInputPublicationCustody:
    decision: object = field(repr=False)
    acknowledgement: object = field(repr=False)
    decision_type: type = field(repr=False)
    route_kind: object
    response_kind: object
    predicate: object = field(repr=False)
    failure_observer: object = field(repr=False)


__all__ = ("RoutedInputPublicationCustody",)
