#20260717_kpopmodder: Verify optional plugin composition stays outside AppComposer.
import unittest
from types import SimpleNamespace

from app_core.optional_plugin_composition import OptionalPluginCompositionService


class OptionalPluginCompositionTests(unittest.TestCase):
    def test_compose_preserves_order_and_lifecycle_roles(self):
        constructed = []
        memory_store = object()

        def instantiate_plugin(
            plugin_name,
            module_path,
            class_name,
            default_enabled,
            project_root,
            *args,
            manifest=None,
            **kwargs,
        ):
            plugin = SimpleNamespace(
                plugin_name=plugin_name,
                module_path=module_path,
                class_name=class_name,
                default_enabled=default_enabled,
                project_root=project_root,
                manifest=manifest,
                kwargs=kwargs,
            )
            constructed.append(plugin)
            return plugin

        service = OptionalPluginCompositionService(
            current_module_directory="C:\\LAVI",
            instantiate_plugin=instantiate_plugin,
        )

        result = service.compose(memory_store=memory_store)

        self.assertEqual(
            [
                "SongPlayer",
                "Chess",
                "StarCraftRemastered",
                "StarCraft116",
                "StarCraft2",
                "ScreenVision",
            ],
            [plugin.plugin_name for plugin in constructed],
        )
        self.assertEqual(
            [
                result.song_player,
                result.starcraft_plugin,
                result.screen_vision,
            ],
            list(result.optional_components),
        )
        self.assertEqual(
            [
                result.song_player,
                result.starcraft_plugin,
                result.screen_vision,
            ],
            list(result.startup_components),
        )
        self.assertIs(memory_store, result.screen_vision.kwargs["memory_store"])

    def test_instantiate_manifest_plugin_uses_manifest_contract(self):
        calls = []

        def manifest_provider(module_name):
            return {
                "module_path": "plugins.Example.example",
                "class_name": "ExamplePlugin",
                "default_enabled": False,
                "id": module_name,
            }

        def instantiate_plugin(*args, **kwargs):
            calls.append((args, kwargs))
            return "plugin"

        service = OptionalPluginCompositionService(
            current_module_directory="C:\\LAVI",
            manifest_provider=manifest_provider,
            instantiate_plugin=instantiate_plugin,
        )

        plugin = service.instantiate_manifest_plugin("Example", extra=True)

        self.assertEqual("plugin", plugin)
        self.assertEqual(
            (
                "Example",
                "plugins.Example.example",
                "ExamplePlugin",
                False,
                "C:\\LAVI",
            ),
            calls[0][0],
        )
        self.assertEqual(
            {
                "manifest": {
                    "module_path": "plugins.Example.example",
                    "class_name": "ExamplePlugin",
                    "default_enabled": False,
                    "id": "Example",
                },
                "extra": True,
            },
            calls[0][1],
        )


if __name__ == "__main__":
    unittest.main()
