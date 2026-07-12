#20260708_kpopmodder: Added thin TTS bridge for SC2 log commentary without GPT-SoVITS rewiring.
from __future__ import annotations

from collections import deque
from typing import Any, Callable, Dict, Optional

from core.logger import log_print


SpeakCallback = Callable[[str], Any]


class SC2TTSBridge:
    """Small adapter that hands parsed SC2 commentary to an injected TTS path."""

    def __init__(
        self,
        tts: Any = None,
        speak_callback: Optional[SpeakCallback] = None,
        enabled: bool = True,
        tail_size: int = 20,
    ):
        self.tts = tts
        self.speak_callback = speak_callback
        self.enabled = bool(enabled)
        self.last_error = ""
        self.spoken_tail = deque(maxlen=max(1, int(tail_size)))

    def set_tts(self, tts: Any) -> None:
        self.tts = tts

    def set_speak_callback(self, callback: Optional[SpeakCallback]) -> None:
        self.speak_callback = callback

    def speak(self, text: str) -> Dict[str, Any]:
        message = str(text or "").strip()
        if not message:
            return {"ok": False, "skipped": True, "reason": "empty_text"}
        if not self.enabled:
            return {"ok": True, "skipped": True, "reason": "speak_events_disabled"}

        try:
            if callable(self.speak_callback):
                self.speak_callback(message)
                self._remember(message)
                return {"ok": True, "method": "callback"}

            method_result = self._call_known_tts_method(message)
            if method_result.get("ok"):
                self._remember(message)
                return method_result

            queue_result = self._try_input_queue(message)
            if queue_result.get("ok"):
                self._remember(message)
                return queue_result
        except Exception as e:
            self.last_error = str(e)
            log_print(f"[SC2TTSBridge] speak failed: {e}")
            return {"ok": False, "error": str(e)}

        # TODO: Bind this to the final LAV TTS queue entry point once that public
        # method is made explicit. Avoid direct GPT-SoVITS rewiring here.
        self.last_error = "tts_binding_missing"
        log_print(f"[SC2TTSBridge] TTS binding missing. Pending text: {message}")
        return {"ok": False, "error": "tts_binding_missing", "text": message}

    def cancel_pending(self, reason: str = "starcraft2_game_ended") -> Dict[str, Any]:
        #20260711_kpopmodder: Stop stale SC2 commentary through the narrowest
        # available TTS API instead of interrupting the entire application.
        target = self.tts
        if target is None:
            return {"ok": True, "skipped": True, "reason": "tts_binding_missing"}

        cancel_pending = getattr(target, "cancel_pending", None)
        try:
            if callable(cancel_pending):
                cancel_pending(reason=reason)
                return {"ok": True, "method": "cancel_pending"}

            handle_interrupt = getattr(target, "handle_interrupt", None)
            if callable(handle_interrupt):
                handle_interrupt()
                return {"ok": True, "method": "handle_interrupt"}
        except Exception as e:
            self.last_error = str(e)
            log_print(f"[SC2TTSBridge] cancel pending failed: {e}")
            return {"ok": False, "error": str(e)}

        return {"ok": True, "skipped": True, "reason": "cancel_binding_missing"}

    def get_status(self) -> Dict[str, Any]:
        return {
            "enabled": self.enabled,
            "has_tts": self.tts is not None,
            "has_callback": callable(self.speak_callback),
            "last_error": self.last_error,
            "spoken_tail": list(self.spoken_tail),
        }

    def _call_known_tts_method(self, message: str) -> Dict[str, Any]:
        target = self.tts
        if target is None:
            return {"ok": False}
        for method_name in (
            "speak_text",
            "receive_input",
            "speak",
            "say",
            "enqueue_text",
            "queue_text",
            "add_text",
        ):
            method = getattr(target, method_name, None)
            if callable(method):
                method(message)
                return {"ok": True, "method": method_name}
        return {"ok": False}

    def _try_input_queue(self, message: str) -> Dict[str, Any]:
        target = self.tts
        input_queue = getattr(target, "input_queue", None)
        put = getattr(input_queue, "put", None)
        if not callable(put):
            return {"ok": False}
        put(message)
        return {"ok": True, "method": "input_queue.put"}

    def _remember(self, message: str) -> None:
        self.last_error = ""
        self.spoken_tail.append(message)
