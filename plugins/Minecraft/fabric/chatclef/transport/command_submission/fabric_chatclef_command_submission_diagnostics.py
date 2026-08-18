#20260818_kpopmodder: Render bounded Fabric ChatClef command-submission diagnostics.
from __future__ import annotations

import json
from typing import Any


class FabricChatClefCommandSubmissionDiagnostics:
    def __init__(self, diagnostics: Any):
        self._diagnostics = diagnostics

    def log(self, event: str, request: Any, details: dict[str, Any]) -> None:
        payload = {
            "event": event,
            "request_id": request.request_id,
            "source": request.source,
            "command": request.command,
            "deadline_ms": request.deadline_ms,
            "metadata": request.metadata,
            "details": details,
        }
        self._diagnostics.info("command gate " + _compact_json(payload))


def _compact_json(payload: Any) -> str:
    try:
        return json.dumps(
            payload,
            ensure_ascii=False,
            sort_keys=True,
            separators=(",", ":"),
        )
    except Exception as error:
        return f"<json failed {type(error).__name__}: {error}>"
