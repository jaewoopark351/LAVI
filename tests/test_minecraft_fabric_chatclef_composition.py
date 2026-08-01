#20260801_kpopmodder: Verify Fabric ChatClef registers through game extension composition.
import unittest
from types import SimpleNamespace

from app_core.extensions import (
    ExtensionRegistry,
    GameEventBus,
    GameExtensionCompositionService,
    GameExtensionContext,
    GameRuntimeContextRegistry,
)


class MinecraftFabricChatClefCompositionTests(unittest.TestCase):
    def test_fabric_chatclef_plugin_registers_backend_specific_extension(self):
        registry = ExtensionRegistry()
        context = GameExtensionContext(
            runtime_contexts=GameRuntimeContextRegistry(),
            event_bus=GameEventBus(),
        )
        plugin = SimpleNamespace(create_adapter=lambda: _FakeAdapter())
        service = GameExtensionCompositionService(
            registry,
            logger=lambda _message: None,
        )

        result = service.compose(
            context=context,
            minecraft_fabric_chatclef_plugin=plugin,
        )

        self.assertEqual(
            ["minecraft_fabric_chatclef"],
            [extension.name for extension in result.registered_extensions],
        )
        self.assertIsNotNone(result.minecraft_fabric_chatclef_extension)
        self.assertIsNotNone(registry.get("minecraft_fabric_chatclef"))
        self.assertEqual([], result.errors)


class _FakeAdapter:
    backend_id = "fabric_chatclef"

    def start(self):
        return None

    def stop(self):
        return None

    def submit_command(self, request):
        return request

    def get_status(self):
        return SimpleNamespace(
            to_dict=lambda: {
                "backend_id": "fabric_chatclef",
                "enabled": False,
                "connected": False,
                "lifecycle_state": "disabled",
                "detail": "disabled",
                "details": {},
                "last_error_code": None,
                "last_error_message": None,
            },
            enabled=False,
            last_error_message=None,
        )


if __name__ == "__main__":
    unittest.main()
