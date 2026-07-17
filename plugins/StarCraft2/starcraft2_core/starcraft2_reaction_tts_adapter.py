#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260713_kpopmodder: Split StarCraft2 reaction side effects out of the runtime coordinator.
from __future__ import annotations

import json
from typing import Any, Dict

from core.logger import log_print
from .starcraft2_contracts import StarCraft2Event


#20260713_kpopmodder: Keep raw-event persistence separate from reaction policy and TTS flow.

class StarCraft2ReactionTTSAdapter:
    def __init__(self, tts=None, terminal_cancel_reason: str | None = None):
        self.tts = tts
        self.terminal_cancel_reason = terminal_cancel_reason

    def cancel_pending(
        self,
        event_type: str,
        event_details: Dict[str, Any] | None = None,
    ) -> None:
        tts = self.tts
        if tts is None:
            return

        event_details = event_details if isinstance(event_details, dict) else {}
        cancel_pending = getattr(tts, "cancel_pending", None)
        try:
            if callable(cancel_pending):
                reason = (
                    str(event_details.get("terminal_cancel_reason") or "").strip()
                    or self.terminal_cancel_reason
                    or f"starcraft2_{event_type}"
                )
                cancel_pending(reason=reason)
                return

            handle_interrupt = getattr(tts, "handle_interrupt", None)
            if callable(handle_interrupt):
                handle_interrupt()
        except Exception as exc:
            log_print(
                "[StarCraft2ReactionTTSAdapter] pending TTS cancellation failed: "
                f"event={event_type} error={exc}"
            )

    def speak(self, text: str) -> bool:
        message = str(text or "").strip()
        if not message or self.tts is None:
            return False

        receive_input = getattr(self.tts, "receive_input", None)
        if callable(receive_input):
            receive_input(message)
            return True

        speak = getattr(self.tts, "speak", None)
        if callable(speak):
            try:
                result = speak(message)
                if isinstance(result, dict):
                    return bool(result.get("ok", True))
            except Exception as exc:
                log_print(f"[StarCraft2ReactionTTSAdapter] tts speak failed: {exc}")
                return False
            return True

        return False
