#20260907_kpopmodder: Replace one accepted technical acknowledgement with its natural START.
from __future__ import annotations

from dataclasses import replace

from plugins.Minecraft.fabric.chatclef.presentation.command_lifecycle import (
    CommandLifecyclePresentationDetailProjector,
)

from .coalesced import CommandLifecycleCoalescedResponseFactory
from .publication import CommandFeedbackReadyDecisionAcknowledgementFactory
from .selection import CommandFeedbackInitialResponseSelector


class CommandFeedbackStartDecisionDecorator:
    def __init__(
        self,
        response_renderer,
        presentation_detail_projector=None,
        initial_response_selector=None,
        coalesced_response_factory=None,
        ready_acknowledgement_factory=None,
    ) -> None:
        self._response_renderer = response_renderer
        self._presentation_details = (
            presentation_detail_projector
            or CommandLifecyclePresentationDetailProjector()
        )
        self._initial_response_selector = (
            initial_response_selector or CommandFeedbackInitialResponseSelector()
        )
        self._coalesced_responses = (
            coalesced_response_factory
            or CommandLifecycleCoalescedResponseFactory(
                response_renderer=response_renderer,
                presentation_detail_projector=self._presentation_details,
            )
        )
        self._ready_acknowledgements = (
            ready_acknowledgement_factory
            or CommandFeedbackReadyDecisionAcknowledgementFactory(
                initial_response_selector=self._initial_response_selector,
                coalesced_response_factory=self._coalesced_responses,
            )
        )

    def decorate(
        self,
        decision: object,
        *,
        descriptor: object = None,
        start_claimed: bool,
        publication_acknowledgement: object = None,
    ):
        if start_claimed is not True or descriptor is None:
            return decision
        acknowledgement = self._ready_acknowledgements.wrap(
            publication_acknowledgement
        )
        return replace(
            decision,
            response_text=self._response_renderer.render_start(descriptor),
            route_kind="command_lifecycle",
            response_kind="command_start",
            response_publication_acknowledgement=acknowledgement,
            presentation_detail_log=self._presentation_details.project(
                descriptor
            ),
        )


__all__ = ("CommandFeedbackStartDecisionDecorator",)
