#20260818_kpopmodder: Collect explicit operator gameplay evidence between batch commands.
from __future__ import annotations

from collections.abc import Callable, Mapping


def collect_console_gameplay_checkpoint(
    environment: Mapping[str, object],
    command_result: Mapping[str, object],
    *,
    reader: Callable[[str], str] = input,
    writer: Callable[[str], None] = print,
) -> dict[str, bool]:
    observation = command_result.get("observation")
    terminal_status = (
        str(observation.get("terminal_status") or "unknown")
        if isinstance(observation, Mapping)
        else "unknown"
    )
    writer("")
    writer(f"명령 종료 확인: {environment.get('command')}")
    writer(f"runtime terminal status: {terminal_status}")
    writer("다음 명령 전 Minecraft 화면과 인벤토리를 직접 확인해 주세요.")
    observation_complete = _read_yes_no(
        "필요한 gameplay 관찰 범위를 모두 확인했나요? [y/n]: ",
        reader,
        writer,
    )
    expected_verified = _read_yes_no(
        "예상한 아이템 획득 효과가 확인됐나요? [y/n]: ",
        reader,
        writer,
    )
    partial_observed = _read_yes_no(
        "부분 실행 또는 미완료 효과가 관찰됐나요? [y/n]: ",
        reader,
        writer,
    )
    unexpected_observed = _read_yes_no(
        "예상하지 않은 이동·블록·인벤토리 변화가 관찰됐나요? [y/n]: ",
        reader,
        writer,
    )
    prohibited_absence_verified = _read_yes_no(
        "금지된 효과가 없음을 관찰 범위 안에서 확인했나요? [y/n]: ",
        reader,
        writer,
    )
    return {
        "gameplay_observation_complete": observation_complete,
        "gameplay_effect_observed": (
            expected_verified or partial_observed or unexpected_observed
        ),
        "expected_gameplay_effect_verified": expected_verified,
        "partial_gameplay_effect_observed": partial_observed,
        "unexpected_effect_observed": unexpected_observed,
        "prohibited_effect_absence_verified": prohibited_absence_verified,
    }


def _read_yes_no(
    prompt: str,
    reader: Callable[[str], str],
    writer: Callable[[str], None],
) -> bool:
    while True:
        answer = reader(prompt).strip().lower()
        if answer in {"y", "yes", "예", "네"}:
            return True
        if answer in {"n", "no", "아니요", "아니오"}:
            return False
        writer("y 또는 n으로 입력해 주세요.")
