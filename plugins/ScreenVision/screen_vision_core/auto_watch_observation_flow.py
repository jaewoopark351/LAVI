#20260706_kpopmodder: Split Auto Watch observation flow from the ScreenVision facade.
import time


class ScreenAutoWatchObservationFlow:
    #20260706_kpopmodder: Owns Auto Watch analyze/filter/save sequencing without changing callbacks.
    def __init__(self, owner):
        self.owner = owner

    def analyze_and_save(self, image, difference, question, source):
        owner = self.owner
        if not owner.analysis_lock.acquire(blocking=False):
            owner.live_textbox.print(
                "[ScreenVision] Screen change skipped: analysis is busy."
            )
            return

        try:
            result = owner._get_observation_analysis_helper().analyze_and_process(
                image=image,
                event_source="auto_watch",
                question=question,
                label="Auto",
                decision_kwargs={
                    "reject_no_important_change": True,
                    "check_duplicate": True,
                    "check_ai_speaking": True,
                    "check_cooldown": True,
                },
                raw_metadata={
                    "difference": difference,
                },
                print_observation_prefix="[ScreenVision] Auto observation",
            )
            if result is None:
                return

            observation = result.observation
            decision = result.decision

            if not decision.accepted:
                return

            owner.last_auto_observation = observation
            owner.last_auto_sent_time = time.time()

            owner.remember_screen_observation(
                observation=observation,
                question=question,
                source=source,
            )
            owner.observation_memory_dispatch.publish_observation_event(
                observation=observation,
                question=question,
                source=source,
            )

            owner.live_textbox.print(
                "[ScreenVision] Auto observation saved silently. Ask about the screen to use it."
            )
            return

        finally:
            owner.analysis_lock.release()
