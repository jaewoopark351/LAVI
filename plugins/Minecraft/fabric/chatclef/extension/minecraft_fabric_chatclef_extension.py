#20260801_kpopmodder: Register Fabric ChatClef as a LAVI game extension.
#20260827_kpopmodder: Enforce staged STORE_HOME source admission before bridge submission.
from __future__ import annotations

from typing import Any

from app_core.extensions.game_extension_interface import GameExtensionInterface
from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission import (
    AutoDepositTrustClaimedSubmissionAuthorizer,
    AutoDepositTrustCommandAdmission,
    KoreanCommandSubmissionAdmission,
    StoreHomeCommandAdmission,
)
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.delivery import (
    AutoDepositTrustInputEventClaimRegistry,
)
from plugins.Minecraft.fabric.chatclef.adapter.fabric_chatclef_adapter import (
    FabricChatClefAdapter,
)
from plugins.Minecraft.fabric.chatclef.extension.command import (
    FabricChatClefCommandSubmissionService,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language import (
    NaturalLanguageCommandCoordinator,
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
    ):
        self.plugin = plugin
        self.adapter = adapter or self._adapter_from_plugin(plugin)
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
        self._command_submission = FabricChatClefCommandSubmissionService(
            adapter=self.adapter,
            record_command=self.record_command,
            record_result=lambda payload, action: self.record_result(
                payload,
                action=action,
            ),
        )
        self._natural_language_commands = NaturalLanguageCommandCoordinator(
            natural_language_service=self.natural_language_service,
            registry_provider=lambda: self.korean_command_registry,
            command_submitter=self.handle_command,
            result_recorder=lambda payload, action: self.record_result(
                payload,
                action=action,
            ),
            admission=KoreanCommandSubmissionAdmission(
                self.store_home_command_admission,
                self.auto_deposit_trust_command_admission,
            ),
        )
        self.context = None
        self.runtime_context = None
        self.event_bus = None

    @property
    def name(self) -> str:
        return self.EXTENSION_NAME

    def start(self) -> None:
        self.adapter.start()
        status = self.adapter.get_status()
        self.mark_started(status.enabled and not bool(status.last_error_message))
        self.publish_event(
            "minecraft_fabric_chatclef_started",
            {"status": status.to_dict()},
        )

    def stop(self) -> None:
        self.adapter.stop()
        self.mark_started(False)
        self.publish_event("minecraft_fabric_chatclef_stopped", {})

    def handle_command(self, command: Any) -> dict[str, Any]:
        return self._command_submission.submit(command)

    def translate_natural_language_command(self, command: Any) -> dict[str, Any]:
        return self._natural_language_commands.translate(command)

    def handle_natural_language_command(self, command: Any) -> dict[str, Any]:
        return self._natural_language_commands.handle(command)

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

    def get_auto_deposit_trust_input_claim_registry(
        self,
    ) -> AutoDepositTrustInputEventClaimRegistry:
        return self.auto_deposit_trust_input_claim_registry

    def get_status(self) -> dict[str, Any]:
        status = self.adapter.get_status().to_dict()
        return self.apply_status_contract(
            {
                "name": self.name,
                "plugin": self._plugin_status(),
                "runtime": {"backend_id": self.adapter.backend_id},
                "details": status,
                "error": status.get("last_error_message"),
            }
        )

    def _adapter_from_plugin(self, plugin: Any) -> FabricChatClefAdapter:
        adapter_factory = getattr(plugin, "create_adapter", None)
        if callable(adapter_factory):
            return adapter_factory()
        return FabricChatClefAdapter()

    def _plugin_status(self) -> dict[str, Any]:
        status = getattr(self.plugin, "get_status", None)
        if callable(status):
            return dict(status())
        return {"present": self.plugin is not None}
