#20260705_kpopmodder: Added this helper to keep ScreenVision observation decision logging outside the facade.
from core.logger import log_print


class ScreenObservationDecisionReporter:
    #20260705_kpopmodder: Preserve existing decision metadata/log text while ScreenVision stays the facade.
    def __init__(self, record_raw_event_callback, live_textbox):
        self.record_raw_event_callback = record_raw_event_callback
        self.live_textbox = live_textbox

    def record_ignored(
        self,
        decision,
        raw_observation,
        event_source,
        question,
        last_auto_observation="",
        metadata=None,
    ):
        event_metadata = {
            "normalized_observation": decision.observation,
            "reason": decision.reason,
            "detail": decision.detail,
            "question": question,
        }
        event_metadata.update(metadata or {})

        if decision.reason == "duplicate/similar":
            event_metadata["last_auto_observation"] = last_auto_observation

        self.record_raw_event_callback(
            event_type="screen_observation_ignored",
            value=raw_observation,
            source=event_source,
            metadata=event_metadata,
        )

    def record_decision(
        self,
        decision,
        raw_observation,
        event_source,
        question,
        last_auto_observation="",
        metadata=None,
    ):
        event_metadata = {
            "accepted": decision.accepted,
            "normalized_observation": decision.observation,
            "reason": decision.reason,
            "detail": decision.detail,
            "question": question,
        }
        event_metadata.update(metadata or {})

        if decision.reason == "duplicate/similar":
            event_metadata["last_auto_observation"] = last_auto_observation

        self.record_raw_event_callback(
            event_type="screen_observation_decision",
            value=raw_observation,
            source=event_source,
            metadata=event_metadata,
        )

    def format_detail(self, decision):
        return decision.detail or "no_detail"

    def report_ignored(self, decision, label):
        observation = decision.observation
        detail = self.format_detail(decision)

        if decision.reason == "broken/noise":
            log_print(
                f"[ScreenVision] {label} ignored: "
                f"broken/noise detail={detail} observation={observation!r}"
            )
            self.live_textbox.print(
                f"[ScreenVision] {label} observation ignored: "
                f"broken/noise ({detail}): {observation}"
            )
        elif decision.reason == "no_important_change":
            self.live_textbox.print(
                "[ScreenVision] No important change. "
                f"Skip LLM/TTS. ({detail})"
            )
        elif decision.reason == "duplicate/similar":
            log_print(
                f"[ScreenVision] {label} ignored: "
                f"duplicate/similar detail={detail} observation={observation!r}"
            )
            self.live_textbox.print(
                f"[ScreenVision] {label} observation ignored: "
                f"duplicate/similar ({detail}): {observation}"
            )
        elif decision.reason == "ai_speaking":
            self.live_textbox.print(
                f"[ScreenVision] {label} observation skipped: AI is speaking. "
                f"({detail})"
            )

    def report_accepted(self, decision, label):
        detail = self.format_detail(decision)
        log_print(
            f"[ScreenVision] {label} accepted: "
            f"detail={detail} observation={decision.observation!r}"
        )
        self.live_textbox.print(
            f"[ScreenVision] {label} observation accepted: {detail}"
        )
