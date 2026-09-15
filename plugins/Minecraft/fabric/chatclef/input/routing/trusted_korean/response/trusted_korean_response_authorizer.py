#20260905_kpopmodder: Own proof-bound response capability authorization.
from __future__ import annotations

from dataclasses import replace

from .coalesced.trusted_korean_coalesced_response_binding import TrustedKoreanCoalescedResponseBinding


class TrustedKoreanResponseAuthorizer:
    _RESPONSE_SOURCE = "minecraft_chatclef"

    def __init__(self, *, owner: object):
        self._owner = owner

    def authorize(self, *, decision, event: object, proof: object):
        if not decision.handled:
            return decision
        if not str(decision.response_text or "").strip():
            if (
                decision.suppress_response is True
                and decision.publish_external_response is False
                and decision.response_emission_capability is None
            ):
                return decision
            return self._suppressed(decision)

        binding = TrustedKoreanCoalescedResponseBinding.create(decision, event)
        selection_args = {} if binding is None else {"deferred_selection": binding.selection()}
        capability = proof.issue_response_emission_capability(
            event,
            self._owner,
            text=str(decision.response_text),
            source=self._RESPONSE_SOURCE,
            response_kind=str(decision.response_kind or "immediate"),
            **selection_args,
        )
        if capability is None:
            return self._suppressed(decision, clear_text=True)
        authorized = replace(
            decision,
            publish_external_response=True,
            response_source=self._RESPONSE_SOURCE,
            response_emission_capability=capability,
            suppress_response=False,
        )
        if binding is not None:
            binding.bind_decision(authorized)
        return authorized

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
