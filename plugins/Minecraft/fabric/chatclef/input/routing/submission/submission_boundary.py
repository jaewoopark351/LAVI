#20260818_kpopmodder: Orchestrate one non-retranslating Fabric ChatClef submit call.
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
        text: str,
        translation: Mapping[str, Any],
    ) -> dict[str, Any]:
        request = self._request_factory.build(text)
        submitter = getattr(extension, "submit_translated_command")
        try:
            result = submitter(request, dict(translation))
        except Exception as error:
            return self._result_normalizer.unknown(
                str(request["request_id"]),
                "Fabric ChatClef submit call outcome is unknown: "
                f"{type(error).__name__}: {error}",
            )
        return self._result_normalizer.normalize(
            result,
            expected_request_id=str(request["request_id"]),
        )
