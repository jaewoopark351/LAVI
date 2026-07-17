#20260717_kpopmodder: Compatibility facade for LC0 UCI engine classes.
from .lc0_engine_error import LC0EngineError
from .lc0_uci_engine_impl import LC0UCIEngine

__all__ = [
    "LC0EngineError",
    "LC0UCIEngine",
]
