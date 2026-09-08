#20260905_kpopmodder: Assemble extension collaborators outside the public facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission import (
    AutoDepositTrustClaimedSubmissionAuthorizer,
    AutoDepositTrustCommandAdmission,
    KoreanCommandSubmissionAdmission,
    StoreHomeCommandAdmission,
)
from plugins.Minecraft.fabric.chatclef.extension.command import (
    FabricChatClefCommandSubmissionService,
)
from plugins.Minecraft.fabric.chatclef.extension.command_feedback import (
    FabricChatClefUiCommandFeedbackCoordinator,
    FabricChatClefUiFeedbackStartListener,
)
from plugins.Minecraft.fabric.chatclef.extension.minecraft_fabric_chatclef_extension_lifecycle import (
    MinecraftFabricChatClefExtensionLifecycle,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language import (
    NaturalLanguageCommandCoordinator,
)
from plugins.Minecraft.fabric.chatclef.extension.minecraft_fabric_chatclef_status_provider import (
    MinecraftFabricChatClefStatusProvider,
)
from plugins.Minecraft.fabric.chatclef.extension.minecraft_fabric_chatclef_stop_facade import (
    MinecraftFabricChatClefStopFacade,
)
from plugins.Minecraft.fabric.chatclef.extension.minecraft_fabric_chatclef_command_feedback_facade import (
    MinecraftFabricChatClefCommandFeedbackFacade,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.generic_crafting_defaults_activation_registry import (
    GenericCraftingDefaultsActivationRegistry,
)
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.delivery import (
    AutoDepositTrustInputEventClaimRegistry,
)
from plugins.Minecraft.fabric.chatclef.intent import ChatClefNaturalLanguageService

from .minecraft_fabric_chatclef_adapter_resolver import (
    MinecraftFabricChatClefAdapterResolver,
)
from .minecraft_fabric_chatclef_extension_compatibility_installer import (
    MinecraftFabricChatClefExtensionCompatibilityInstaller,
)


class MinecraftFabricChatClefExtensionComponentGraph:
    def __init__(
        self,
        *,
        owner,
        extension_name: str,
        plugin=None,
        adapter=None,
        natural_language_service=None,
        auto_deposit_trust_claim_registry=None,
        generic_crafting_defaults_activation_registry=None,
    ):
        self._adapter_resolver = MinecraftFabricChatClefAdapterResolver()
        self._compatibility_installer = (
            MinecraftFabricChatClefExtensionCompatibilityInstaller()
        )
        self.plugin = plugin
        self.adapter = adapter or self._adapter_resolver.resolve(plugin)
        self.natural_language_service = (
            natural_language_service or ChatClefNaturalLanguageService()
        )
        self.korean_command_registry = KoreanChatClefCommandRegistry()
        self.store_home_command_admission = StoreHomeCommandAdmission()
        self.auto_deposit_trust_input_claim_registry = (
            auto_deposit_trust_claim_registry
            or AutoDepositTrustInputEventClaimRegistry()
        )
        self.auto_deposit_trust_command_admission = (
            AutoDepositTrustCommandAdmission(
                authorizer=AutoDepositTrustClaimedSubmissionAuthorizer(
                    self.auto_deposit_trust_input_claim_registry
                )
            )
        )
        self.generic_crafting_defaults_activation_registry = (
            generic_crafting_defaults_activation_registry
            or GenericCraftingDefaultsActivationRegistry()
        )
        self.command_submission = FabricChatClefCommandSubmissionService(
            adapter=self.adapter,
            record_command=lambda command: owner.record_command(command),
            record_result=lambda payload, action: owner.record_result(
                payload,
                action=action,
            ),
        )
        self.natural_language_commands = NaturalLanguageCommandCoordinator(
            natural_language_service=self.natural_language_service,
            registry_provider=lambda: self.korean_command_registry,
            command_submitter=lambda command: owner.handle_command(command),
            result_recorder=lambda payload, action: owner.record_result(
                payload,
                action=action,
            ),
            admission=KoreanCommandSubmissionAdmission(
                self.store_home_command_admission,
                self.auto_deposit_trust_command_admission,
            ),
            generic_crafting_defaults_activation_registry=(
                self.generic_crafting_defaults_activation_registry
            ),
        )
        self.lifecycle = MinecraftFabricChatClefExtensionLifecycle(
            adapter=self.adapter,
            mark_started=lambda value: owner.mark_started(value),
            publish_event=lambda event_type, details: owner.publish_event(
                event_type,
                details,
            ),
        )
        self.stop_facade = MinecraftFabricChatClefStopFacade(self.adapter)
        #20260907_kpopmodder: Assemble one generalized Fabric-only feedback facade.
        self.command_feedback_facade = MinecraftFabricChatClefCommandFeedbackFacade(
            self.adapter
        )
        self.crafting_feedback_facade = self.command_feedback_facade
        self.ui_feedback_start_listener = FabricChatClefUiFeedbackStartListener()
        self.ui_command_feedback = FabricChatClefUiCommandFeedbackCoordinator(
            command_submission=self.command_submission,
            natural_language_commands=self.natural_language_commands,
            command_feedback_facade=self.command_feedback_facade,
            start_listener=self.ui_feedback_start_listener,
        )
        self.status_provider = MinecraftFabricChatClefStatusProvider(
            extension_name=extension_name,
            plugin=self.plugin,
            adapter=self.adapter,
            apply_status_contract=lambda status: owner.apply_status_contract(
                status
            ),
        )

    def install_compatibility_seams(self, owner) -> None:
        self._compatibility_installer.install(owner, self)

    def adapter_from_plugin(self, plugin):
        return self._adapter_resolver.resolve(plugin)


__all__ = ("MinecraftFabricChatClefExtensionComponentGraph",)
