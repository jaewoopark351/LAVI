#20260819_kpopmodder: Convert validated fields into one canonical result value.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)

from .canonical_submission_result import CanonicalSubmissionResult


class CanonicalSubmissionResultFactory:
    def create(
        self,
        *,
        request_id: str,
        ok: bool,
        status: CommandResultStatus,
        error_code: str | None,
        message: str,
        data: Mapping[str, Any],
    ) -> CanonicalSubmissionResult:
        return CanonicalSubmissionResult(
            request_id=request_id,
            ok=ok,
            status=status,
            error_code=error_code,
            message=message,
            data=data,
        )
