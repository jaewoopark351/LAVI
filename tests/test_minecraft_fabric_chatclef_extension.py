#20260801_kpopmodder: Cover Fabric ChatClef GameExtension without GUI wiring.
import unittest

from app_core.extensions import (
    GameEventBus,
    GameExtensionContext,
    GameRuntimeContextRegistry,
)
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.bridge_lifecycle_state import (
    BridgeLifecycleState,
)
from plugins.Minecraft.fabric.chatclef.extension import (
    MinecraftFabricChatClefExtension,
)
from plugins.Minecraft.fabric.chatclef.minecraft_fabric_chatclef_plugin import (
    MinecraftFabricChatClefPlugin,
)


class MinecraftFabricChatClefExtensionTests(unittest.TestCase):
    def test_default_plugin_extension_reports_disabled_status(self):
        extension = MinecraftFabricChatClefExtension(
            plugin=MinecraftFabricChatClefPlugin()
        )
        context = GameExtensionContext(
            runtime_contexts=GameRuntimeContextRegistry(),
            event_bus=GameEventBus(),
        )
        extension.initialize(context)

        status = extension.get_status()

        self.assertEqual("minecraft_fabric_chatclef", extension.name)
        self.assertEqual(
            BridgeLifecycleState.DISABLED.value,
            status["details"]["lifecycle_state"],
        )
        self.assertFalse(status["details"]["connected"])
        self.assertIn("game_status", status)

    def test_string_command_is_rejected_without_enabled_bridge(self):
        extension = MinecraftFabricChatClefExtension(
            plugin=MinecraftFabricChatClefPlugin()
        )

        result = extension.handle_command("@get dirt 1")

        self.assertFalse(result["ok"])
        self.assertEqual(
            BridgeErrorCode.BRIDGE_DISABLED.value,
            result["status"]["error_code"],
        )
        self.assertEqual("rejected", result["status"]["status"])


if __name__ == "__main__":
    unittest.main()
