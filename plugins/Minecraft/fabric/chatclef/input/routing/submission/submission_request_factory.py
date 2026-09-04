#20260818_kpopmodder: Build one router-owned Fabric ChatClef submission request.
from __future__ import annotations

import uuid

from input_core.input_event.contracts import LaviInputEvent


class MinecraftChatClefSubmissionRequestFactory:
    def build(
        self,
        event: LaviInputEvent,
        *,
        original_text: str | None = None,
        translation_input_text: str | None = None,
    ) -> dict[str, object]:
        raw_text = event.text if original_text is None else original_text
        translated_text = (
            raw_text
            if translation_input_text is None
            else translation_input_text
        )
        return {
            "request_id": f"lavi-input-ko-{uuid.uuid4().hex}",
            "text": raw_text,
            "source": event.source,
            "metadata": {
                "input_route": "minecraft_fabric_chatclef",
                "language": "ko",
                "input_event": {
                    "source": event.source,
                    "provider_id": event.provider_id,
                    "event_kind": event.event_kind,
                    "final": event.final,
                    "event_id": event.event_id,
                },
                "natural_language_input": {
                    "translation_input_text": translated_text,
                },
            },
        }
