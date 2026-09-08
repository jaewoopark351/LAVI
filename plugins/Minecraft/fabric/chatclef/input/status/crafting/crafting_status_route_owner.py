#20260907_kpopmodder: Own zero-submission crafting status responses after trusted ingress.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.crafting import (
    CraftingFeedbackStatusSnapshot,
)


class CraftingStatusRouteOwner:
    def __init__(
        self,
        *,
        extension,
        live_proof_validator,
        classifier,
        response_renderer,
    ) -> None:
        self._extension = extension
        self._live_proof_validator = live_proof_validator
        self._classifier = classifier
        self._response_renderer = response_renderer

    def try_route(
        self,
        event: object,
        korean_eligibility_proof: object,
    ) -> MinecraftChatClefInputRouteDecision | None:
        query = self._classifier.classify(getattr(event, "text", None))
        if query is None:
            return None
        if self._live_proof_validator(
            korean_eligibility_proof,
            event,
        ) is not True:
            return None
        inspect_status = getattr(
            self._extension,
            "inspect_crafting_feedback_status",
            None,
        )
        snapshot = None
        if callable(inspect_status):
            try:
                snapshot = inspect_status(query.target_item)
            except Exception:
                snapshot = None
        state = (
            snapshot.state
            if type(snapshot) is CraftingFeedbackStatusSnapshot
            else CraftingFeedbackStatusSnapshot.UNAVAILABLE
        )
        acknowledgement = getattr(
            snapshot,
            "publication_acknowledgement",
            None,
        )
        try:
            return MinecraftChatClefInputRouteDecision.handled_result(
                reason="minecraft_crafting_status_query",
                response_text=self._response_renderer.render_status(state),
                result={
                    "ok": True,
                    "status": state,
                    "read_only": True,
                    "command_submitted": False,
                },
                route_kind="crafting_status_query",
                response_publication_acknowledgement=acknowledgement,
            )
        except Exception:
            callback = getattr(acknowledgement, "acknowledge", None)
            if callable(callback):
                try:
                    callback(published=False)
                except Exception:
                    pass
            raise


__all__ = ("CraftingStatusRouteOwner",)
