#20260913_kpopmodder: Observe two finite input boundaries without logging raw conversation.
from __future__ import annotations

import re
from typing import Callable

from plugins.Minecraft.fabric.chatclef.intent.navigation.goto import GotoParseResult


class GotoInputDiagnostics:
    def __init__(self, log_callback: Callable[[str], None] | None = None):
        self._log_callback = log_callback

    def observe(
        self,
        event: object,
        *,
        boundary: str,
        parsed: GotoParseResult | None,
        reason: str,
    ) -> None:
        # One input guard event and at most one binding event per routed input;
        # no polling, retries, raw text, microphone data, or behavior counters.
        if self._log_callback is None:
            return
        try:
            fields = (
                "event=korean_goto_input",
                f"boundary={self._token(boundary)}",
                f"source={self._token(getattr(event, 'source', 'unknown'))}",
                f"event_id={self._token(getattr(event, 'event_id', 'unknown'))}",
                f"decision={parsed.decision.value if parsed is not None else 'missing'}",
                f"reason={self._token(reason)}",
            )
            coordinates = ""
            if parsed is not None and parsed.xyz is not None:
                x, y, z = parsed.xyz
                coordinates = f" x={x} y={y} z={z}"
            self._log_callback(" ".join(fields) + coordinates)
        except Exception:
            # Emission failure is diagnostic-only and cannot change admission.
            pass

    @staticmethod
    def _token(value: object) -> str:
        return re.sub(r"[^A-Za-z0-9_.-]", "_", str(value))[:64]
