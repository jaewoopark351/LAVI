#20260905_kpopmodder: Resolve and validate injected router dependencies outside assembly.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.generic_crafting_defaults_admission import (
    GenericCraftingDefaultsAdmission,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.generic_crafting_defaults_route_owner import (
    GenericCraftingDefaultsRouteOwner,
)
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust import (
    AutoDepositTrustInputEventClaimRegistry,
)
from plugins.Minecraft.fabric.chatclef.input.stop import (
    KoreanStopControlRouteOwner,
    StopControlClaimRegistry,
    StopInputDecisionLogger,
)


class MinecraftChatClefRouterDependencyResolver:
    def __init__(self, *, owner, extension):
        self._owner = owner
        self._extension = extension

    def input_event_normalizer(self, provided):
        if provided:
            return provided
        from input_core.input_event.normalization import LaviInputEventNormalizer

        return LaviInputEventNormalizer()

    def auto_deposit_trust_claim_registry(self, provided):
        extension_registry = self.extension_registry(
            self._extension,
            "get_auto_deposit_trust_input_claim_registry",
        )
        if (
            provided is not None
            and extension_registry is not None
            and provided is not extension_registry
        ):
            raise ValueError("H5 router and extension must share one claim registry")
        claim_registry = provided if provided is not None else extension_registry
        if claim_registry is None:
            claim_registry = AutoDepositTrustInputEventClaimRegistry()
        claim_owner_binder = getattr(claim_registry, "_bind_claim_owner", None)
        if not callable(claim_owner_binder):
            raise TypeError("H5 claim registry cannot bind its router owner")
        claim_owner_binder(self._owner)
        return claim_registry

    def stop_route_owner(self, *, provided, log_callback):
        if provided is not None:
            return provided
        claim_registry = self.extension_registry(
            self._extension,
            "get_stop_control_claim_registry",
        )
        if claim_registry is None:
            claim_registry = StopControlClaimRegistry()
        if type(claim_registry) is not StopControlClaimRegistry:
            raise TypeError("STOP control requires one exact claim registry")
        claim_registry._bind_claim_owner(self._owner)
        return KoreanStopControlRouteOwner(
            extension=self._extension,
            claim_registry=claim_registry,
            decision_logger=StopInputDecisionLogger(log_callback),
        )

    def generic_crafting_route_owner(
        self,
        *,
        provided_admission,
        provided_owner,
        trusted_input_route_coordinator,
        translation_boundary,
        submission_precheck,
        submission_boundary,
        submission_reconciliation,
        decision_factory,
    ):
        if provided_admission is not None and provided_owner is not None:
            raise ValueError("provide Feature-B admission or route owner, not both")
        if provided_owner is not None:
            return provided_owner
        if provided_admission is not None:
            self.validate_generic_crafting_registry(provided_admission)
            admission = provided_admission
        else:
            activation_registry = self.extension_registry(
                self._extension,
                "get_generic_crafting_defaults_activation_registry",
            )
            if activation_registry is None:
                return None
            admission = GenericCraftingDefaultsAdmission(
                activation_registry,
                trusted_input_route_coordinator.is_live_proof,
            )
        return GenericCraftingDefaultsRouteOwner(
            extension=self._extension,
            admission=admission,
            translation_boundary=translation_boundary,
            submission_precheck=submission_precheck,
            submission_boundary=submission_boundary,
            submission_reconciliation=submission_reconciliation,
            decision_factory=decision_factory,
        )

    def validate_generic_crafting_registry(self, admission) -> None:
        activation_registry = self.extension_registry(
            self._extension,
            "get_generic_crafting_defaults_activation_registry",
        )
        if (
            activation_registry is not None
            and activation_registry is not admission.activation_registry
        ):
            raise ValueError(
                "Feature-B router and extension must share one activation registry"
            )

    def extension_registry(self, extension, getter_name: str):
        getter = getattr(extension, getter_name, None)
        if callable(getter):
            return getter()
        return None


__all__ = ("MinecraftChatClefRouterDependencyResolver",)
