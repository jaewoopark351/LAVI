#20260905_kpopmodder: Coordinate event-listener topology through responsibility-specific collaborators.
from .direct_game_input_wiring import DirectGameInputWiring
from .minecraft_stop_terminal_response_wiring import (
    MinecraftStopTerminalResponseWiring,
)
from .trusted_voice_input_wiring import TrustedVoiceInputWiring


class ComponentEventListenerWiring:
    def __init__(
        self,
        minecraft_input_router_wiring,
        *,
        direct_game_input_wiring=None,
        trusted_voice_input_wiring=None,
        minecraft_stop_terminal_response_wiring=None,
    ):
        self._minecraft_input_router_wiring = minecraft_input_router_wiring
        self._direct_game_input_wiring = (
            direct_game_input_wiring or DirectGameInputWiring()
        )
        self._trusted_voice_input_wiring = (
            trusted_voice_input_wiring or TrustedVoiceInputWiring()
        )
        self._minecraft_stop_terminal_response_wiring = (
            minecraft_stop_terminal_response_wiring
            or MinecraftStopTerminalResponseWiring()
        )

    def wire(
        self,
        *,
        input_component,
        llm,
        translate,
        tts,
        vtuber,
        song_player=None,
        starcraft_plugin=None,
        screen_vision=None,
        screen_vision_input_callback=None,
        minecraft_fabric_chatclef_extension=None,
    ):
        self._minecraft_input_router_wiring.wire(
            llm=llm,
            extension=minecraft_fabric_chatclef_extension,
        )
        input_component.add_output_event_listener(llm.receive_input)
        self._trusted_voice_input_wiring.wire(
            input_component=input_component,
            llm=llm,
        )
        self._minecraft_stop_terminal_response_wiring.wire(
            llm=llm,
            extension=minecraft_fabric_chatclef_extension,
        )
        llm.add_output_event_listener(translate.receive_input)
        translate.add_output_event_listener(tts.receive_input)
        tts.add_output_event_listener(vtuber.receive_input)
        if song_player is not None:
            song_player.add_output_event_listener(vtuber.receive_input)
            song_player.add_expression_event_listener(
                vtuber.receive_song_expression
            )
        self._direct_game_input_wiring.wire_starcraft(
            llm=llm,
            starcraft_plugin=starcraft_plugin,
        )
        self._direct_game_input_wiring.wire_screen(
            screen_vision=screen_vision,
            screen_vision_input_callback=screen_vision_input_callback,
        )


__all__ = ("ComponentEventListenerWiring",)
