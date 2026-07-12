#20260705_kpopmodder: Added this helper to isolate ScreenVision analyze-to-result-flow orchestration.


class ScreenObservationAnalysisHelper:
    #20260705_kpopmodder: This helper preserves the existing analyzer and decision flow callbacks.
    def __init__(self, analyze_callback, result_flow_provider):
        self.analyze_callback = analyze_callback
        self.result_flow_provider = result_flow_provider

    def analyze_and_process(
        self,
        image,
        question,
        event_source,
        label,
        decision_kwargs=None,
        raw_metadata=None,
        decision_metadata=None,
        print_observation_prefix=None,
        analyze_kwargs=None,
    ):
        raw_observation = self.analyze_callback(
            image=image,
            question=question,
            **(analyze_kwargs or {}),
        )

        if raw_observation is None:
            return None

        return self.result_flow_provider().process(
            raw_observation=raw_observation,
            event_source=event_source,
            question=question,
            label=label,
            decision_kwargs=decision_kwargs,
            raw_metadata=raw_metadata,
            decision_metadata=decision_metadata,
            print_observation_prefix=print_observation_prefix,
        )
