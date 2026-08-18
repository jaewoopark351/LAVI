#20260818_kpopmodder: Submit one approved command once and never infer a safe replay.
#20260819_kpopmodder: Reuse the production canonical result boundary for supervised submits.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.routing.submission.submission_result_normalizer import (
    MinecraftChatClefSubmissionResultNormalizer,
)

from ..observation.live_run_observation import (
    new_live_run_observation,
)
from .submission_result_observation import (
    apply_submission_result_to_observation,
)


_RESULT_NORMALIZER = MinecraftChatClefSubmissionResultNormalizer()


def submit_command_once(gateway: object, command: str) -> dict[str, object]:
    observation = new_live_run_observation()
    try:
        payload = gateway.submit_korean_command(command)
    except Exception as error:
        result = _RESULT_NORMALIZER.unknown(
            "",
            "Fabric ChatClef supervised submit call outcome is unknown: "
            f"{type(error).__name__}: {error}",
        )
    else:
        result = _RESULT_NORMALIZER.normalize(
            payload,
            expected_request_id=None,
        )
    observation["gradio_submit_call_count"] = _call_count(gateway)
    return apply_submission_result_to_observation(observation, result)


def _call_count(gateway: object) -> int:
    try:
        return int(getattr(gateway, "submit_call_count", 0))
    except (TypeError, ValueError):
        return 0
