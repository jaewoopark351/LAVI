#20260725_kpopmodder: Added parser chain so parser ordering is not embedded in MinecraftCommandParser.
from __future__ import annotations

from typing import Dict, Iterable

from .minecraft_equip_command_parser import MinecraftEquipCommandParser
from .minecraft_get_item_command_parser import MinecraftGetItemCommandParser
from .minecraft_goto_command_parser import MinecraftGotoCommandParser
from .minecraft_simple_command_parser import MinecraftSimpleCommandParser


class MinecraftParserChain:
    def __init__(self, parsers: Iterable[object] | None = None):
        self.parsers = tuple(
            parsers
            or (
                MinecraftSimpleCommandParser(),
                MinecraftGotoCommandParser(),
                MinecraftEquipCommandParser(),
                MinecraftGetItemCommandParser(),
            )
        )

    def parse(self, text: str, lowered: str) -> Dict[str, object] | None:
        for parser in self.parsers:
            result = parser.parse(text, lowered)
            if result is not None:
                return result
        return None
