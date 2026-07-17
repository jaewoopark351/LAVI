#20260717_kpopmodder: Keeps the game event callback alias reusable across bus modules.
from typing import Any, Callable, Dict


GameEventCallback = Callable[[Dict[str, Any]], None]
