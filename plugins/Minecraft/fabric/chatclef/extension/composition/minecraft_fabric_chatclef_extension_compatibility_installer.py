#20260905_kpopmodder: Install legacy extension attributes without owning assembly.


class MinecraftFabricChatClefExtensionCompatibilityInstaller:
    def install(self, owner, graph) -> None:
        owner.plugin = graph.plugin
        owner.adapter = graph.adapter
        owner.natural_language_service = graph.natural_language_service
        owner.korean_command_registry = graph.korean_command_registry
        owner.store_home_command_admission = graph.store_home_command_admission
        owner.auto_deposit_trust_input_claim_registry = (
            graph.auto_deposit_trust_input_claim_registry
        )
        owner.auto_deposit_trust_command_admission = (
            graph.auto_deposit_trust_command_admission
        )
        owner.generic_crafting_defaults_activation_registry = (
            graph.generic_crafting_defaults_activation_registry
        )
        owner._command_submission = graph.command_submission
        owner._natural_language_commands = graph.natural_language_commands
        owner._lifecycle = graph.lifecycle
        owner._stop_facade = graph.stop_facade
        owner._status_provider = graph.status_provider
        owner.context = None
        owner.runtime_context = None
        owner.event_bus = None


__all__ = ("MinecraftFabricChatClefExtensionCompatibilityInstaller",)
