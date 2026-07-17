#20260717_kpopmodder: Split adapter helper from engine interface facade for AGENTS 29.1.
from __future__ import annotations

from typing import Any

from .legacy_starcraft2_engine_adapter import LegacyStarCraft2EngineAdapter
from .starcraft2_engine_interface_base import StarCraft2EngineInterface


def adapt_starcraft2_engine(engine: Any) -> StarCraft2EngineInterface:
    if bool(getattr(engine, "uses_engine_dto_contract", False)):
        return engine
    return LegacyStarCraft2EngineAdapter(engine)
