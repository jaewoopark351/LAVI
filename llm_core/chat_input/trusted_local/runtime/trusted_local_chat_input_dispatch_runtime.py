#20260905_kpopmodder: Sequence one trusted local-Chat callback invocation.
from __future__ import annotations


class TrustedLocalChatInputDispatchRuntime:
    def __init__(
        self,
        *,
        delivery_factory,
        fallback_dispatcher,
        registered_dispatcher,
    ):
        self._delivery_factory = delivery_factory
        self._fallback_dispatcher = fallback_dispatcher
        self._registered_dispatcher = registered_dispatcher

    def dispatch(self, message, history, system_prompt):
        attempt = self._delivery_factory.create(message)
        if attempt.delivery is None:
            yield from self._fallback_dispatcher.dispatch(
                attempt.registrar,
                history,
                system_prompt,
            )
            return
        yield from self._registered_dispatcher.dispatch(
            attempt.delivery,
            history,
            system_prompt,
        )


__all__ = ("TrustedLocalChatInputDispatchRuntime",)
