#20260907_kpopmodder: Format bounded command-result diagnostic messages.
from __future__ import annotations

import json
from typing import Any


class FabricChatClefCommandResultDiagnosticFormatter:
    def malformed(self, error: Exception) -> str:
        return (
            "ignored malformed command result "
            f"error={type(error).__name__}: {error}"
        )

    def rejected(self, *, envelope: Any, result: Any, outcome: Any) -> str:
        return (
            "ignored command result "
            f"request={result.request_id} "
            f"status={result.status.value} "
            f"reason={outcome.reason} "
            f"session={envelope.session_id} "
            f"correlation={envelope.correlation_id} "
            f"before={self._compact_json(outcome.before_snapshot)} "
            f"after={self._compact_json(outcome.after_snapshot)} "
            f"data={self._compact_json(result.data)}"
            f" audit={self._compact_json(outcome.audit)}"
        )

    def accepted(self, *, result: Any, outcome: Any) -> str:
        return (
            "command result "
            f"request={result.request_id} status={result.status.value} ok={result.ok} "
            f"error_code={result.error_code} "
            f"message={result.message} "
            f"before={self._compact_json(outcome.before_snapshot)} "
            f"after={self._compact_json(outcome.after_snapshot)} "
            f"data={self._compact_json(result.data)}"
            f" audit={self._compact_json(outcome.audit)}"
        )

    @staticmethod
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
