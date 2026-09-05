#20260905_kpopmodder: Preserve submission decision APIs as a thin facade.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.response import ChatClefCommandResponseRenderer

from ..submission_readiness import MinecraftChatClefSubmissionReadiness
from .minecraft_submission_result_status_classifier import (
    MinecraftSubmissionResultStatusClassifier,
)
from .submission import MinecraftSubmissionRouteDecisionComponentGraph


class MinecraftSubmissionRouteDecisionFactory:
    def __init__(
        self,
        response_renderer: ChatClefCommandResponseRenderer,
        status_classifier: MinecraftSubmissionResultStatusClassifier,
    ):
        self._response_renderer = response_renderer
        self._status_classifier = status_classifier
        self._component_graph = MinecraftSubmissionRouteDecisionComponentGraph(
            response_renderer,
            status_classifier,
        )
        self._reconciliation_factory = (
            self._component_graph.reconciliation_factory
        )
        self._precheck_factory = self._component_graph.precheck_factory
        self._submitted_factory = self._component_graph.submitted_factory

    def reconciled_without_submission(
        self,
        request_id: str,
    ):
        return self._reconciliation_factory.build(request_id)

    def precheck_rejection(
        self,
        readiness: MinecraftChatClefSubmissionReadiness,
    ):
        return self._precheck_factory.build(readiness)

    def submitted(
        self,
        translation: Mapping[str, Any],
        result: Mapping[str, Any],
    ):
        return self._submitted_factory.build(translation, result)

    def response_text(
        self,
        translation: Mapping[str, Any],
        result: Mapping[str, Any],
    ) -> str:
        return self._submitted_factory.response_text(translation, result)


__all__ = ("MinecraftSubmissionRouteDecisionFactory",)
