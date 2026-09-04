#20260827_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

import uuid
from typing import Any, Mapping

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.fabric.chatclef.intent import ChatClefTranslationResultDTO


class TranslatedCommandRequestFactory:
    def build(
        self,
        command: Any,
        translation: ChatClefTranslationResultDTO,
        original_text: str,
    ) -> CommandRequestDTO:
        payload = dict(command) if isinstance(command, Mapping) else {}
        metadata = dict(payload.get("metadata") or {})
        route_input = metadata.pop("natural_language_input", {})
        if not isinstance(route_input, Mapping):
            route_input = {}
        translation_input_text = str(
            route_input.get("translation_input_text") or original_text
        )
        input_event = metadata.get("input_event")
        if isinstance(input_event, Mapping):
            metadata["input_event"] = {
                key: input_event.get(key)
                for key in (
                    "source",
                    "provider_id",
                    "event_kind",
                    "final",
                    "event_id",
                )
            }
        else:
            metadata.pop("input_event", None)
        metadata["natural_language"] = {
            "language": "ko",
            "original_text": original_text,
            "translation_input_text": translation_input_text,
            "translation": translation.to_dict(),
        }
        return CommandRequestDTO(
            request_id=payload.get("request_id", f"lavi-ko-{uuid.uuid4().hex}"),
            command=str(translation.command or ""),
            source=payload.get("source", "lavi_korean_intent"),
            deadline_ms=payload.get("deadline_ms"),
            metadata=metadata,
        )
