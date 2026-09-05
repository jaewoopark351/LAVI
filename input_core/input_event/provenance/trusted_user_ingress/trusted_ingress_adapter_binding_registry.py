#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

import threading

from input_core.input_event.provenance.input_provider_source_policy import (
    InputProviderSourcePolicy,
)


class TrustedIngressAdapterBindingRegistry:
    _LOCAL_CHAT_POLICY = (
        "lavi_chat_ui",
        "lavi_chat_ui",
        "chat_submit",
        True,
    )
    _VOICE_FINAL_POLICY = (
        "voice_input_final",
        "VoiceInput",
        "final_transcript",
        True,
    )
    _CANONICAL_POLICIES = frozenset({_LOCAL_CHAT_POLICY, _VOICE_FINAL_POLICY})

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._local_chat_input_event_adapter = None
        self._voice_input_final_event_adapter = None

    def bind_local_chat(self, input_event_adapter: object) -> None:
        self._bind_exact_adapter(
            input_event_adapter,
            self._LOCAL_CHAT_POLICY,
            "_local_chat_input_event_adapter",
        )

    def bind_voice_final(self, input_event_adapter: object) -> None:
        self._bind_exact_adapter(
            input_event_adapter,
            self._VOICE_FINAL_POLICY,
            "_voice_input_final_event_adapter",
        )

    def validate_invocation(
        self,
        *,
        input_event_adapter: object,
        source_policy: InputProviderSourcePolicy,
    ) -> None:
        if type(source_policy) is not InputProviderSourcePolicy:
            raise TypeError("source_policy must be InputProviderSourcePolicy")
        policy_tuple = (
            source_policy.source,
            source_policy.provider_id,
            source_policy.event_kind,
            source_policy.final,
        )
        if policy_tuple not in self._CANONICAL_POLICIES:
            raise ValueError("source_policy is not a trusted user ingress tuple")
        self._validate_exact_adapter(input_event_adapter, policy_tuple)
        if getattr(input_event_adapter, "policy", None) is not source_policy:
            raise ValueError("source_policy must be the adapter's exact policy")
        with self._lock:
            if policy_tuple == self._LOCAL_CHAT_POLICY:
                bound_adapter = self._local_chat_input_event_adapter
            else:
                bound_adapter = self._voice_input_final_event_adapter
            if input_event_adapter is not bound_adapter:
                raise ValueError(
                    "input_event_adapter is not the bound trusted instance"
                )

    def _bind_exact_adapter(
        self,
        input_event_adapter: object,
        policy_tuple: tuple[object, ...],
        attribute_name: str,
    ) -> None:
        self._validate_exact_adapter(input_event_adapter, policy_tuple)
        policy = getattr(input_event_adapter, "policy", None)
        if type(policy) is not InputProviderSourcePolicy or (
            policy.source,
            policy.provider_id,
            policy.event_kind,
            policy.final,
        ) != policy_tuple:
            raise ValueError("input_event_adapter policy is not canonical")
        with self._lock:
            bound_adapter = getattr(self, attribute_name)
            if bound_adapter is None:
                setattr(self, attribute_name, input_event_adapter)
                return
            if bound_adapter is not input_event_adapter:
                raise RuntimeError(
                    "trusted input adapter identity is already bound"
                )

    @staticmethod
    def _validate_exact_adapter(
        input_event_adapter: object,
        policy_tuple: tuple[object, ...],
    ) -> None:
        from input_core.input_event.adapters.local_chat_input_event_adapter import (
            LocalChatInputEventAdapter,
        )
        from input_core.input_event.adapters.provider_bound_input_event_adapter import (
            ProviderBoundInputEventAdapter,
        )

        if policy_tuple == TrustedIngressAdapterBindingRegistry._LOCAL_CHAT_POLICY:
            expected_type = LocalChatInputEventAdapter
        else:
            expected_type = ProviderBoundInputEventAdapter
        if type(input_event_adapter) is not expected_type:
            raise TypeError("input_event_adapter type is not trusted")
        adapt = getattr(input_event_adapter, "adapt", None)
        if (
            not callable(adapt)
            or getattr(adapt, "__self__", None) is not input_event_adapter
            or getattr(adapt, "__func__", None) is not expected_type.adapt
        ):
            raise TypeError("input_event_adapter.adapt binding is not trusted")


__all__ = ("TrustedIngressAdapterBindingRegistry",)
