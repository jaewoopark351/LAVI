#20260905_kpopmodder: Bind only the exact VoiceInput-final callback to trusted queue ownership.
from __future__ import annotations


class TrustedVoiceInputWiring:
    PROVIDER_ID = "VoiceInput"

    def wire(self, *, input_component, llm):
        bind_listener = getattr(
            input_component,
            "bind_provider_input_event_listener",
            None,
        )
        create_coordinator = getattr(
            llm,
            "create_trusted_voice_input_final_enqueue_coordinator",
            None,
        )
        if not callable(bind_listener) or not callable(create_coordinator):
            return None

        def build_listener(input_event_adapter):
            return create_coordinator(
                input_event_adapter,
                pre_accept_observers=(
                    lambda event: input_component.send_output(
                        event,
                        excluded_listeners=(llm.receive_input,),
                    ),
                ),
            )

        return bind_listener(self.PROVIDER_ID, build_listener)


__all__ = ("TrustedVoiceInputWiring",)
