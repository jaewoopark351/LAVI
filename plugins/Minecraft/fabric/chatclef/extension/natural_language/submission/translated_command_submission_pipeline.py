#20260905_kpopmodder: Orchestrate translated-command stages without owning their policies.
from __future__ import annotations

from typing import Any, Callable

from plugins.Minecraft.fabric.chatclef.intent import ChatClefTranslationResultDTO

from .pipeline import TranslatedCommandPipelineComponentGraph


class TranslatedCommandSubmissionPipeline:
    def __init__(
        self,
        *,
        command_submitter,
        result_recorder,
        result_factory,
        input_stage,
        admission_stage,
        request_stage,
        route_claim_lifecycle,
    ):
        self._command_submitter = command_submitter
        self._result_recorder = result_recorder
        self._result_factory = result_factory
        self._input_stage = input_stage
        self._admission_stage = admission_stage
        self._request_stage = request_stage
        self._route_claim_lifecycle = route_claim_lifecycle
        self._component_graph = TranslatedCommandPipelineComponentGraph(
            command_submitter=command_submitter,
            result_recorder=result_recorder,
            result_factory=result_factory,
            request_stage=request_stage,
        )
        self._request_builder = self._component_graph.request_builder
        self._pre_submit_guard = self._component_graph.guard
        self._transport = self._component_graph.transport
        self._failure_handler = self._component_graph.failure_handler
        self._result_handler = self._component_graph.result_handler

    def submit_translated(
        self,
        command: Any,
        translation: Any,
        *,
        route_claim: object = None,
    ) -> dict[str, Any]:
        try:
            try:
                text = self._input_stage.natural_language_text(command)
            except Exception as error:
                return self._failure_handler.operation_failure(
                    command,
                    error,
                    "submit_translated_command",
                )
            try:
                translated = self._input_stage.translation(translation)
            except Exception as error:
                return self._failure_handler.malformed_translation(
                    error,
                    "submit_translated_command",
                )
            return self.submit_translation(
                command,
                translated,
                text,
                action="submit_translated_command",
                route_claim=route_claim,
            )
        finally:
            self._route_claim_lifecycle.abandon_if_issued(route_claim)

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
        try:
            if not translation.executable:
                return self._result_handler.translation_rejection(
                    translation,
                    action,
                )

            command_name = self._input_stage.command_name(translation.command)
            source = self._input_stage.source(command)
            if command_name:
                try:
                    inspection = self._admission_stage.inspect(
                        command_name,
                        source,
                        route_claim,
                    )
                except Exception as error:
                    return self._failure_handler.operation_failure(
                        command,
                        error,
                        action,
                    )
                if not inspection.allowed:
                    return self._result_handler.admission_rejection(
                        command,
                        translation,
                        inspection,
                        action,
                    )
            else:
                inspection = None

            try:
                request = self._request_builder.build(
                    command,
                    translation,
                    original_text,
                )
            except Exception as error:
                return self._failure_handler.operation_failure(
                    command,
                    error,
                    action,
                )

            if inspection is not None:
                commit = self._admission_stage.commit(
                    inspection,
                    route_claim,
                    request,
                )
                if not commit.allowed:
                    return self._result_handler.admission_rejection(
                        command,
                        translation,
                        commit,
                        action,
                    )

            payload = self._pre_submit_guard.invoke(
                pre_submit_guard,
                request,
                translation,
            )
            if payload is not None:
                return self._result_handler.guarded(payload, action)

            return self._transport.submit(request)
        finally:
            self._route_claim_lifecycle.abandon_if_issued(route_claim)


__all__ = ("TranslatedCommandSubmissionPipeline",)
