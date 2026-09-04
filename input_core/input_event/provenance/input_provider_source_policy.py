#20260905_kpopmodder: Carries one immutable descriptor-derived provider provenance policy.
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class InputProviderSourcePolicy:
    source: str
    provider_id: str
    event_kind: str
    final: bool


__all__ = ["InputProviderSourcePolicy"]
