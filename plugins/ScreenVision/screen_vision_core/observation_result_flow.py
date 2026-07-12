#20260705_kpopmodder: Added this helper to keep ScreenVision observation result flow outside the facade.
from dataclasses import dataclass


@dataclass
class ScreenObservationFlowResult:
    observation: str
    decision: object
    accepted: bool


class ScreenObservationResultFlow:
    #20260705_kpopmodder: This helper only orchestrates existing callbacks; it does not change filtering policy.
    def __init__(
        self,
        normalize_callback,
        evaluate_callback,
        record_raw_event_callback,
        record_decision_callback,
        record_ignored_callback,
        report_ignored_callback,
        report_accepted_callback,
        live_textbox,
    ):
        self.normalize_callback = normalize_callback
        self.evaluate_callback = evaluate_callback
        self.record_raw_event_callback = record_raw_event_callback
        self.record_decision_callback = record_decision_callback
        self.record_ignored_callback = record_ignored_callback
        self.report_ignored_callback = report_ignored_callback
        self.report_accepted_callback = report_accepted_callback
        self.live_textbox = live_textbox

    def process(
        self,
        raw_observation,
        event_source,
        question,
        label,
        decision_kwargs=None,
        raw_metadata=None,
        decision_metadata=None,
        print_observation_prefix=None,
    ):
        observation = self.normalize_callback(raw_observation)
        self.record_raw_event_callback(
            event_type="screen_observation_raw",
            value=raw_observation,
            source=event_source,
            metadata=self._raw_metadata(
                observation=observation,
                question=question,
                extra_metadata=raw_metadata,
            ),
        )

        if print_observation_prefix:
            self.live_textbox.print(
                f"{print_observation_prefix}: {observation}"
            )

        decision = self.evaluate_callback(
            observation,
            **(decision_kwargs or {}),
        )
        self.record_decision_callback(
            decision=decision,
            raw_observation=raw_observation,
            event_source=event_source,
            question=question,
            metadata=decision_metadata,
        )

        if not decision.accepted:
            self.record_ignored_callback(
                decision=decision,
                raw_observation=raw_observation,
                event_source=event_source,
                question=question,
                metadata=decision_metadata,
            )
            self.report_ignored_callback(decision, label)
            return ScreenObservationFlowResult(
                observation=observation,
                decision=decision,
                accepted=False,
            )

        self.report_accepted_callback(decision, label)
        return ScreenObservationFlowResult(
            observation=observation,
            decision=decision,
            accepted=True,
        )

    def _raw_metadata(self, observation, question, extra_metadata=None):
        metadata = {
            "normalized_observation": observation,
            "stage": "captured",
            "question": question,
        }
        metadata.update(extra_metadata or {})
        return metadata
