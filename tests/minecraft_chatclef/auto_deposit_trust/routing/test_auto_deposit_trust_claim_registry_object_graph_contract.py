#20260905_kpopmodder: Verify one instance-owned registry is shared through the H5 route graph.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.extension import (
    MinecraftFabricChatClefExtension,
)
from plugins.Minecraft.fabric.chatclef.input import MinecraftChatClefInputRouter
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.delivery import (
    AutoDepositTrustInputEventClaimRegistry,
)


class AutoDepositTrustClaimRegistryObjectGraphContractTests(unittest.TestCase):
    def test_router_extension_and_authorizer_share_one_registry_instance(self):
        extension = MinecraftFabricChatClefExtension(
            adapter=SimpleNamespace(backend_id="fabric_chatclef")
        )
        router = MinecraftChatClefInputRouter(
            extension=extension,
            log_callback=lambda _message: None,
        )
        registry = extension.get_auto_deposit_trust_input_claim_registry()

        self.assertIs(registry, router.auto_deposit_trust_claim_registry)
        self.assertIs(
            registry,
            extension.auto_deposit_trust_command_admission.authorizer.claim_registry,
        )

    def test_explicit_registry_override_cannot_split_the_extension_graph(self):
        extension = MinecraftFabricChatClefExtension(
            adapter=SimpleNamespace(backend_id="fabric_chatclef")
        )

        with self.assertRaisesRegex(ValueError, "must share one claim registry"):
            MinecraftChatClefInputRouter(
                extension=extension,
                auto_deposit_trust_claim_registry=(
                    AutoDepositTrustInputEventClaimRegistry()
                ),
                log_callback=lambda _message: None,
            )


if __name__ == "__main__":
    unittest.main()
