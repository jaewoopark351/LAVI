#20260905_kpopmodder: Carry one immutable trusted VoiceInput-final binding.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class TrustedVoiceInputBinding:
    input_event_adapter: object
    producer_registrar_factory: object
    invocation_owner: object
    source_policy: object


__all__ = ("TrustedVoiceInputBinding",)
