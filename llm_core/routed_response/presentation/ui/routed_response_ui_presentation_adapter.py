#20260905_kpopmodder: Keep routed-response UI presentation in the established responsibility-split boundary.
#20260907_kpopmodder: Render a typed source badge for routed responses.
#20260908_kpopmodder: Preserve lifecycle presentation identity through Gradio metadata normalization.
from __future__ import annotations

import gradio as gr

from ..routed_response_presentation_metadata import (
    RoutedResponsePresentationMetadata,
)
from .routed_response_ui_presentation_identity import (
    RoutedResponseUiPresentationIdentity,
)


class RoutedResponseUiPresentationAdapter:
    SINK = "ui_presentation"

    def render(
        self,
        text: object,
        metadata: RoutedResponsePresentationMetadata,
        *,
        presentation_identity: RoutedResponseUiPresentationIdentity,
    ):
        if type(metadata) is not RoutedResponsePresentationMetadata:
            raise TypeError("presentation metadata must be exact")
        if (
            type(presentation_identity)
            is not RoutedResponseUiPresentationIdentity
        ):
            raise TypeError("presentation_identity must be exact")
        ui_metadata = {
            "title": metadata.badge_label,
            "id": presentation_identity.token,
        }
        if metadata.detail_log:
            ui_metadata["log"] = metadata.detail_log
        return gr.ChatMessage(
            content=str(text or ""),
            role="assistant",
            metadata=ui_metadata,
        )


__all__ = ("RoutedResponseUiPresentationAdapter",)
