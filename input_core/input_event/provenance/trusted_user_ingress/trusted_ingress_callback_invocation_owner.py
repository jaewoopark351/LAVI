#20260905_kpopmodder: Owns one callback-bound path for issuing trusted ingress invocations.
from __future__ import annotations


class TrustedIngressCallbackInvocationOwner:
    def __init__(
        self,
        *,
        begin_owned_invocation_callback,
        input_event_adapter,
        source_policy,
        owner_token,
    ) -> None:
        if not callable(begin_owned_invocation_callback):
            raise TypeError("begin_owned_invocation_callback must be callable")
        if owner_token is None:
            raise ValueError("owner_token is required")
        self._begin_owned_invocation_callback = (
            begin_owned_invocation_callback
        )
        self._input_event_adapter = input_event_adapter
        self._source_policy = source_policy
        self._owner_token = owner_token

    def begin_invocation(self):
        return self._begin_owned_invocation_callback(
            input_event_adapter=self._input_event_adapter,
            source_policy=self._source_policy,
            owner_token=self._owner_token,
        )


__all__ = ("TrustedIngressCallbackInvocationOwner",)
