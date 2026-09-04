#20260818_kpopmodder: Orchestrate one non-retranslating Fabric ChatClef submit call.
#20260819_kpopmodder: Normalize every post-submit outcome without replaying it.
from __future__ import annotations

from typing import Any, Mapping

from .submission_request_factory import (
    MinecraftChatClefSubmissionRequestFactory,
)
from .submission_result_normalizer import (
    MinecraftChatClefSubmissionResultNormalizer,
)


class MinecraftChatClefSubmissionBoundary:
    def __init__(self):
        self._request_factory = MinecraftChatClefSubmissionRequestFactory()
        self._result_normalizer = MinecraftChatClefSubmissionResultNormalizer()

    def is_available(self, extension: Any) -> bool:
        return callable(getattr(extension, "submit_translated_command", None))

    def submit_once(
        self,
        extension: Any,
        event: Any,
        translation: Mapping[str, Any],
        *,
        route_claim: object = None,
        original_text: str | None = None,
        translation_input_text: str | None = None,
    ) -> dict[str, Any]:
        request = self._request_factory.build(
            event,
            original_text=original_text,
            translation_input_text=translation_input_text,
        )
        request_id = str(request["request_id"])
        submitter = getattr(extension, "submit_translated_command")
        try:
            if route_claim is None:
                payload = submitter(request, dict(translation))
            else:
                payload = submitter(
                    request,
                    dict(translation),
                    route_claim=route_claim,
                )
        except Exception as error:
            result = self._result_normalizer.unknown(
                request_id,
                "Fabric ChatClef submit call outcome is unknown: "
                f"{type(error).__name__}: {error}",
            )
        else:
            try:
                result = self._result_normalizer.normalize(
                    payload,
                    expected_request_id=request_id,
                )
            except Exception as error:
                result = self._result_normalizer.unknown(
                    request_id,
                    "Fabric ChatClef submit result normalization outcome is "
                    f"unknown: {type(error).__name__}: {error}",
                )
        return result
