#20260827_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

import uuid
from typing import Any, Mapping

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO


class FabricChatClefCommandRequestFactory:
    def build(self, command: Any) -> CommandRequestDTO:
        if isinstance(command, CommandRequestDTO):
            return command
        if isinstance(command, str):
            return CommandRequestDTO(
                request_id=f"lavi-command-{uuid.uuid4().hex}",
                command=command,
                source="lavi",
                metadata={},
            )
        if isinstance(command, Mapping):
            payload = dict(command)
            if "command" not in payload and "action" in payload:
                payload["command"] = payload["action"]
            payload.setdefault("request_id", f"lavi-command-{uuid.uuid4().hex}")
            payload.setdefault("source", "lavi")
            payload.setdefault("metadata", {})
            return CommandRequestDTO.from_mapping(payload)
        return CommandRequestDTO(
            request_id=f"lavi-command-{uuid.uuid4().hex}",
            command="",
            source="lavi",
            metadata={"raw_type": command.__class__.__name__},
        )
