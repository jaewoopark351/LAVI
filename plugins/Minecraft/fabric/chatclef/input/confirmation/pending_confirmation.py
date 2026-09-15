#20260915_kpopmodder: Freeze the exact original request without retaining a spent ingress proof.
from dataclasses import dataclass
import json


def semantic_translation(translation):
    return json.dumps(
        {
            key: translation.get(key)
            for key in (
                "command",
                "intent",
                "resolved_target",
                "data",
            )
        },
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
        allow_nan=False,
    )


@dataclass(frozen=True, slots=True)
class PendingKoreanCommandConfirmation:
    event: object
    command_text: str
    command: str
    semantic_json: str
    session: tuple[str, int]
    expires_at: float
    goto_input_binding: object = None
