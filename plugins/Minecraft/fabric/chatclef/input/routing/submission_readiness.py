#20260818_kpopmodder: Represent one fail-closed Fabric ChatClef submission-readiness result.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Mapping


@dataclass(frozen=True)
class MinecraftChatClefSubmissionReadiness:
    ready: bool
    reason: str
    error: str = ""
    message: str = ""
    status: dict[str, Any] = field(default_factory=dict)

    @classmethod
    def accepted(
        cls,
        status: Mapping[str, Any],
    ) -> "MinecraftChatClefSubmissionReadiness":
        return cls(
            ready=True,
            reason="minecraft_submission_ready",
            status=dict(status),
        )

    @classmethod
    def rejected(
        cls,
        *,
        reason: str,
        error: str,
        message: str,
        status: Mapping[str, Any] | None = None,
    ) -> "MinecraftChatClefSubmissionReadiness":
        return cls(
            ready=False,
            reason=str(reason or "minecraft_bridge_status_unavailable"),
            error=str(error or "status_unavailable"),
            message=str(message or "Fabric ChatClef status is unavailable."),
            status=dict(status or {}),
        )
