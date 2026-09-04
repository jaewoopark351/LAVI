#20260717_kpopmodder: Keeps AppComposer focused on assembly order while this service owns component wiring rules.
from app_core.composition_core.component_wiring import (
    ComponentEventListenerWiring,
    MinecraftInputRouterWiring,
    OptionalPluginCallbackWiring,
)
from app_core.composition_core.managed_components import (
    ManagedComponentCollectionService,
)


class AppComponentWiringService:
    def __init__(
        self,
        *,
        optional_plugin_callback_wiring=None,
        minecraft_input_router_wiring=None,
        component_event_listener_wiring=None,
        managed_component_collection_service=None,
    ):
        #20260905_kpopmodder: Keep the compatibility facade while collaborators own separate change reasons.
        self._optional_plugin_callback_wiring = (
            optional_plugin_callback_wiring or OptionalPluginCallbackWiring()
        )
        self._minecraft_input_router_wiring = (
            minecraft_input_router_wiring or MinecraftInputRouterWiring()
        )
        self._component_event_listener_wiring = (
            component_event_listener_wiring
            or ComponentEventListenerWiring(self._minecraft_input_router_wiring)
        )
        self._managed_component_collection_service = (
            managed_component_collection_service
            or ManagedComponentCollectionService()
        )

    #20260717_kpopmodder: Cross-component callback wiring belongs here, not in AppComposer startup flow.
    def wire_optional_plugin_callbacks(self, *, starcraft_plugin=None, screen_vision=None):
        return self._optional_plugin_callback_wiring.wire(
            starcraft_plugin=starcraft_plugin,
            screen_vision=screen_vision,
        )

    def wire_event_listeners(
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
        return self._component_event_listener_wiring.wire(
            input_component=input_component,
            llm=llm,
            translate=translate,
            tts=tts,
            vtuber=vtuber,
            song_player=song_player,
            starcraft_plugin=starcraft_plugin,
            screen_vision=screen_vision,
            screen_vision_input_callback=screen_vision_input_callback,
            minecraft_fabric_chatclef_extension=(
                minecraft_fabric_chatclef_extension
            ),
        )

    def build_managed_components(
        self,
        *,
        input_component,
        llm,
        translate,
        tts,
        vtuber,
        screen_vision=None,
        song_player=None,
        starcraft_plugin=None,
        game_extension_registry=None,
        optional_components=(),
    ):
        return self._managed_component_collection_service.build(
            input_component=input_component,
            llm=llm,
            translate=translate,
            tts=tts,
            vtuber=vtuber,
            screen_vision=screen_vision,
            song_player=song_player,
            starcraft_plugin=starcraft_plugin,
            game_extension_registry=game_extension_registry,
            optional_components=optional_components,
        )
