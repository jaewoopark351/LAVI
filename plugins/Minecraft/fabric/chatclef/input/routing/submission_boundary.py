#20260818_kpopmodder: Submit one validated prefixless command through the non-retranslating extension API.
from __future__ import annotations

import uuid
from typing import Any, Mapping


class MinecraftChatClefSubmissionBoundary:
    def is_available(self, extension: Any) -> bool:
        return callable(getattr(extension, "submit_translated_command", None))

    def submit_once(
        self,
        extension: Any,
        text: str,
        translation: Mapping[str, Any],
    ) -> dict[str, Any]:
        submitter = getattr(extension, "submit_translated_command")
        result = submitter(self._request(text), dict(translation))
        return self._mapping_payload(result)

    def _request(self, text: str) -> dict[str, Any]:
        return {
            "request_id": f"lavi-input-ko-{uuid.uuid4().hex}",
            "text": text,
            "source": "lavi_chat_mic_router",
            "metadata": {
                "input_route": "minecraft_fabric_chatclef",
                "language": "ko",
            },
        }

    def _mapping_payload(self, payload: Any) -> dict[str, Any]:
        if isinstance(payload, Mapping):
            return dict(payload)
        to_dict = getattr(payload, "to_dict", None)
        if callable(to_dict):
            mapped = to_dict()
            if isinstance(mapped, Mapping):
                return dict(mapped)
        return {"raw": payload}
