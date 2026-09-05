#20260905_kpopmodder: Select trusted feedback wording from typed route outcomes.
from __future__ import annotations

from typing import Mapping

from plugins.Minecraft.fabric.chatclef.response.generic_crafting_defaults_response_renderer import (
    GenericCraftingDefaultsResponseRenderer,
)

from .trusted_korean_command_feedback_renderer import (
    TrustedKoreanCommandFeedbackRenderer,
)
from plugins.Minecraft.fabric.chatclef.response.trusted_korean.trusted_korean_feedback_route_selector import TrustedKoreanFeedbackRouteSelector


class TrustedKoreanCommandFeedbackFacade:
    def __init__(
        self,
        *,
        feedback_renderer: TrustedKoreanCommandFeedbackRenderer | None = None,
        generic_crafting_renderer: (
            GenericCraftingDefaultsResponseRenderer | None
        ) = None,
        route_selector: TrustedKoreanFeedbackRouteSelector | None = None,
    ):
        self._feedback_renderer = (
            feedback_renderer or TrustedKoreanCommandFeedbackRenderer()
        )
        self._generic_crafting_renderer = (
            generic_crafting_renderer or GenericCraftingDefaultsResponseRenderer()
        )
        self._route_selector = route_selector or TrustedKoreanFeedbackRouteSelector()

    def render(self, decision: object) -> str:
        existing = str(getattr(decision, "response_text", "") or "").strip()
        route = self._route_selector.select(decision)
        if route == self._route_selector.DISCONNECTED:
            return self._feedback_renderer.render_disconnected()
        if route == self._route_selector.BUSY:
            return self._feedback_renderer.render_busy()
        if route == self._route_selector.TERMINAL_UNKNOWN:
            return self._feedback_renderer.render_terminal_unknown()
        if route == self._route_selector.ITEM_REJECTION:
            return self._feedback_renderer.render_item_command_rejection(
                self._result_message(decision)
            )
        if route == self._route_selector.CRAFTING_REJECTION:
            return self._generic_crafting_renderer.render_rejection(
                self._result_error(decision),
                self._result_message(decision),
            )
        if route == self._route_selector.CRAFTING_SUBMITTED:
            rendered = self._generic_crafting_renderer.render_submitted(
                self._mapping(getattr(decision, "translation", None)),
                self._mapping(getattr(decision, "result", None)),
            )
            if rendered is not None:
                return rendered
        return existing

    def _result_message(self, decision: object) -> object:
        return self._mapping(getattr(decision, "result", None)).get("message")

    def _result_error(self, decision: object) -> object:
        return self._mapping(getattr(decision, "result", None)).get("error")

    def _mapping(self, value: object) -> Mapping:
        return value if isinstance(value, Mapping) else {}


__all__ = ("TrustedKoreanCommandFeedbackFacade",)
