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
    ) -> dict[str, Any]:
        text = natural_language_text(command)
        try:
            translated = ChatClefTranslationResultDTO.from_mapping(translation)
        except (TypeError, ValueError) as error:
            payload = self._result_factory.malformed_translation(error)
            self._result_recorder(payload, "submit_translated_command")
            return payload
        return self._submit_translation(
            command,
            translated,
            text,
            action="submit_translated_command",
        )

    def _submit_translation(
        self,
        command: Any,
        translation: ChatClefTranslationResultDTO,
        original_text: str,
        *,
        action: str,
    ) -> dict[str, Any]:
        if not translation.executable:
            payload = self._result_factory.translation_rejection(translation)
            self._result_recorder(payload, action)
            return payload

        command_name = translated_command_name(translation.command)
        if command_name:
            admission = self._admission.inspect(
                command_name,
                request_source(command),
                self._registry_provider(),
            )
            if not admission.allowed:
                payload = self._result_factory.admission_rejection(
                    command,
                    translation,
                    admission,
                )
                self._result_recorder(payload, action)
                return payload

        request = self._request_factory.build(
            command,
            translation,
            original_text,
        )
        return self._command_submitter(request)
