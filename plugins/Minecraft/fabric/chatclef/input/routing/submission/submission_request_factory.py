#20260818_kpopmodder: Build one router-owned Fabric ChatClef submission request.
from __future__ import annotations

import uuid


class MinecraftChatClefSubmissionRequestFactory:
    def build(self, text: str) -> dict[str, object]:
        return {
            "request_id": f"lavi-input-ko-{uuid.uuid4().hex}",
            "text": text,
            "source": "lavi_chat_mic_router",
            "metadata": {
                "input_route": "minecraft_fabric_chatclef",
                "language": "ko",
            },
        }
