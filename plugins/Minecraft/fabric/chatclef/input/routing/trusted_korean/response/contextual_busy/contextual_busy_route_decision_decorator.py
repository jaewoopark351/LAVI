#20260905_kpopmodder: Keep trusted Korean route responsibilities split by module.
#20260909_kpopmodder: Decorate only an already rejected typed busy decision.
from __future__ import annotations

from dataclasses import replace

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)


class ContextualBusyRouteDecisionDecorator:
    ROUTE_KIND = "command_busy_current_work"
    RESPONSE_KIND = "command_status"
    RESPONSE_SOURCE = "minecraft_chatclef"
    CAUTIOUS_TEXT = "지금 마인크래프트 작업 상태를 확인하지 못했어"

    def current_work(
        self,
        decision: object,
        *,
        response_text: object,
        acknowledgement: object,
        presentation_detail_log: object,
    ) -> MinecraftChatClefInputRouteDecision:
        text = self._nonempty_text(response_text)
        detail = self._text(presentation_detail_log)
        return self._decorate(
            decision,
            response_text=text,
            acknowledgement=acknowledgement,
            presentation_detail_log=detail,
        )

    def cautious(
        self,
        decision: object,
        *,
        acknowledgement: object,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._decorate(
            decision,
            response_text=self.CAUTIOUS_TEXT,
            acknowledgement=acknowledgement,
            presentation_detail_log="",
        )

    def _decorate(
        self,
        decision: object,
        *,
        response_text: str,
        acknowledgement: object,
        presentation_detail_log: str,
    ) -> MinecraftChatClefInputRouteDecision:
        if type(decision) is not MinecraftChatClefInputRouteDecision:
            raise TypeError("contextual busy decoration requires an exact route decision")
        if acknowledgement is None:
            raise TypeError("contextual busy decoration requires an acknowledgement")
        return replace(
            decision,
            response_text=response_text,
            publish_external_response=False,
            response_source=self.RESPONSE_SOURCE,
            response_emission_capability=None,
            suppress_response=False,
            route_kind=self.ROUTE_KIND,
            response_kind=self.RESPONSE_KIND,
            response_publication_acknowledgement=acknowledgement,
            presentation_detail_log=presentation_detail_log,
        )

    @staticmethod
    def _nonempty_text(value: object) -> str:
        if type(value) is not str or not value:
            raise TypeError("contextual busy response text must be nonempty")
        return value

    @staticmethod
    def _text(value: object) -> str:
        if type(value) is not str:
            raise TypeError("contextual busy presentation detail must be text")
        return value


__all__ = ("ContextualBusyRouteDecisionDecorator",)
