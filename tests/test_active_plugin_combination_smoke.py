#20260718_kpopmodder: Smoke-test active plugin combinations without starting heavy runtimes.
import unittest
from pathlib import Path
from types import SimpleNamespace

from app_core.extensions import (
    ExtensionRegistry,
    GameEventBus,
    GameExtensionCompositionService,
    GameExtensionContext,
    GameRuntimeContextRegistry,
)
from app_core.optional_plugin_composition import OptionalPluginCompositionService
from core.profile_resolver import ModuleSettingsSnapshot, load_module_settings


PROJECT_ROOT = Path(__file__).resolve().parents[1]


class ActivePluginCombinationSmokeTests(unittest.TestCase):
    #20260718_kpopmodder: Covers currently enabled plugin combinations with fake resources.
    def test_production_modules_snapshot_keeps_legacy_voice_plugins_disabled(self):
        snapshot = self._production_snapshot()

        self.assertTrue(snapshot.is_enabled("VoiceInput"))
        self.assertTrue(snapshot.is_enabled("GPTSoVITS"))
        self.assertTrue(snapshot.is_enabled("VtubeStudio"))
        self.assertTrue(snapshot.is_enabled("ScreenVision"))
        self.assertTrue(snapshot.is_enabled("Chess"))
        self.assertTrue(snapshot.is_enabled("StarCraft116"))
        self.assertTrue(snapshot.is_enabled("StarCraft2"))
        for module_name in ("rvc", "vitsTTS", "silero", "voicevox"):
            self.assertFalse(snapshot.is_enabled(module_name), module_name)

    def test_active_optional_plugin_combo_composes_with_mocked_instances(self):
        snapshot = self._production_snapshot()
        constructed = []

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
            if not snapshot.is_enabled(plugin_name):
                return None
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

        memory_store = object()
        service = OptionalPluginCompositionService(
            current_module_directory=str(PROJECT_ROOT),
            instantiate_plugin=instantiate_plugin,
        )

        result = service.compose(memory_store=memory_store)
        constructed_names = [plugin.plugin_name for plugin in constructed]

        self.assertEqual(
            ["SongPlayer", "Chess", "StarCraft116", "StarCraft2", "ScreenVision"],
            constructed_names,
        )
        self.assertNotIn("StarCraftRemastered", constructed_names)
        self.assertIs(memory_store, result.screen_vision.kwargs["memory_store"])
        self.assertEqual("Chess", result.chess_plugin.plugin_name)
        self.assertEqual("StarCraft116", result.starcraft116_plugin.plugin_name)
        self.assertEqual("StarCraft2", result.starcraft2_plugin.plugin_name)

    def test_active_game_plugins_register_through_extension_registry(self):
        registry = ExtensionRegistry()
        context = GameExtensionContext(
            runtime_contexts=GameRuntimeContextRegistry(),
            event_bus=GameEventBus(),
        )
        service = GameExtensionCompositionService(registry, logger=lambda _message: None)

        result = service.compose(
            context=context,
            chess_plugin=_fake_chess_plugin(),
            starcraft116_plugin=_fake_starcraft116_plugin(),
            starcraft2_plugin=_fake_starcraft2_plugin(),
            starcraft2_changeling_observer_extension=_fake_observer_extension(),
        )

        registered_names = [extension.name for extension in result.registered_extensions]

        self.assertEqual(
            ["starcraft116", "starcraft2", "chess"],
            registered_names,
        )
        self.assertEqual(
            "starcraft2_changeling_observer",
            result.starcraft2_changeling_observer_extension.name,
        )
        self.assertIsNotNone(registry.get("chess"))
        self.assertIsNotNone(registry.get("starcraft116"))
        self.assertIsNotNone(registry.get("starcraft2"))
        self.assertEqual([], result.errors)

    def _production_snapshot(self):
        resolution = load_module_settings(PROJECT_ROOT)
        snapshot = resolution.snapshot()
        self.assertIsInstance(snapshot, ModuleSettingsSnapshot)
        return snapshot


def _fake_chess_plugin():
    server_url = "http://127.0.0.1:8765"
    return SimpleNamespace(
        server_url=server_url,
        server_message="ready",
        controller=SimpleNamespace(new_game=lambda: {"fen": "startpos"}),
        web_server=SimpleNamespace(url=server_url),
    )


def _fake_starcraft116_plugin():
    return SimpleNamespace(
        status_event_callback=None,
        game_event_thread=None,
        game_event_stop_event=None,
        config_manager=object(),
        launcher=object(),
        status_reader=object(),
        state=object(),
        get_status=lambda: {"ready": True},
    )


def _fake_starcraft2_plugin():
    return SimpleNamespace(
        start=lambda overrides=None, launch_source="": {"ok": True},
        stop=lambda: {"ok": True},
        shutdown=lambda: None,
        get_status=lambda: {"running": False},
    )


def _fake_observer_extension():
    return SimpleNamespace(
        name="starcraft2_changeling_observer",
        initialize=lambda context: None,
        start=lambda: {"ok": True},
        stop=lambda: {"ok": True},
        shutdown=lambda: None,
        handle_command=lambda command: {"ok": True, "command": command},
        get_status=lambda: {"name": "starcraft2_changeling_observer", "running": False},
    )


if __name__ == "__main__":
    unittest.main()
