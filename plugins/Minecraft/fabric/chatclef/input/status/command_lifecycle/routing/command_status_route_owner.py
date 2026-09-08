#20260907_kpopmodder: Own one trusted zero-submission generalized status response.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.presentation.command_lifecycle import (
    CommandLifecyclePresentationDetailProjector,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackLifecycleSnapshot,
)


class CommandStatusRouteOwner:
    def __init__(
        self,
        *,
        extension,
        live_proof_validator,
        classifier,
        response_renderer,
        presentation_detail_projector=None,
    ) -> None:
        self._extension = extension
        self._live_proof_validator = live_proof_validator
        self._classifier = classifier
        self._response_renderer = response_renderer
        self._presentation_details = (
            presentation_detail_projector
            or CommandLifecyclePresentationDetailProjector()
        )

    def try_route(self, event: object, korean_eligibility_proof: object):
        query = self._classifier.classify(getattr(event, "text", None))
        if query is None:
            return None
        if self._live_proof_validator(korean_eligibility_proof, event) is not True:
            return None
        snapshot = self._inspect(query)
        if snapshot is None and getattr(query, "addressed", False) is not True:
            return None
        if snapshot is None:
            snapshot = CommandFeedbackLifecycleSnapshot(
                state=CommandFeedbackLifecycleSnapshot.UNAVAILABLE,
                query_matched=True,
                owner_present=False,
                availability_reason="inspection_failed",
            )
        if snapshot is not None and getattr(snapshot, "query_matched", True) is not True:
            return None
        state = (
            snapshot.state
            if type(snapshot) is CommandFeedbackLifecycleSnapshot
            else CommandFeedbackLifecycleSnapshot.UNAVAILABLE
        )
        acknowledgement = getattr(snapshot, "publication_acknowledgement", None)
        try:
            return MinecraftChatClefInputRouteDecision.handled_result(
                reason="minecraft_command_status_query",
                response_text=self._response_renderer.render_status(snapshot, query),
                result={
                    "ok": True,
                    "status": state,
                    "read_only": True,
                    "command_submitted": False,
                },
                route_kind="command_status_query",
                response_kind="command_status",
                response_publication_acknowledgement=acknowledgement,
                presentation_detail_log=self._presentation_details.project(
                    getattr(snapshot, "descriptor", None)
                ),
            )
        except Exception:
            callback = getattr(acknowledgement, "acknowledge", None)
            if callable(callback):
                try:
                    callback(published=False)
                except Exception:
                    pass
            raise

    def _inspect(self, query: object):
        inspect = getattr(self._extension, "inspect_command_feedback_status", None)
        if callable(inspect):
            try:
                return inspect(query)
            except Exception:
                return None
        legacy = getattr(self._extension, "inspect_crafting_feedback_status", None)
        if callable(legacy) and getattr(query, "requested_family", "") == "item_get":
            try:
                return legacy(None)
            except Exception:
                return None
        return None


__all__ = ("CommandStatusRouteOwner",)
