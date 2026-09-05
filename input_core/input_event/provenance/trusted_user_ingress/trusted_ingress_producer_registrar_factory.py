#20260905_kpopmodder: Preserves the trusted producer registrar factory API.
from __future__ import annotations

from input_core.input_event.provenance.input_provider_source_policy import (
    InputProviderSourcePolicy,
)

from .composition import TrustedIngressProducerRegistrarFactoryComponentGraph
from .trusted_ingress_adapter_binding_registry import TrustedIngressAdapterBindingRegistry
from .trusted_ingress_callback_invocation_owner import (
    TrustedIngressCallbackInvocationOwner,
)
from .trusted_ingress_producer_registrar import TrustedIngressProducerRegistrar
from .trusted_user_input_ingress_claim_registry import (
    TrustedUserInputIngressClaimRegistry,
)


class TrustedIngressProducerRegistrarFactory:
    _CANONICAL_POLICIES = TrustedIngressAdapterBindingRegistry._CANONICAL_POLICIES

    def __init__(self, registry: TrustedUserInputIngressClaimRegistry):
        if type(registry) is not TrustedUserInputIngressClaimRegistry:
            raise TypeError(
                "registry must be TrustedUserInputIngressClaimRegistry"
            )
        self._registry = registry
        self._components = TrustedIngressProducerRegistrarFactoryComponentGraph(
            registry=registry
        )
        self._factory_token = self._components.factory_token
        self._binding_authority = self._components.binding_authority
        self._adapter_binding_registry = (
            self._binding_authority.adapter_binding_registry
        )
        self._adapter_binding_lock = self._adapter_binding_registry._lock
        self._local_chat_input_event_adapter = None
        self._voice_input_final_event_adapter = None
        self._callback_invocation_owner_registry = (
            self._binding_authority.callback_owner_registry
        )
        self._registry_token = self._components.registry_token
        self._registrar_issuer = self._components.registrar_issuer
        self._invocation_issuer = self._registrar_issuer.issuer

    @property
    def registry(self) -> TrustedUserInputIngressClaimRegistry:
        return self._registry

    def begin_invocation(
        self,
        *,
        input_event_adapter: object,
        source_policy: InputProviderSourcePolicy,
    ) -> TrustedIngressProducerRegistrar:
        self._binding_authority.validate_unowned_invocation(
            input_event_adapter=input_event_adapter,
            source_policy=source_policy,
        )
        return self._registrar_issuer.issue(
            input_event_adapter=input_event_adapter,
            source_policy=source_policy,
        )

    def _claim_callback_invocation_owner(
        self,
        *,
        input_event_adapter: object,
        source_policy: InputProviderSourcePolicy,
    ) -> TrustedIngressCallbackInvocationOwner:
        return self._binding_authority.claim_callback_owner(
            begin_owned_invocation_callback=self._begin_owned_invocation,
            input_event_adapter=input_event_adapter,
            source_policy=source_policy,
        )

    def _begin_owned_invocation(
        self,
        *,
        input_event_adapter: object,
        source_policy: InputProviderSourcePolicy,
        owner_token: object,
    ) -> TrustedIngressProducerRegistrar:
        self._binding_authority.authorize_owned_invocation(
            input_event_adapter=input_event_adapter,
            source_policy=source_policy,
            owner_token=owner_token,
        )
        return self._registrar_issuer.issue(
            input_event_adapter=input_event_adapter,
            source_policy=source_policy,
        )

    def _bind_local_chat_input_event_adapter(
        self,
        input_event_adapter: object,
    ) -> None:
        self._binding_authority.bind_local_chat(input_event_adapter)
        self._local_chat_input_event_adapter = input_event_adapter

    def _bind_voice_input_final_event_adapter(
        self,
        input_event_adapter: object,
    ) -> None:
        self._binding_authority.bind_voice_final(input_event_adapter)
        self._voice_input_final_event_adapter = input_event_adapter

    def _bind_exact_adapter(
        self,
        input_event_adapter: object,
        policy_tuple: tuple[object, ...],
        attribute_name: str,
    ) -> None:
        self._binding_authority.bind_exact_adapter(
            input_event_adapter,
            policy_tuple,
            attribute_name,
        )
        setattr(self, attribute_name, input_event_adapter)

    def _validate_exact_adapter(
        self,
        input_event_adapter: object,
        policy_tuple: tuple[object, ...],
    ) -> None:
        self._binding_authority.validate_exact_adapter(
            input_event_adapter,
            policy_tuple,
        )


__all__ = ("TrustedIngressProducerRegistrarFactory",)
