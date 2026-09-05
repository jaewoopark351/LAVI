#20260905_kpopmodder: Delegate trusted VoiceInput-final coordinator creation.
from __future__ import annotations


class LlmTrustedVoiceEnqueueFacade:
    def __init__(self, voice_enqueue_factory):
        self._voice_enqueue_factory = voice_enqueue_factory

    def create(
        self,
        input_event_adapter,
        *,
        pre_accept_observers=(),
        post_accept_observers=(),
    ):
        return self._voice_enqueue_factory.create(
            input_event_adapter,
            pre_accept_observers=pre_accept_observers,
            post_accept_observers=post_accept_observers,
        )


__all__ = ("LlmTrustedVoiceEnqueueFacade",)
