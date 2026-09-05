#20260905_kpopmodder: Preserve translated submission APIs as a thin pipeline facade.
from __future__ import annotations

from typing import Any, Callable

from plugins.Minecraft.fabric.chatclef.command_registry.admission import (
    KoreanCommandSubmissionAdmission,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.natural_language_command_result_payload_factory import (
    NaturalLanguageCommandResultPayloadFactory,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.submission.composition import (
    TranslatedCommandSubmissionComponentGraph,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.translated_command_request_factory import (
    TranslatedCommandRequestFactory,
)
from plugins.Minecraft.fabric.chatclef.intent import ChatClefTranslationResultDTO


class TranslatedCommandSubmissionCoordinator:
    def __init__(
        self,
        *,
        registry_provider: Callable[[], object],
        command_submitter: Callable[[Any], dict[str, Any]],
        result_recorder: Callable[[dict[str, Any], str], None],
        admission: KoreanCommandSubmissionAdmission,
        request_factory: TranslatedCommandRequestFactory,
        result_factory: NaturalLanguageCommandResultPayloadFactory,
    ):
        self._component_graph = TranslatedCommandSubmissionComponentGraph(
            registry_provider=registry_provider,
            command_submitter=command_submitter,
            result_recorder=result_recorder,
            admission=admission,
            request_factory=request_factory,
            result_factory=result_factory,
        )
        self._component_graph.install_compatibility_seams(self)

    @property
    def request_factory(self) -> object:
        return self._request_stage.request_factory

    def replace_request_factory(self, request_factory: object) -> None:
        self._request_stage.replace_request_factory(request_factory)

    @property
    def _request_factory(self) -> object:
        return self._request_stage.request_factory

    @_request_factory.setter
    def _request_factory(self, request_factory: object) -> None:
        self._request_stage.replace_request_factory(request_factory)

    def submit_translated(
        self,
        command: Any,
        translation: Any,
        *,
        route_claim: object = None,
    ) -> dict[str, Any]:
        return self._pipeline.submit_translated(
            command,
            translation,
            route_claim=route_claim,
        )

    def submit_translation(
        self,
        command: Any,
        translation: ChatClefTranslationResultDTO,
        original_text: str,
        *,
        action: str,
        route_claim: object = None,
        pre_submit_guard: (
            Callable[
                [Any, ChatClefTranslationResultDTO],
                dict[str, Any] | None,
            ]
            | None
        ) = None,
    ) -> dict[str, Any]:
        return self._pipeline.submit_translation(
            command,
            translation,
            original_text,
            action=action,
            route_claim=route_claim,
            pre_submit_guard=pre_submit_guard,
        )

    def _abandon_route_claim(self, route_claim: object) -> None:
        self._route_claim_lifecycle.abandon_if_issued(route_claim)
