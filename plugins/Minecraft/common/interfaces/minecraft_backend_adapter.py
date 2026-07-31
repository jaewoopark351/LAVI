#20260801_kpopmodder: Define the minimal backend adapter contract without lifecycle implementation.
from __future__ import annotations

from typing import Protocol

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.dto.status_snapshot_dto import StatusSnapshotDTO


class MinecraftBackendAdapter(Protocol):
    @property
    def backend_id(self) -> str:
        ...

    def start(self) -> None:
        ...

    def stop(self) -> None:
        ...

    def submit_command(self, request: CommandRequestDTO) -> CommandResultDTO:
        ...

    def get_status(self) -> StatusSnapshotDTO:
        ...
