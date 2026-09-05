#20260905_kpopmodder: Project one ordinary-command diagnostic event into its payload.
from __future__ import annotations


class FabricChatClefCommandSubmissionDiagnosticProjector:
    @staticmethod
    def project(*, event: str, request, details) -> dict:
        return {
            "event": event,
            "request_id": request.request_id,
            "source": request.source,
            "command": request.command,
            "deadline_ms": request.deadline_ms,
            "metadata": request.metadata,
            "details": details,
        }
