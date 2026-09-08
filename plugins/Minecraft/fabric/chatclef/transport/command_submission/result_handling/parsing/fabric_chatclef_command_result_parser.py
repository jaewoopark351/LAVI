#20260907_kpopmodder: Parse command-result wire payloads without owning diagnostics.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO


class FabricChatClefCommandResultParser:
    @staticmethod
    def extract_payload(envelope: Any) -> dict[str, Any]:
        return (
            dict(envelope.payload)
            if isinstance(envelope.payload, Mapping)
            else {}
        )

    @staticmethod
    def parse_payload(raw_payload: Mapping[str, Any]) -> CommandResultDTO:
        return CommandResultDTO.from_mapping(raw_payload)
