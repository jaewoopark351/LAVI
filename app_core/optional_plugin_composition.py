#20260717_kpopmodder: Optional plugin composition keeps AppComposer focused on assembly order.
from dataclasses import dataclass
from typing import Any, Callable, Tuple

from app_core.optional_module_manifest import get_optional_module_manifest
from app_core.optional_plugin_loader import instantiate_optional_plugin


@dataclass(frozen=True)
class OptionalPluginCompositionContext:
    #20260717_kpopmodder: Typed DTO for optional plugin construction inputs.
    current_module_directory: str
    memory_store: Any = None


@dataclass(frozen=True)
class OptionalPluginSpec:
    #20260717_kpopmodder: Declarative optional plugin assembly rule.
    module_name: str
    attribute_name: str
    lifecycle_component: bool = False
    startup_component: bool = False
    kwargs_factory: Callable[[OptionalPluginCompositionContext], dict] = (
        lambda context: {}
    )


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


def _screen_vision_kwargs(context):
    return {"memory_store": context.memory_store}


class OptionalPluginCompositionService:
    #20260717_kpopmodder: Owns optional direct plugin construction and lifecycle roles.
    DEFAULT_SPECS = (
        OptionalPluginSpec(
            "SongPlayer",
            "song_player",
            lifecycle_component=True,
            startup_component=True,
        ),
        OptionalPluginSpec("Chess", "chess_plugin"),
        OptionalPluginSpec(
            "StarCraftRemastered",
            "starcraft_plugin",
            lifecycle_component=True,
            startup_component=True,
        ),
        OptionalPluginSpec("StarCraft116", "starcraft116_plugin"),
        OptionalPluginSpec("StarCraft2", "starcraft2_plugin"),
        OptionalPluginSpec(
            "ScreenVision",
            "screen_vision",
            lifecycle_component=True,
            startup_component=True,
            kwargs_factory=_screen_vision_kwargs,
        ),
    )

    def __init__(
        self,
        current_module_directory,
        specs=None,
        manifest_provider=get_optional_module_manifest,
        instantiate_plugin=instantiate_optional_plugin,
    ):
        self.current_module_directory = current_module_directory
        self.specs = tuple(specs or self.DEFAULT_SPECS)
        self.manifest_provider = manifest_provider
        self.instantiate_plugin = instantiate_plugin

    def compose(self, memory_store=None):
        context = OptionalPluginCompositionContext(
            current_module_directory=self.current_module_directory,
            memory_store=memory_store,
        )
        plugins = {}
        for spec in self.specs:
            plugins[spec.attribute_name] = self.instantiate_manifest_plugin(
                spec.module_name,
                **spec.kwargs_factory(context),
            )

        optional_components = tuple(
            plugins[spec.attribute_name]
            for spec in self.specs
            if spec.lifecycle_component
            and plugins.get(spec.attribute_name) is not None
        )
        startup_components = tuple(
            plugins[spec.attribute_name]
            for spec in self.specs
            if spec.startup_component
            and plugins.get(spec.attribute_name) is not None
        )

        return OptionalPluginCompositionResult(
            song_player=plugins.get("song_player"),
            chess_plugin=plugins.get("chess_plugin"),
            starcraft_plugin=plugins.get("starcraft_plugin"),
            starcraft116_plugin=plugins.get("starcraft116_plugin"),
            starcraft2_plugin=plugins.get("starcraft2_plugin"),
            screen_vision=plugins.get("screen_vision"),
            optional_components=optional_components,
            startup_components=startup_components,
        )

    def instantiate_manifest_plugin(self, module_name, *args, **kwargs):
        manifest = self.manifest_provider(module_name)
        return self.instantiate_plugin(
            module_name,
            manifest["module_path"],
            manifest["class_name"],
            manifest["default_enabled"],
            self.current_module_directory,
            *args,
            manifest=manifest,
            **kwargs,
        )
