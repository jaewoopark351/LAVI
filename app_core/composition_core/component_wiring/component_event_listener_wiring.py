#20260905_kpopmodder: Owns event-listener topology and source-bound direct input adapters.
from input_core.input_event.adapters import DirectCallbackInputEventAdapter
from input_core.input_event.provenance import SCREEN_VISION, STARCRAFT_REMASTERED


class ComponentEventListenerWiring:
    def __init__(self, minecraft_input_router_wiring):
        self._minecraft_input_router_wiring = minecraft_input_router_wiring
        self._direct_input_adapters = {}

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
        llm.add_output_event_listener(translate.receive_input)
        translate.add_output_event_listener(tts.receive_input)
        tts.add_output_event_listener(vtuber.receive_input)
        if song_player is not None:
            song_player.add_output_event_listener(vtuber.receive_input)
            song_player.add_expression_event_listener(
                vtuber.receive_song_expression
            )
        if starcraft_plugin is not None:
            starcraft_input_callback = self._direct_adapter(
                source=STARCRAFT_REMASTERED,
                provider_id="StarCraftRemastered",
                event_kind="starcraft_output",
                output_callback=llm.receive_input,
            )
            starcraft_plugin.add_output_event_listener(starcraft_input_callback)
            llm.add_output_event_listener(
                starcraft_plugin.receive_coach_response,
                full_response=True,
            )

        if screen_vision is not None and screen_vision_input_callback is not None:
            screen_vision_callback = self._direct_adapter(
                source=SCREEN_VISION,
                provider_id="ScreenVision",
                event_kind="screen_observation",
                output_callback=screen_vision_input_callback,
            )
            screen_vision.add_output_event_listener(screen_vision_callback)

    def _direct_adapter(
        self,
        *,
        source,
        provider_id,
        event_kind,
        output_callback,
    ):
        callback_owner = getattr(output_callback, "__self__", None)
        callback_function = getattr(output_callback, "__func__", output_callback)
        key = (source, id(callback_owner), callback_function)
        adapter = self._direct_input_adapters.get(key)
        if adapter is None:
            adapter = DirectCallbackInputEventAdapter(
                output_callback=output_callback,
                source=source,
                provider_id=provider_id,
                event_kind=event_kind,
                final=True,
            )
            self._direct_input_adapters[key] = adapter
        return adapter


__all__ = ["ComponentEventListenerWiring"]
