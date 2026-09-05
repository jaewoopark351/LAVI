#20260905_kpopmodder: Own proof-bound response capability authorization.
from __future__ import annotations

from dataclasses import replace


class TrustedKoreanResponseAuthorizer:
    _RESPONSE_SOURCE = "minecraft_chatclef"

    def __init__(self, *, owner: object):
        self._owner = owner

    def authorize(self, *, decision, event: object, proof: object):
        if not decision.handled:
            return decision
        if not str(decision.response_text or "").strip():
            return self._suppressed(decision)

        capability = proof.issue_response_emission_capability(
            event,
            self._owner,
            text=str(decision.response_text),
            source=self._RESPONSE_SOURCE,
        )
        if capability is None:
            return self._suppressed(decision, clear_text=True)
        return replace(
            decision,
            publish_external_response=True,
            response_source=self._RESPONSE_SOURCE,
            response_emission_capability=capability,
            suppress_response=False,
        )

    @staticmethod
    def _suppressed(decision, *, clear_text: bool = False):
        return replace(
            decision,
            response_text="" if clear_text else decision.response_text,
            publish_external_response=False,
            response_emission_capability=None,
            suppress_response=True,
        )


__all__ = ("TrustedKoreanResponseAuthorizer",)
