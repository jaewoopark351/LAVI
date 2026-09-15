#20260915_kpopmodder: Negative operational cases preserve zero submissions and STOP precedence.
import pytest
from ..confirmation.trusted_route_fixture import TrustedConfirmationRouteFixture
from .catalogue_fixture import catalogue
from .command_cases import COMMAND_CASES


@pytest.mark.parametrize("voice", (False, True), ids=("chat", "final_mic"))
@pytest.mark.parametrize(
    "name,text,expected,confirmation",
    COMMAND_CASES,
    ids=[row[0] for row in COMMAND_CASES],
)
def test_question_form_does_not_submit_any_registered_command(
    name, text, expected, confirmation, voice
):
    f = TrustedConfirmationRouteFixture(catalogue())
    f.dispatch(text + "?", voice=voice)
    assert f.adapter.requests == []
    assert f.adapter.stop_requests == []


@pytest.mark.parametrize("voice", (False, True), ids=("chat", "final_mic"))
@pytest.mark.parametrize(
    "name,text,expected,confirmation",
    COMMAND_CASES,
    ids=[row[0] for row in COMMAND_CASES],
)
def test_existing_busy_owner_is_preserved_for_every_command_except_priority_stop(
    name, text, expected, confirmation, voice
):
    f = TrustedConfirmationRouteFixture(catalogue())
    f.adapter.busy = "already-running"
    f.dispatch(text, voice=voice)
    assert f.adapter.requests == []
    assert len(f.adapter.stop_requests) == int(name == "stop")
    assert f.adapter.busy == "already-running"


@pytest.mark.parametrize("voice", (False, True), ids=("chat", "final_mic"))
@pytest.mark.parametrize(
    "text",
    (
        "좀비 세 마리 공격하지 마",
        "기억 초기화가 뭐야?",
        "철괴 1.5개 구해 줘",
        "철괴 1개 2개 구해 줘",
        "철괴 0개 보관해 줘",
        "철괴 2147483647개와 철괴 한 개 구해 줘",
        "밝기를 NaN으로 설정해 줘",
        "밝기를 1e999로 설정해 줘",
        "Alex에게 철괴 두 개와 금괴 한 개 줘",
        "플레이어 Alex 공격해 줘",
        "요새 찾아가 줘",
        "오버레이 꺼 줘 그리고 철괴 구해 줘",
        '"챗클레프 꺼 줘"라고 말했어',
    ),
)
def test_unsafe_ambiguous_invalid_quantity_and_combined_inputs_never_submit(
    text, voice
):
    f = TrustedConfirmationRouteFixture(catalogue())
    f.dispatch(text, voice=voice)
    assert f.adapter.requests == [] and f.adapter.stop_requests == []
