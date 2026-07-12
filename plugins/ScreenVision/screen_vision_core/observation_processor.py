from dataclasses import dataclass


@dataclass(frozen=True)
class ObservationDecision:
    observation: str
    accepted: bool
    reason: str = ""
    #20260623_kpopmodder: Keep the exact filter explanation for later tuning.
    detail: str = ""


class ScreenObservationProcessor:#20260621_kpopmodder
    """ScreenVision 관찰 결과의 공통 판정 순서를 담당한다."""

    REASON_BROKEN = "broken/noise"
    REASON_NO_IMPORTANT_CHANGE = "no_important_change"
    REASON_DUPLICATE = "duplicate/similar"
    REASON_AI_SPEAKING = "ai_speaking"
    REASON_COOLDOWN = "cooldown"

    def __init__(self, observation_policy):
        self.observation_policy = observation_policy

    def normalize(self, raw_observation):
        return self.observation_policy.normalize(raw_observation)

    #20260623_kpopmodder: Detail helpers must never break the original filter flow.
    def _policy_detail(self, method_name, *args):
        method = getattr(self.observation_policy, method_name, None)
        if method is None:
            return ""
        try:
            return method(*args)
        except Exception:
            return ""

    def _accepted_detail(
        self,
        reject_no_important_change,
        check_duplicate,
        is_ai_speaking_callback,
        can_send_callback,
        summary_detail="",
    ):
        checks = [self.REASON_BROKEN]
        if reject_no_important_change:
            checks.append(self.REASON_NO_IMPORTANT_CHANGE)
        if check_duplicate:
            checks.append(self.REASON_DUPLICATE)
        if is_ai_speaking_callback is not None:
            checks.append(self.REASON_AI_SPEAKING)
        if can_send_callback is not None:
            checks.append(self.REASON_COOLDOWN)
        detail = "passed checks: " + ", ".join(checks)
        if summary_detail:
            detail = detail + "; " + summary_detail
        return detail

    def evaluate(
        self,
        raw_observation,
        previous_observation="",
        reject_no_important_change=False,
        check_duplicate=False,
        is_ai_speaking_callback=None,
        can_send_callback=None,
    ):
        observation = self.normalize(raw_observation)

        if self.observation_policy.is_broken(observation):
            return ObservationDecision(
                observation=observation,
                accepted=False,
                reason=self.REASON_BROKEN,
                detail=self._policy_detail("describe_broken", observation),
            )

        if (
            reject_no_important_change
            and self.observation_policy.is_no_important_change(observation)
        ):
            return ObservationDecision(
                observation=observation,
                accepted=False,
                reason=self.REASON_NO_IMPORTANT_CHANGE,
                detail=self._policy_detail(
                    "describe_no_important_change",
                    observation,
                ),
            )

        if (
            check_duplicate
            and self.observation_policy.is_duplicate(
                previous_observation,
                observation,
            )
        ):
            return ObservationDecision(
                observation=observation,
                accepted=False,
                reason=self.REASON_DUPLICATE,
                detail=self._policy_detail(
                    "describe_duplicate",
                    previous_observation,
                    observation,
                ),
            )

        if (
            is_ai_speaking_callback is not None
            and is_ai_speaking_callback()
        ):
            return ObservationDecision(
                observation=observation,
                accepted=False,
                reason=self.REASON_AI_SPEAKING,
                detail="global_state IS_AI_SPEAKING=True",
            )

        if can_send_callback is not None and not can_send_callback():
            return ObservationDecision(
                observation=observation,
                accepted=False,
                reason=self.REASON_COOLDOWN,
                detail="can_send_callback returned False",
            )

        accepted_observation = self._policy_detail(
            "summarize_if_long",
            observation,
        ) or observation
        summary_detail = self._policy_detail(
            "describe_summary",
            observation,
            accepted_observation,
        )

        return ObservationDecision(
            observation=accepted_observation,
            accepted=True,
            detail=self._accepted_detail(
                reject_no_important_change=reject_no_important_change,
                check_duplicate=check_duplicate,
                is_ai_speaking_callback=is_ai_speaking_callback,
                can_send_callback=can_send_callback,
                summary_detail=summary_detail,
            ),
        )
