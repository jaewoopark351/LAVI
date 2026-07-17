#20260717_kpopmodder: Groups global state key and store classes behind the legacy facade.

from .global_keys import GlobalKeys
from .global_state import GlobalState

__all__ = [
    "GlobalKeys",
    "GlobalState",
]
