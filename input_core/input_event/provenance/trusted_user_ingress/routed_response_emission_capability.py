#20260905_kpopmodder: Carries one registry-authorized routed-response emission commit.
from __future__ import annotations

import hashlib
import threading


_ISSUANCE_TOKEN = object()


class RoutedResponseEmissionCapability:
    __slots__ = (
        "_event",
        "_event_signature",
        "_lock",
        "_registry_token",
        "_response_kind",
        "_source",
        "_spent",
        "_text_digest",
        "_text_length",
    )

    def __init__(
        self,
        *,
        registry_token: object,
        event: object,
        event_signature: tuple[object, ...],
        text: str,
        source: str,
        response_kind: str,
        _issuance_token: object = None,
    ):
        if _issuance_token is not _ISSUANCE_TOKEN:
            raise TypeError(
                "RoutedResponseEmissionCapability is issued only by trusted ingress"
            )
        self._registry_token = registry_token
        self._event = event
        self._event_signature = event_signature
        self._text_length = len(text)
        self._text_digest = hashlib.sha256(text.encode("utf-8")).digest()
        self._source = source
        self._response_kind = response_kind
        self._lock = threading.Lock()
        self._spent = False

    @classmethod
    def _issue(
        cls,
        *,
        registry_token: object,
        event: object,
        event_signature: tuple[object, ...],
        text: str,
        source: str,
        response_kind: str,
    ) -> "RoutedResponseEmissionCapability":
        return cls(
            registry_token=registry_token,
            event=event,
            event_signature=event_signature,
            text=text,
            source=source,
            response_kind=response_kind,
            _issuance_token=_ISSUANCE_TOKEN,
        )

    def _consume(
        self,
        *,
        registry_token: object,
        event: object,
        text: str,
        source: str,
        response_kind: str,
    ) -> bool:
        if (
            registry_token is not self._registry_token
            or event is not self._event
            or not self._event_matches(event)
            or type(text) is not str
            or len(text) != self._text_length
            or hashlib.sha256(text.encode("utf-8")).digest()
            != self._text_digest
            or source != self._source
            or response_kind != self._response_kind
        ):
            return False
        with self._lock:
            if self._spent:
                return False
            self._spent = True
            return True

    def _event_matches(self, event: object) -> bool:
        try:
            text = event.text
            signature = (
                event.source,
                event.provider_id,
                event.event_kind,
                event.final,
                event.event_id,
                len(text),
                hashlib.sha256(text.encode("utf-8")).digest(),
            )
        except Exception:
            return False
        return signature == self._event_signature

    @property
    def spent(self) -> bool:
        with self._lock:
            return self._spent

    def __repr__(self) -> str:
        return "RoutedResponseEmissionCapability(<opaque>)"

    def __copy__(self):
        raise TypeError("routed_response_emission_capability_is_not_copyable")

    def __deepcopy__(self, _memo):
        raise TypeError("routed_response_emission_capability_is_not_copyable")

    def __reduce__(self):
        raise TypeError("routed_response_emission_capability_is_not_serializable")

    def __reduce_ex__(self, _protocol):
        raise TypeError("routed_response_emission_capability_is_not_serializable")


__all__ = ("RoutedResponseEmissionCapability",)
