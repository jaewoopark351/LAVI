#20260905_kpopmodder: Owns normalized routed-or-LLM prediction dispatch outside the LLM facade.
from __future__ import annotations


class LlmPredictionDispatchCoordinator:
    def __init__(
        self,
        *,
        input_event_normalizer_callback,
        routed_dispatch_coordinator_callback,
        response_pipeline_callback,
        effective_system_prompt_callback,
    ) -> None:
        callbacks = (
            input_event_normalizer_callback,
            routed_dispatch_coordinator_callback,
            response_pipeline_callback,
            effective_system_prompt_callback,
        )
        if not all(callable(callback) for callback in callbacks):
            raise TypeError("prediction dispatch callbacks must be callable")
        self._input_event_normalizer_callback = input_event_normalizer_callback
        self._routed_dispatch_coordinator_callback = (
            routed_dispatch_coordinator_callback
        )
        self._response_pipeline_callback = response_pipeline_callback
        self._effective_system_prompt_callback = effective_system_prompt_callback

    def predict(
        self,
        message,
        history,
        system_prompt,
        *,
        trusted_ingress_evidence=None,
    ):
        input_event = self._input_event_normalizer_callback().normalize(message)
        routed_dispatch = self._routed_dispatch_coordinator_callback()
        outcome = routed_dispatch.dispatch(
            input_event,
            trusted_ingress_evidence=trusted_ingress_evidence,
        )
        if outcome.handled:
            if outcome.suppress_response:
                return
            yield routed_dispatch.prepare_response_for_yield(
                input_event,
                outcome.response,
            )
            return
        yield from self._response_pipeline_callback().predict(
            input_event.fallback_payload,
            history,
            self._effective_system_prompt_callback(system_prompt),
        )


__all__ = ("LlmPredictionDispatchCoordinator",)
