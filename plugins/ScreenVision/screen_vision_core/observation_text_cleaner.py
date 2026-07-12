#20260705_kpopmodder: Added this helper to keep ScreenVision observation text cleanup outside the facade.


class ScreenObservationTextCleaner:
    #20260705_kpopmodder: Delegate policy-owned normalization while preserving facade prefix stripping behavior.
    def __init__(self, observation_policy):
        self.observation_policy = observation_policy

    def normalize(self, observation):
        return self.observation_policy.normalize(observation)

    def normalize_for_decision(self, observation):
        return self.observation_policy.normalize_for_decision(observation)

    def strip_screen_prefix(self, text):
        text = str(text or "").strip()

        #20260709_kpopmodder: Keep Korean prefix stripping readable after mojibake cleanup.
        prefixes = (
            "현재 화면에는 ",
            "PC 화면에는 ",
            "화면에는 ",
            "현재 화면은 ",
            "PC 화면은 ",
            "화면은 ",
        )

        changed = True
        while changed:
            changed = False
            for prefix in prefixes:
                if text.startswith(prefix):
                    text = text[len(prefix):].strip()
                    changed = True

        return text
