#20260905_kpopmodder: Validate and bind the one VoiceInput-final callback owner.
from __future__ import annotations

from input_core.input_event.provenance.input_provider_source_policy import (
    InputProviderSourcePolicy,
)

from .trusted_voice_input_binding import TrustedVoiceInputBinding


class TrustedVoiceInputBindingFactory:
    _VOICE_POLICY = (
        "voice_input_final",
        "VoiceInput",
        "final_transcript",
        True,
    )

    def create(
        self,
        *,
        input_event_adapter,
        producer_registrar_factory,
    ) -> TrustedVoiceInputBinding:
        self._validate_adapter(input_event_adapter)
        policy = input_event_adapter.policy
        self._validate_factory(producer_registrar_factory)
        producer_registrar_factory._bind_voice_input_final_event_adapter(
            input_event_adapter
        )
        invocation_owner = (
            producer_registrar_factory._claim_callback_invocation_owner(
                input_event_adapter=input_event_adapter,
                source_policy=policy,
            )
        )
        return TrustedVoiceInputBinding(
            input_event_adapter=input_event_adapter,
            producer_registrar_factory=producer_registrar_factory,
            invocation_owner=invocation_owner,
            source_policy=policy,
        )

    def _validate_adapter(self, input_event_adapter) -> None:
        if not callable(getattr(input_event_adapter, "adapt", None)):
            raise TypeError("input_event_adapter.adapt must be callable")
        policy = getattr(input_event_adapter, "policy", None)
        if type(policy) is not InputProviderSourcePolicy:
            raise TypeError(
                "input_event_adapter.policy must be InputProviderSourcePolicy"
            )
        if (
            policy.source,
            policy.provider_id,
            policy.event_kind,
            policy.final,
        ) != self._VOICE_POLICY:
            raise ValueError("input_event_adapter is not VoiceInput-final")

    @staticmethod
    def _validate_factory(producer_registrar_factory) -> None:
        if not callable(
            getattr(producer_registrar_factory, "begin_invocation", None)
        ):
            raise TypeError(
                "producer_registrar_factory.begin_invocation must be callable"
            )
        if not callable(
            getattr(
                producer_registrar_factory,
                "_bind_voice_input_final_event_adapter",
                None,
            )
        ):
            raise TypeError(
                "producer registrar factory cannot bind VoiceInput-final"
            )
        if not callable(
            getattr(
                producer_registrar_factory,
                "_claim_callback_invocation_owner",
                None,
            )
        ):
            raise TypeError("producer registrar factory cannot bind callback owner")


__all__ = ("TrustedVoiceInputBindingFactory",)
