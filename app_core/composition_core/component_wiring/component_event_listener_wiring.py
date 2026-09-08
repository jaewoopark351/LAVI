#20260905_kpopmodder: Coordinate event-listener topology through responsibility-specific collaborators.
#20260908_kpopmodder: Keep STOP terminal delivery fully wired before trusted Minecraft input is exposed.
from .app_component_wiring_error import AppComponentWiringError
from .direct_game_input_wiring import DirectGameInputWiring
from .minecraft_stop_terminal_response_wiring import (
    MinecraftStopTerminalResponseWiring,
)
#20260907_kpopmodder: Compose crafting terminal output independently from STOP.
from .minecraft_crafting_terminal_response_wiring import (
    MinecraftCraftingTerminalResponseWiring,
)
from .minecraft_command_lifecycle_terminal_response_wiring import (
    MinecraftCommandLifecycleTerminalResponseWiring,
)
from .minecraft_command_lifecycle_start_response_wiring import (
    MinecraftCommandLifecycleStartResponseWiring,
)
from .minecraft_lifecycle_tts_receipt_wiring import (
    MinecraftLifecycleTtsReceiptWiring,
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
        minecraft_crafting_terminal_response_wiring=None,
        minecraft_command_lifecycle_start_response_wiring=None,
        minecraft_command_lifecycle_terminal_response_wiring=None,
        minecraft_lifecycle_tts_receipt_wiring=None,
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
        self._minecraft_crafting_terminal_response_wiring = (
            minecraft_crafting_terminal_response_wiring
            or MinecraftCraftingTerminalResponseWiring()
        )
        self._minecraft_command_lifecycle_terminal_response_wiring = (
            minecraft_command_lifecycle_terminal_response_wiring
            or MinecraftCommandLifecycleTerminalResponseWiring()
        )
        self._minecraft_command_lifecycle_start_response_wiring = (
            minecraft_command_lifecycle_start_response_wiring
            or MinecraftCommandLifecycleStartResponseWiring()
        )
        self._minecraft_lifecycle_tts_receipt_wiring = (
            minecraft_lifecycle_tts_receipt_wiring
            or MinecraftLifecycleTtsReceiptWiring()
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
        stop_terminal_callback = self._minecraft_stop_terminal_response_wiring.wire(
            llm=llm,
            extension=minecraft_fabric_chatclef_extension,
        )
        if (
            minecraft_fabric_chatclef_extension is not None
            and not callable(stop_terminal_callback)
        ):
            raise AppComponentWiringError(
                "minecraft_stop_terminal_response_callback",
                (
                    "Fabric ChatClef extension did not install a callable "
                    "STOP terminal response callback"
                ),
            )
        #20260907_kpopmodder: Bind the one-shot crafting terminal listener to normal output/TTS.
        self._minecraft_crafting_terminal_response_wiring.wire(
            llm=llm,
            extension=minecraft_fabric_chatclef_extension,
        )
        self._minecraft_command_lifecycle_start_response_wiring.wire(
            llm=llm,
            extension=minecraft_fabric_chatclef_extension,
        )
        self._minecraft_command_lifecycle_terminal_response_wiring.wire(
            llm=llm,
            extension=minecraft_fabric_chatclef_extension,
        )
        llm.add_output_event_listener(translate.receive_input)
        translate.add_output_event_listener(tts.receive_input)
        self._minecraft_lifecycle_tts_receipt_wiring.wire(
            llm=llm,
            tts=tts,
        )
        tts.add_output_event_listener(vtuber.receive_input)
        self._minecraft_input_router_wiring.wire(
            llm=llm,
            extension=minecraft_fabric_chatclef_extension,
        )
        input_component.add_output_event_listener(llm.receive_input)
        self._trusted_voice_input_wiring.wire(
            input_component=input_component,
            llm=llm,
        )
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
