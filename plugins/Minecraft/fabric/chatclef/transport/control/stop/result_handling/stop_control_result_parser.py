#20260905_kpopmodder: Isolate STOP wire payload parsing from semantic validation.
from __future__ import annotations

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO


class StopControlResultParser:
    def parse(
        self,
        payload: dict,
    ) -> tuple[CommandResultDTO | None, Exception | None]:
        try:
            return CommandResultDTO.from_mapping(payload), None
        except Exception as error:
            return None, error


__all__ = ("StopControlResultParser",)
