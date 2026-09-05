#20260905_kpopmodder: Normalize only one Fabric ChatClef ordinary-command request identity.
from __future__ import annotations

import uuid

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO


def normalize_command_request(request: CommandRequestDTO) -> CommandRequestDTO:
    command_request = CommandRequestDTO.from_mapping(request)
    if command_request.request_id:
        return command_request
    return CommandRequestDTO(
        request_id=f"lavi-command-{uuid.uuid4().hex}",
        command=command_request.command,
        source=command_request.source,
        deadline_ms=command_request.deadline_ms,
        metadata=command_request.metadata,
    )
