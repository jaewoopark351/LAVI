#20260818_kpopmodder: Render Fabric ChatClef routing outcomes into the existing LAVI decision contract.
#20260819_kpopmodder: Report verified reconciliation without submitting the triggering command.
#20260905_kpopmodder: Delegate independent decision families to focused builders.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import MinecraftChatClefInputRouteDecision
from plugins.Minecraft.fabric.chatclef.response import ChatClefCommandResponseRenderer

from plugins.Minecraft.fabric.chatclef.input.routing.decisions.minecraft_route_decision_component_graph import MinecraftRouteDecisionComponentGraph
from .submission_readiness import MinecraftChatClefSubmissionReadiness


class MinecraftChatClefRouteDecisionFactory:
    def __init__(
        self,
        response_renderer: ChatClefCommandResponseRenderer | None = None,
    ):
        graph = MinecraftRouteDecisionComponentGraph(response_renderer)
        self._response_renderer = graph.response_renderer
        self._status_classifier = graph.status_classifier
        self._submission_factory = graph.submission_factory
        self._translation_factory = graph.translation_factory
        self._item_command_factory = graph.item_command_factory
        self._auto_deposit_trust_factory = graph.auto_deposit_trust_factory
        self._generic_crafting_defaults_factory = (
            graph.generic_crafting_defaults_factory
        )

    def reconciled_without_submission(
        self,
        request_id: str,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._submission_factory.reconciled_without_submission(request_id)

    def precheck_rejection(
        self,
        readiness: MinecraftChatClefSubmissionReadiness,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._submission_factory.precheck_rejection(readiness)

    def submitted(
        self,
        translation: Mapping[str, Any],
        result: Mapping[str, Any],
    ) -> MinecraftChatClefInputRouteDecision:
        return self._submission_factory.submitted(translation, result)

    def translation_rejection(
        self,
        translation: Mapping[str, Any],
    ) -> MinecraftChatClefInputRouteDecision:
        return self._translation_factory.rejection(translation)

    def item_command_translation_rejection(
        self,
        translation: Mapping[str, Any],
        reason_code: str,
        message: str,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._item_command_factory.translation_rejection(
            translation,
            reason_code,
            message,
        )

    def malformed_translation(
        self,
        error: Exception,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._translation_factory.malformed(error)

    def operation_failure(
        self,
        reason: str,
        error: Exception,
        translation: Mapping[str, Any] | None = None,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._translation_factory.operation_failure(reason, error, translation)

    def input_rejection(
        self,
        reason_code: str,
        message: str,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._auto_deposit_trust_factory.input_rejection(reason_code, message)

    def generic_crafting_defaults_rejection(
        self,
        reason_code: str,
        message: str,
        translation: Mapping[str, Any] | None = None,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._generic_crafting_defaults_factory.rejection(
            reason_code,
            message,
            translation,
        )

    def _response_text(
        self,
        translation: Mapping[str, Any],
        result: Mapping[str, Any],
    ) -> str:
        return self._submission_factory.response_text(translation, result)

    def result_status(self, result: Mapping[str, Any]) -> str:
        return self._status_classifier.classify(result)
