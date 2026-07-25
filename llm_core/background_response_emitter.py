#20260725_kpopmodder: Added emitter for delayed non-LLM responses such as Minecraft action completion.
from __future__ import annotations

from typing import Callable

from core.logger import log_print


class LLMBackgroundResponseEmitter:
    def emit(
        self,
        text: str,
        *,
        response_generation_callback: Callable[[], int],
        build_stream_payload_callback: Callable[[str, int], object],
        send_output_callback: Callable[[object], object],
        send_full_output_callback: Callable[[str], object],
        live_textbox,
    ) -> bool:
        clean_text = str(text or "").strip()
        if not clean_text:
            return False

        response_generation = response_generation_callback()
        payload = build_stream_payload_callback(clean_text, response_generation)
        log_print(f"[LLM background response] {clean_text}")
        live_textbox.print("AI: ")
        live_textbox.print(clean_text, append_to_last=True)
        send_output_callback(payload)
        send_full_output_callback(clean_text)
        return True
