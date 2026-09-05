#20260905_kpopmodder: Owns callback-owner token registration and exact authorization.
from __future__ import annotations

import threading


class TrustedIngressCallbackInvocationOwnerRegistry:
    def __init__(self) -> None:
        self._owner_tokens = {}
        self._lock = threading.Lock()

    def claim(self, input_event_adapter: object) -> object:
        owner_token = object()
        with self._lock:
            if input_event_adapter in self._owner_tokens:
                raise RuntimeError(
                    "trusted callback invocation owner is already bound"
                )
            self._owner_tokens[input_event_adapter] = owner_token
        return owner_token

    def is_claimed(self, input_event_adapter: object) -> bool:
        with self._lock:
            return input_event_adapter in self._owner_tokens

    def authorize(
        self,
        input_event_adapter: object,
        owner_token: object,
    ) -> None:
        with self._lock:
            expected_token = self._owner_tokens.get(input_event_adapter)
            if owner_token is not expected_token:
                raise PermissionError(
                    "trusted callback invocation ownership is invalid"
                )


__all__ = ("TrustedIngressCallbackInvocationOwnerRegistry",)
