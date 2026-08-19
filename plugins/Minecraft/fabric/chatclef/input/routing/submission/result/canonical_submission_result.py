#20260819_kpopmodder: Hold one validated submit result before canonical mapping creation.
from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Mapping

from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)


@dataclass(frozen=True)
class CanonicalSubmissionResult:
    request_id: str
    ok: bool
    status: CommandResultStatus
    error_code: str | None
    message: str
    data: Mapping[str, Any]
