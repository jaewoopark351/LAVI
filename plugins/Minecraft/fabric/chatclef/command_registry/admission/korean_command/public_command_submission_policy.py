#20260905_kpopmodder: Isolate registry readiness and public-source admission policy.
from __future__ import annotations

from ..korean_command_submission_admission_decision import (
    KoreanCommandSubmissionAdmissionDecision,
)
from ..store_home_command_admission import StoreHomeCommandAdmission


class KoreanPublicCommandSubmissionPolicy:
    _NOT_PUBLIC_MESSAGE = (
        "Korean command parsed successfully but is not public-enabled for "
        "automatic Minecraft submission."
    )

    def __init__(
        self,
        store_home_admission: StoreHomeCommandAdmission | None = None,
    ):
        self._store_home_admission = (
            store_home_admission or StoreHomeCommandAdmission()
        )

    def unknown(self, command_name: str, source: str) -> KoreanCommandSubmissionAdmissionDecision:
        return self._not_public(command_name, source)

    def inspect(
        self,
        command_name: str,
        source: str,
        spec: object,
    ) -> KoreanCommandSubmissionAdmissionDecision:
        store_home = self._store_home_admission.inspect(
            command_name,
            source,
            spec,
        )
        if not store_home.allowed:
            return KoreanCommandSubmissionAdmissionDecision(
                allowed=False,
                command_name=command_name,
                source=source,
                reason_code=store_home.reason_code,
                message=store_home.message,
                expose_admission_reason=True,
            )
        if not spec.readiness_axes.public_korean_enabled:
            return self._not_public(command_name, source)
        if source not in spec.allowed_input_sources:
            return KoreanCommandSubmissionAdmissionDecision(
                allowed=False,
                command_name=command_name,
                source=source,
                reason_code="korean_command_source_not_allowed",
                message=(
                    "This input source is not admitted for Korean command submission."
                ),
                expose_admission_reason=True,
            )
        return KoreanCommandSubmissionAdmissionDecision(
            allowed=True,
            command_name=command_name,
            source=source,
        )

    def _not_public(
        self,
        command_name: str,
        source: str,
    ) -> KoreanCommandSubmissionAdmissionDecision:
        return KoreanCommandSubmissionAdmissionDecision(
            allowed=False,
            command_name=command_name,
            source=source,
            reason_code="korean_command_not_public",
            message=self._NOT_PUBLIC_MESSAGE,
        )


__all__ = ("KoreanPublicCommandSubmissionPolicy",)
