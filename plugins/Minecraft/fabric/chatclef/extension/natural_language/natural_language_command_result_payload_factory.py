#20260827_kpopmodder: Added this module to keep one project class per Python file.
#20260905_kpopmodder: Render fail-closed scoped-crafting authorization rejection payloads.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.command_registry.admission import (
    KoreanCommandSubmissionAdmissionDecision,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.natural_language_command_input import (
    request_id,
)
from plugins.Minecraft.fabric.chatclef.intent import ChatClefTranslationResultDTO


class NaturalLanguageCommandResultPayloadFactory:
    def translation_rejection(
        self,
        translation: ChatClefTranslationResultDTO,
    ) -> dict[str, Any]:
        return {
            "ok": False,
            "status": translation.to_dict(),
            "error": translation.reason_code,
            "message": translation.message,
            "details": translation.data,
        }

    def admission_rejection(
        self,
        command: Any,
        translation: ChatClefTranslationResultDTO,
        decision: KoreanCommandSubmissionAdmissionDecision,
    ) -> dict[str, Any]:
        rejected_request_id = request_id(command)
        data = dict(translation.data)
        data.update(
            {
                "blocked_command": decision.command_name,
                #20260905_kpopmodder: Distinguish public control admission from not-public rollout.
                "public_korean_enabled": (
                    decision.reason_code != "korean_command_not_public"
                ),
            }
        )
        if decision.expose_admission_reason:
            data.update(
                {
                    "blocked_source": decision.source,
                    "admission_reason": decision.reason_code,
                }
            )
        return {
            "request_id": rejected_request_id,
            "ok": False,
            "status": {
                "request_id": rejected_request_id,
                "ok": False,
                "status": CommandResultStatus.REJECTED.value,
                "error_code": BridgeErrorCode.INVALID_REQUEST.value,
                "message": decision.message,
                "data": data,
            },
            "error": BridgeErrorCode.INVALID_REQUEST.value,
            "message": decision.message,
            "details": data,
        }

    def malformed_translation(self, error: Exception) -> dict[str, Any]:
        message = f"{type(error).__name__}: {error}"
        return {
            "ok": False,
            "status": {},
            "error": "malformed_translation_result",
            "message": message,
            "details": {},
        }

    def generic_crafting_defaults_rejection(
        self,
        command: Any,
        reason_code: str,
        message: str,
    ) -> dict[str, Any]:
        rejected_request_id = request_id(command)
        details = {
            "reason_code": str(reason_code or "generic_crafting_activation_invalid"),
            "generic_crafting_defaults": True,
        }
        return {
            "request_id": rejected_request_id,
            "ok": False,
            "status": {
                "request_id": rejected_request_id,
                "ok": False,
                "status": CommandResultStatus.REJECTED.value,
                "error_code": BridgeErrorCode.INVALID_REQUEST.value,
                "message": str(message or "Feature-B activation is invalid."),
                "data": dict(details),
            },
            "error": BridgeErrorCode.INVALID_REQUEST.value,
            "message": str(message or "Feature-B activation is invalid."),
            "details": details,
        }

    def operation_failure(
        self,
        command: Any,
        reason_code: str,
        error: Exception,
    ) -> dict[str, Any]:
        rejected_request_id = request_id(command)
        message = f"{type(error).__name__}: {error}"
        return {
            "request_id": rejected_request_id,
            "ok": False,
            "status": {
                "request_id": rejected_request_id,
                "ok": False,
                "status": CommandResultStatus.REJECTED.value,
                "error_code": BridgeErrorCode.INTERNAL_ERROR.value,
                "message": message,
                "data": {"reason_code": reason_code},
            },
            "error": BridgeErrorCode.INTERNAL_ERROR.value,
            "message": message,
            "details": {"reason_code": reason_code},
        }
