#20260818_kpopmodder: Normalize one untrusted extension submit result fail-closed.
#20260819_kpopmodder: Keep the legacy normalizer import as a focused compatibility facade.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode

from .result import SubmissionResultFactory, SubmissionResultValidator


class MinecraftChatClefSubmissionResultNormalizer:
    def __init__(self):
        self._validator = SubmissionResultValidator()
        self._factory = SubmissionResultFactory()

    def normalize(
        self,
        payload: Any,
        *,
        expected_request_id: str | None = None,
    ) -> dict[str, Any]:
        validated, request_id, error_message = self._validator.validate(
            payload,
            expected_request_id=expected_request_id,
        )
        if validated is None:
            return self._factory.unknown(
                request_id,
                str(
                    error_message
                    or "Fabric ChatClef submission outcome is unknown."
                ),
            )
        return self._factory.canonical(validated)

    def unknown(
        self,
        expected_request_id: str | None,
        message: str,
        *,
        error: str = BridgeErrorCode.INTERNAL_ERROR.value,
    ) -> dict[str, Any]:
        return self._factory.unknown(
            expected_request_id,
            message,
            error=error,
        )
