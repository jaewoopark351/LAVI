#20260801_kpopmodder: Register Fabric ChatClef as a LAVI game extension.
#20260827_kpopmodder: Enforce staged STORE_HOME source admission before bridge submission.
#20260905_kpopmodder: Preserve the extension API as a thin collaborator facade.
from __future__ import annotations

from typing import Any

from app_core.extensions.game_extension_interface import GameExtensionInterface
from plugins.Minecraft.fabric.chatclef.adapter.fabric_chatclef_adapter import (
    FabricChatClefAdapter,
)
from plugins.Minecraft.fabric.chatclef.extension.composition import (
    MinecraftFabricChatClefExtensionComponentGraph,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.generic_crafting_defaults_activation_registry import (
    GenericCraftingDefaultsActivationRegistry,
)
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.delivery import (
    AutoDepositTrustInputEventClaimRegistry,
)
from plugins.Minecraft.fabric.chatclef.intent import ChatClefNaturalLanguageService


class MinecraftFabricChatClefExtension(GameExtensionInterface):
    EXTENSION_NAME = "minecraft_fabric_chatclef"

    def __init__(
        self,
        plugin: Any = None,
        adapter: FabricChatClefAdapter | None = None,
        natural_language_service: ChatClefNaturalLanguageService | None = None,
        auto_deposit_trust_claim_registry: (
            AutoDepositTrustInputEventClaimRegistry | None
        ) = None,
        generic_crafting_defaults_activation_registry: (
            GenericCraftingDefaultsActivationRegistry | None
        ) = None,
    ):
        self._component_graph = MinecraftFabricChatClefExtensionComponentGraph(
            owner=self,
            extension_name=self.EXTENSION_NAME,
            plugin=plugin,
            adapter=adapter,
            natural_language_service=natural_language_service,
            auto_deposit_trust_claim_registry=(
                auto_deposit_trust_claim_registry
            ),
            generic_crafting_defaults_activation_registry=(
                generic_crafting_defaults_activation_registry
            ),
        )
        self._component_graph.install_compatibility_seams(self)

    @property
    def name(self) -> str:
        return self.EXTENSION_NAME

    def start(self) -> None:
        self._lifecycle.start()

    def stop(self) -> None:
        self._lifecycle.stop()

    def handle_command(self, command: Any) -> dict[str, Any]:
        return self._command_submission.submit(command)

    def submit_stop_control(
        self,
        *,
        event: object,
        eligibility_proof: object,
        receipt: object,
    ):
        return self._stop_facade.submit(
            event=event,
            eligibility_proof=eligibility_proof,
            receipt=receipt,
        )

    def get_stop_control_claim_registry(self):
        return self._stop_facade.claim_registry()

    def set_stop_terminal_response_callback(self, callback) -> None:
        self._stop_facade.set_terminal_response_callback(callback)

    def translate_natural_language_command(self, command: Any) -> dict[str, Any]:
        return self._natural_language_commands.translate(command)

    def handle_natural_language_command(self, command: Any) -> dict[str, Any]:
        return self._natural_language_commands.handle(command)

    def translate_generic_crafting_defaults_command(
        self,
        command: Any,
        *,
        input_event: object,
        item_resolution_profile: object,
        activation_receipt: object,
        korean_eligibility_proof: object,
    ) -> dict[str, Any]:
        return self._natural_language_commands.translate_generic_crafting_defaults(
            command,
            input_event=input_event,
            item_resolution_profile=item_resolution_profile,
            activation_receipt=activation_receipt,
            korean_eligibility_proof=korean_eligibility_proof,
        )

    def submit_translated_command(
        self,
        command: Any,
        translation: Any,
        *,
        route_claim: object = None,
    ) -> dict[str, Any]:
        return self._natural_language_commands.submit_translated(
            command,
            translation,
            route_claim=route_claim,
        )

    def submit_translated_generic_crafting_defaults_command(
        self,
        command: Any,
        translation: Any,
        *,
        activation_receipt: object,
        korean_eligibility_proof: object,
    ) -> dict[str, Any]:
        return (
            self._natural_language_commands.submit_translated_generic_crafting_defaults(
                command,
                translation,
                activation_receipt=activation_receipt,
                korean_eligibility_proof=korean_eligibility_proof,
            )
        )

    def get_auto_deposit_trust_input_claim_registry(
        self,
    ) -> AutoDepositTrustInputEventClaimRegistry:
        return self.auto_deposit_trust_input_claim_registry

    def get_generic_crafting_defaults_activation_registry(
        self,
    ) -> GenericCraftingDefaultsActivationRegistry:
        return self.generic_crafting_defaults_activation_registry

    def get_status(self) -> dict[str, Any]:
        return self._status_provider.get_status()

    def _adapter_from_plugin(self, plugin: Any) -> FabricChatClefAdapter:
        return self._component_graph.adapter_from_plugin(plugin)

    def _plugin_status(self) -> dict[str, Any]:
        return self._status_provider.plugin_status()
