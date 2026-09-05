#20260905_kpopmodder: Verify one authoritative STOP claim registry across production owners.
import unittest

from plugins.Minecraft.fabric.chatclef.adapter.fabric_chatclef_adapter import (
    FabricChatClefAdapter,
)
from plugins.Minecraft.fabric.chatclef.config import FabricChatClefConfig
from plugins.Minecraft.fabric.chatclef.extension import (
    MinecraftFabricChatClefExtension,
)
from plugins.Minecraft.fabric.chatclef.input import MinecraftChatClefInputRouter
from plugins.Minecraft.fabric.chatclef.input.stop import StopControlClaimRegistry


class StopControlObjectGraphTests(unittest.TestCase):
    def test_runtime_route_owner_and_submitter_share_exact_registry(self):
        adapter = FabricChatClefAdapter(
            config=FabricChatClefConfig(enabled=False),
        )
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )

        registry = extension.get_stop_control_claim_registry()
        self.assertIs(type(registry), StopControlClaimRegistry)
        self.assertIs(router._stop_control_route_owner.claim_registry, registry)
        self.assertIs(
            adapter._server._stop_control_submitter._claim_registry,
            registry,
        )


if __name__ == "__main__":
    unittest.main()
