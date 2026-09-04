#20260827_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from typing import Any, Callable

from plugins.Minecraft.fabric.chatclef.command_registry.admission import (
    KoreanCommandSubmissionAdmission,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.natural_language_command_input import (
    natural_language_text,
    request_source,
    translated_command_name,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.natural_language_command_result_payload_factory import (
    NaturalLanguageCommandResultPayloadFactory,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.translated_command_request_factory import (
    TranslatedCommandRequestFactory,
)
from plugins.Minecraft.fabric.chatclef.intent import ChatClefTranslationResultDTO


class NaturalLanguageCommandCoordinator:
    def __init__(
        self,
        *,
        natural_language_service: object,
        registry_provider: Callable[[], object],
        command_submitter: Callable[[Any], dict[str, Any]],
        result_recorder: Callable[[dict[str, Any], str], None],
        admission: KoreanCommandSubmissionAdmission | None = None,
        request_factory: TranslatedCommandRequestFactory | None = None,
        result_factory: NaturalLanguageCommandResultPayloadFactory | None = None,
    ):
        self._natural_language_service = natural_language_service
        self._registry_provider = registry_provider
        self._command_submitter = command_submitter
        self._result_recorder = result_recorder
        self._admission = admission or KoreanCommandSubmissionAdmission()
        self._request_factory = request_factory or TranslatedCommandRequestFactory()
        self._result_factory = (
            result_factory or NaturalLanguageCommandResultPayloadFactory()
        )

    def translate(self, command: Any) -> dict[str, Any]:
        text = natural_language_text(command)
        return self._natural_language_service.translate(text).to_dict()

    def handle(self, command: Any) -> dict[str, Any]:
        text = natural_language_text(command)
        translation = self._natural_language_service.translate(text)
        return self._submit_translation(
            command,
            translation,
            text,
            action="translate_natural_language_command",
        )

    def submit_translated(
        self,
        command: Any,
        translation: Any,
        *,
        route_claim: object = None,
    ) -> dict[str, Any]:
        try:
            try:
                text = natural_language_text(command)
            except Exception as error:
                payload = self._result_factory.operation_failure(
                    command,
                    "auto_deposit_trust_input_internal_error",
                    error,
                )
                self._result_recorder(payload, "submit_translated_command")
                return payload
            try:
                translated = ChatClefTranslationResultDTO.from_mapping(translation)
            except Exception as error:
                payload = self._result_factory.malformed_translation(error)
                self._result_recorder(payload, "submit_translated_command")
                return payload
            return self._submit_translation(
                command,
                translated,
                text,
                action="submit_translated_command",
                route_claim=route_claim,
            )
        finally:
            self._abandon_route_claim(route_claim)

    def _submit_translation(
        self,
        command: Any,
        translation: ChatClefTranslationResultDTO,
        original_text: str,
        *,
        action: str,
        route_claim: object = None,
    ) -> dict[str, Any]:
        try:
            if not translation.executable:
                payload = self._result_factory.translation_rejection(translation)
                self._result_recorder(payload, action)
                return payload

            command_name = translated_command_name(translation.command)
            source = request_source(command)
            if command_name:
                try:
                    inspection = self._admission.inspect(
                        command_name,
                        source,
                        self._registry_provider(),
                        route_claim,
                    )
                except Exception as error:
                    payload = self._result_factory.operation_failure(
                        command,
                        "auto_deposit_trust_input_internal_error",
                        error,
                    )
                    self._result_recorder(payload, action)
                    return payload
                if not inspection.allowed:
                    payload = self._result_factory.admission_rejection(
                        command,
                        translation,
                        inspection,
                    )
                    self._result_recorder(payload, action)
                    return payload
            else:
                inspection = None

            try:
                request = self._request_factory.build(
                    command,
                    translation,
                    original_text,
                )
            except Exception as error:
                payload = self._result_factory.operation_failure(
                    command,
                    "auto_deposit_trust_input_internal_error",
                    error,
                )
                self._result_recorder(payload, action)
                return payload

            if inspection is not None:
                commit = self._admission.commit(
                    inspection,
                    route_claim,
                    request,
                )
                if not commit.allowed:
                    payload = self._result_factory.admission_rejection(
                        command,
                        translation,
                        commit,
                    )
                    self._result_recorder(payload, action)
                    return payload

            return self._command_submitter(request)
        finally:
            self._abandon_route_claim(route_claim)

    def _abandon_route_claim(self, route_claim: object) -> None:
        try:
            self._admission.abandon_if_issued(route_claim)
        except Exception:
            return
