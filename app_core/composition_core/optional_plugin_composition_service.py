#20260717_kpopmodder: Keeps optional plugin construction details outside AppComposer.
from app_core.optional_module_manifest import get_optional_module_manifest
from app_core.optional_plugin_loader import instantiate_optional_plugin

from .optional_plugin_composition_context import OptionalPluginCompositionContext
from .optional_plugin_composition_result import OptionalPluginCompositionResult
from .optional_plugin_spec import OptionalPluginSpec


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
        OptionalPluginSpec("Minecraft", "minecraft_plugin"),
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
            minecraft_plugin=plugins.get("minecraft_plugin"),
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
