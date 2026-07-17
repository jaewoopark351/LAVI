#20260717_kpopmodder: Isolates optional plugin composition output from the service.
from dataclasses import dataclass
from typing import Any, Tuple


@dataclass(frozen=True)
class OptionalPluginCompositionResult:
    #20260717_kpopmodder: AppComposer consumes this result without knowing manifest details.
    song_player: Any = None
    chess_plugin: Any = None
    starcraft_plugin: Any = None
    starcraft116_plugin: Any = None
    starcraft2_plugin: Any = None
    screen_vision: Any = None
    optional_components: Tuple[Any, ...] = ()
    startup_components: Tuple[Any, ...] = ()

    def attribute_map(self):
        return {
            "song_player": self.song_player,
            "chess_plugin": self.chess_plugin,
            "starcraft_plugin": self.starcraft_plugin,
            "starcraft116_plugin": self.starcraft116_plugin,
            "starcraft2_plugin": self.starcraft2_plugin,
            "screen_vision": self.screen_vision,
        }
