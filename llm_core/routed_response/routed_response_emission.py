#20260905_kpopmodder: Records one LLM-owned routed-response generation outcome.
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class RoutedResponseEmission:
    text: str
    source: str
    response_generation: int
    output_delivered: bool
    full_output_delivered: bool
    history_remembered: bool
    event_id: str = "none"
    route_kind: str = "minecraft_chatclef_external"
    response_kind: str = "external"


__all__ = ("RoutedResponseEmission",)
