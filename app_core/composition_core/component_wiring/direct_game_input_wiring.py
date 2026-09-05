#20260905_kpopmodder: Preserve the direct game-input composition facade.
from .direct_game_input import DirectGameInputComponentGraph


class DirectGameInputWiring:
    def __init__(self):
        self._components = DirectGameInputComponentGraph()

    def wire_starcraft(self, *, llm, starcraft_plugin) -> None:
        self._components.starcraft_wiring.wire(
            llm=llm,
            starcraft_plugin=starcraft_plugin,
        )

    def wire_screen(
        self,
        *,
        screen_vision,
        screen_vision_input_callback,
    ) -> None:
        self._components.screen_vision_wiring.wire(
            screen_vision=screen_vision,
            screen_vision_input_callback=screen_vision_input_callback,
        )


__all__ = ("DirectGameInputWiring",)
