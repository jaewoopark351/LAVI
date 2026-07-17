#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

from ._starcraft2_bot_engine_section import _StarCraft2BotEngineSection
from ._starcraft2_local_match_section import _StarCraft2LocalMatchSection

__all__ = [
    '_StarCraft2BotEngineSection',
    '_StarCraft2LocalMatchSection',
]
