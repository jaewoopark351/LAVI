#20260905_kpopmodder: Records one LLM-owned routed-response generation outcome.
from dataclasses import dataclass

from .presentation import (
    RoutedResponsePresentationMetadata,
    RoutedResponsePresentationReceipt,
)


@dataclass(frozen=True, slots=True)
class RoutedResponseEmission:
    text: str
    source: str
    response_generation: int | None
    output_delivered: bool
    full_output_delivered: bool
    history_remembered: bool
    event_id: str = "none"
    route_kind: str = "minecraft_chatclef_external"
    response_kind: str = "external"
    delivery_mode: str = "current_input"
    presentation_metadata: RoutedResponsePresentationMetadata | None = None
    presentation_receipt: RoutedResponsePresentationReceipt | None = None


__all__ = ("RoutedResponseEmission",)
