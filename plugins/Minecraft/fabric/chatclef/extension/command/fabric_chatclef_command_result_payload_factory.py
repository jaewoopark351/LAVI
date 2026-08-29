#20260827_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO


class FabricChatClefCommandResultPayloadFactory:
    def build(self, result: CommandResultDTO) -> dict[str, Any]:
        return {
            "ok": result.ok,
            "status": result.to_dict(),
            "error": None if result.error_code is None else result.error_code.value,
            "message": result.message,
            "details": result.data,
        }
