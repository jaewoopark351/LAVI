#20260905_kpopmodder: Render only bounded ordinary-command submission diagnostics.
from __future__ import annotations

from typing import Any

from .fabric_chatclef_command_submission_diagnostic_encoder import (
    FabricChatClefCommandSubmissionDiagnosticEncoder,
)
from .fabric_chatclef_command_submission_diagnostic_projector import (
    FabricChatClefCommandSubmissionDiagnosticProjector,
)


class FabricChatClefCommandSubmissionDiagnostics:
    def __init__(self, diagnostics: Any) -> None:
        self._diagnostics = diagnostics
        self._projector = FabricChatClefCommandSubmissionDiagnosticProjector()
        self._encoder = FabricChatClefCommandSubmissionDiagnosticEncoder()

    def log(self, event: str, request: Any, details: dict[str, Any]) -> None:
        payload = self._projector.project(
            event=event,
            request=request,
            details=details,
        )
        self._diagnostics.info("command gate " + self._encoder.encode(payload))
