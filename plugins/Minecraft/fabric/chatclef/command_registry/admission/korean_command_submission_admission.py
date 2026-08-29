#20260827_kpopmodder: Keep Korean command rollout policy outside the extension facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.command_registry.admission.korean_command_submission_admission_decision import (
    KoreanCommandSubmissionAdmissionDecision,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission.store_home_command_admission import (
    StoreHomeCommandAdmission,
)


class KoreanCommandSubmissionAdmission:
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

    def inspect(
        self,
        command_name: str,
        source: str,
        registry: object,
    ) -> KoreanCommandSubmissionAdmissionDecision:
        command = str(command_name or "").strip().lower()
        request_source = str(source or "").strip()
        try:
            spec = registry.spec(command)
        except KeyError:
            return self._not_public(command, request_source)

        store_home = self._store_home_admission.inspect(
            command,
            request_source,
            spec,
        )
        if not store_home.allowed:
            return KoreanCommandSubmissionAdmissionDecision(
                allowed=False,
                command_name=command,
                source=request_source,
                reason_code=store_home.reason_code,
                message=store_home.message,
                expose_admission_reason=True,
            )
        if spec.readiness_axes.public_korean_enabled:
            return KoreanCommandSubmissionAdmissionDecision(
                allowed=True,
                command_name=command,
                source=request_source,
            )
        return self._not_public(command, request_source)

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
