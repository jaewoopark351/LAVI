#20260905_kpopmodder: Expose only ordinary Fabric ChatClef command submission.
from __future__ import annotations

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO


class FabricChatClefServerCommandApi:
    def __init__(self, *, command_submitter) -> None:
        self._command_submitter = command_submitter

    def submit(self, request: CommandRequestDTO) -> CommandResultDTO:
        return self._command_submitter.submit(request)
