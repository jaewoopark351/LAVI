#20260905_kpopmodder: Preserves the Korean eligibility-proof compatibility API.
from __future__ import annotations

from .proof import KoreanChatMicrophoneProofComponentGraph


_ISSUANCE_TOKEN = object()


class KoreanChatMicrophoneEligibilityProof:
    __slots__ = ("_components",)

    def __init__(
        self,
        *,
        event: object,
        consumed_evidence: object,
        owner: object,
        _issuance_token: object = None,
    ):
        if _issuance_token is not _ISSUANCE_TOKEN:
            raise TypeError(
                "KoreanChatMicrophoneEligibilityProof is admission-issued only"
            )
        self._components = KoreanChatMicrophoneProofComponentGraph(
            event=event,
            consumed_evidence=consumed_evidence,
            owner=owner,
        )

    @classmethod
    def _issue(
        cls,
        *,
        event: object,
        consumed_evidence: object,
        owner: object,
    ):
        return cls(
            event=event,
            consumed_evidence=consumed_evidence,
            owner=owner,
            _issuance_token=_ISSUANCE_TOKEN,
        )

    @property
    def is_open(self) -> bool:
        return not self._components.lifecycle.closed

    @property
    def _event(self):
        return self._components.lifecycle.event

    @property
    def _consumed_evidence(self):
        return self._components.lifecycle.consumed_evidence

    @property
    def _owner(self):
        return self._components.lifecycle.owner

    @property
    def _event_signature(self):
        return self._components.lifecycle.event_signature

    @property
    def _lock(self):
        return self._components.lifecycle.lock

    @property
    def _closed(self):
        return self._components.lifecycle.closed

    def matches_event(self, event: object, owner: object = None) -> bool:
        return self._components.lifecycle.matches_event(event, owner)

    def issue_response_emission_capability(
        self,
        event: object,
        owner: object,
        *,
        text: str,
        source: str,
        response_kind: str = "immediate",
    ):
        return self._components.response_capability_issuer.issue(
            event,
            owner,
            text=text,
            source=source,
            response_kind=response_kind,
        )

    def close(self) -> bool:
        return self._components.lifecycle.close()

    def __copy__(self):
        raise TypeError("KoreanChatMicrophoneEligibilityProof cannot be copied")

    def __deepcopy__(self, _memo):
        raise TypeError("KoreanChatMicrophoneEligibilityProof cannot be copied")

    def __reduce__(self):
        raise TypeError("KoreanChatMicrophoneEligibilityProof cannot be serialized")

    def __reduce_ex__(self, _protocol):
        raise TypeError("KoreanChatMicrophoneEligibilityProof cannot be serialized")

__all__ = ("KoreanChatMicrophoneEligibilityProof",)
