#20260905_kpopmodder: Owns trusted adapter binding and callback-owner authorization.
from __future__ import annotations

from input_core.input_event.provenance.input_provider_source_policy import (
    InputProviderSourcePolicy,
)

from ..trusted_ingress_adapter_binding_registry import (
    TrustedIngressAdapterBindingRegistry,
)
from ..trusted_ingress_callback_invocation_owner import (
    TrustedIngressCallbackInvocationOwner,
)
from ..trusted_ingress_callback_invocation_owner_registry import (
    TrustedIngressCallbackInvocationOwnerRegistry,
)


class TrustedIngressProducerBindingAuthority:
    def __init__(self) -> None:
        self.adapter_binding_registry = TrustedIngressAdapterBindingRegistry()
        self.callback_owner_registry = (
            TrustedIngressCallbackInvocationOwnerRegistry()
        )

    def validate_unowned_invocation(
        self,
        *,
        input_event_adapter: object,
        source_policy: InputProviderSourcePolicy,
    ) -> None:
        self.validate_invocation(
            input_event_adapter=input_event_adapter,
            source_policy=source_policy,
        )
        if self.callback_owner_registry.is_claimed(input_event_adapter):
            raise PermissionError(
                "trusted invocation is owned by its bound callback"
            )

    def validate_invocation(
        self,
        *,
        input_event_adapter: object,
        source_policy: InputProviderSourcePolicy,
    ) -> None:
        self.adapter_binding_registry.validate_invocation(
            input_event_adapter=input_event_adapter,
            source_policy=source_policy,
        )

    def claim_callback_owner(
        self,
        *,
        input_event_adapter: object,
        source_policy: InputProviderSourcePolicy,
        begin_owned_invocation_callback,
    ) -> TrustedIngressCallbackInvocationOwner:
        self.validate_invocation(
            input_event_adapter=input_event_adapter,
            source_policy=source_policy,
        )
        owner_token = self.callback_owner_registry.claim(input_event_adapter)
        return TrustedIngressCallbackInvocationOwner(
            begin_owned_invocation_callback=begin_owned_invocation_callback,
            input_event_adapter=input_event_adapter,
            source_policy=source_policy,
            owner_token=owner_token,
        )

    def authorize_owned_invocation(
        self,
        *,
        input_event_adapter: object,
        source_policy: InputProviderSourcePolicy,
        owner_token: object,
    ) -> None:
        self.validate_invocation(
            input_event_adapter=input_event_adapter,
            source_policy=source_policy,
        )
        self.callback_owner_registry.authorize(
            input_event_adapter,
            owner_token,
        )

    def bind_local_chat(self, input_event_adapter: object) -> None:
        self.adapter_binding_registry.bind_local_chat(input_event_adapter)

    def bind_voice_final(self, input_event_adapter: object) -> None:
        self.adapter_binding_registry.bind_voice_final(input_event_adapter)

    def bind_exact_adapter(
        self,
        input_event_adapter: object,
        policy_tuple: tuple[object, ...],
        attribute_name: str,
    ) -> None:
        self.adapter_binding_registry._bind_exact_adapter(
            input_event_adapter,
            policy_tuple,
            attribute_name,
        )

    def validate_exact_adapter(
        self,
        input_event_adapter: object,
        policy_tuple: tuple[object, ...],
    ) -> None:
        self.adapter_binding_registry._validate_exact_adapter(
            input_event_adapter,
            policy_tuple,
        )


__all__ = ("TrustedIngressProducerBindingAuthority",)
