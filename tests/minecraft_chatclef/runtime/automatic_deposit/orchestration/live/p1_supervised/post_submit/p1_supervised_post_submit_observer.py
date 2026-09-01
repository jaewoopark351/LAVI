#20260901_kpopmodder: Invoke one bounded post-submit log reader without submitting or reconciling.
from __future__ import annotations


def observe_p1_supervised_post_submit_log(
    reader: object,
    execution: object,
) -> tuple[object | None, str]:
    if not callable(reader):
        return None, "P1_SUPERVISED_POST_SUBMIT_READER_NOT_CALLABLE"
    try:
        value = reader(execution)
    except Exception as error:
        return None, f"P1_SUPERVISED_POST_SUBMIT_READER_FAILED:{type(error).__name__}"
    if type(value) is not tuple or len(value) != 2:
        return None, "P1_SUPERVISED_POST_SUBMIT_READER_RESULT_INVALID"
    evidence, reason = value
    return evidence, reason if _reason(reason) else ""


def _reason(value: object) -> bool:
    return (
        type(value) is str
        and value == value.strip()
        and 0 < len(value) <= 512
        and not any(
            ord(character) < 0x20 or ord(character) == 0x7F
            for character in value
        )
    )
