#20260717_kpopmodder: Isolates extension composition result state from registration logic.
from dataclasses import dataclass, field
from typing import Any, List


@dataclass
class GameExtensionCompositionResult:
    starcraft116_game_extension: Any = None
    starcraft2_game_extension: Any = None
    starcraft2_changeling_observer_extension: Any = None
    minecraft_game_extension: Any = None
    chess_game_extension: Any = None
    registered_extensions: List[Any] = field(default_factory=list)
    errors: List[str] = field(default_factory=list)
