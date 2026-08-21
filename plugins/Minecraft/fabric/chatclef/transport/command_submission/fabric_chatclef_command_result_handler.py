#20260818_kpopmodder: Own matching Fabric ChatClef command-result acceptance.
from __future__ import annotations

import json
from typing import Any, Mapping

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO


class FabricChatClefCommandResultHandler:
    def __init__(self, *, connection_ownership, command_lock, diagnostics):
        self._connection_ownership = connection_ownership
        self._command_lock = command_lock
        self._diagnostics = diagnostics

    def handle(self, websocket: Any, envelope: Any) -> None:
        raw_payload = (
            dict(envelope.payload) if isinstance(envelope.payload, Mapping) else {}
        )
        try:
            result = CommandResultDTO.from_mapping(raw_payload)
        except Exception as error:
            self._diagnostics.warning(
                "ignored malformed command result "
                f"error={type(error).__name__}: {error}"
            )
            return
        with self._command_lock:
            outcome = self._connection_ownership.accept_result_and_reconcile(
                websocket=websocket,
                envelope=envelope,
                result=result,
                raw_payload=raw_payload,
            )
        if not outcome.accepted:
            self._diagnostics.warning(
                "ignored command result "
                f"request={result.request_id} "
                f"status={result.status.value} "
                f"reason={outcome.reason} "
                f"session={envelope.session_id} "
                f"correlation={envelope.correlation_id} "
                f"before={_compact_json(outcome.before_snapshot)} "
                f"after={_compact_json(outcome.after_snapshot)} "
                f"data={_compact_json(result.data)}"
                f" audit={_compact_json(outcome.audit)}"
            )
            return
        self._diagnostics.info(
            "command result "
            f"request={result.request_id} status={result.status.value} ok={result.ok} "
            f"error_code={result.error_code} "
            f"message={result.message} "
            f"before={_compact_json(outcome.before_snapshot)} "
            f"after={_compact_json(outcome.after_snapshot)} "
            f"data={_compact_json(result.data)}"
            f" audit={_compact_json(outcome.audit)}"
        )


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
