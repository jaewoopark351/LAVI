#20260725_kpopmodder: Added orchestration for focused Minecraft text command parsers.
from __future__ import annotations

from typing import Any, Dict

from .minecraft_craft_command_parser import MinecraftCraftCommandParser
from .minecraft_equip_command_parser import MinecraftEquipCommandParser
from .minecraft_get_and_equip_command_parser import MinecraftGetAndEquipCommandParser
from .minecraft_get_item_command_parser import MinecraftGetItemCommandParser
from .minecraft_goto_command_parser import MinecraftGotoCommandParser
from .minecraft_mapping_command_parser import MinecraftMappingCommandParser
from .minecraft_parser_chain import MinecraftParserChain
from .minecraft_simple_command_parser import MinecraftSimpleCommandParser
from .minecraft_text_parse_helpers import squash_spaces


class MinecraftCommandParser:
    def __init__(
        self,
        simple_parser: MinecraftSimpleCommandParser | None = None,
        goto_parser: MinecraftGotoCommandParser | None = None,
        get_and_equip_parser: MinecraftGetAndEquipCommandParser | None = None,
        craft_parser: MinecraftCraftCommandParser | None = None,
        equip_parser: MinecraftEquipCommandParser | None = None,
        get_item_parser: MinecraftGetItemCommandParser | None = None,
        parser_chain: MinecraftParserChain | None = None,
        mapping_parser: MinecraftMappingCommandParser | None = None,
    ):
        self.parser_chain = parser_chain or MinecraftParserChain(
            (
                simple_parser or MinecraftSimpleCommandParser(),
                goto_parser or MinecraftGotoCommandParser(),
                get_and_equip_parser or MinecraftGetAndEquipCommandParser(),
                craft_parser or MinecraftCraftCommandParser(),
                equip_parser or MinecraftEquipCommandParser(),
                get_item_parser or MinecraftGetItemCommandParser(),
            )
        )
        self.mapping_parser = mapping_parser or MinecraftMappingCommandParser()

    def parse(self, command: Any) -> Dict[str, Any] | None:
        if isinstance(command, dict):
            return self.mapping_parser.parse(command, self._parse_text)
        if not isinstance(command, str):
            return None
        return self._parse_text(command)

    def _parse_text(self, command: str) -> Dict[str, Any] | None:
        text = command.strip()
        if not text:
            return None

        lowered = squash_spaces(text.lower())
        return self.parser_chain.parse(text, lowered)
