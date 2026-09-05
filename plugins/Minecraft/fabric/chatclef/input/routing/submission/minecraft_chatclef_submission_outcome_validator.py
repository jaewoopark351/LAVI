#20260905_kpopmodder: Isolate router submission outcome normalization.
from __future__ import annotations


class MinecraftChatClefSubmissionOutcomeValidator:
    def __init__(self, result_normalizer):
        self._result_normalizer = result_normalizer

    @property
    def result_normalizer(self):
        return self._result_normalizer

    def replace_result_normalizer(self, result_normalizer: object) -> None:
        self._result_normalizer = result_normalizer

    def validate(self, payload: object, *, request_id: str) -> dict[str, object]:
        return self._result_normalizer.normalize(
            payload,
            expected_request_id=request_id,
        )

    def transport_unknown(
        self,
        request_id: str,
        error: Exception,
        *,
        scoped: bool,
    ) -> dict[str, object]:
        label = "Fabric ChatClef scoped submit call" if scoped else (
            "Fabric ChatClef submit call"
        )
        return self._result_normalizer.unknown(
            request_id,
            f"{label} outcome is unknown: {type(error).__name__}: {error}",
        )

    def validation_unknown(
        self,
        request_id: str,
        error: Exception,
        *,
        scoped: bool,
    ) -> dict[str, object]:
        if scoped:
            message = (
                "Fabric ChatClef scoped submit result normalization outcome "
                f"is unknown: {type(error).__name__}: {error}"
            )
        else:
            message = (
                "Fabric ChatClef submit result normalization outcome is "
                f"unknown: {type(error).__name__}: {error}"
            )
        return self._result_normalizer.unknown(request_id, message)


__all__ = ("MinecraftChatClefSubmissionOutcomeValidator",)
