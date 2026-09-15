#20260915_kpopmodder: Each canonical command traverses actual trusted Chat/final-mic ingress once.
import pytest
from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)
from ..confirmation.trusted_route_fixture import TrustedConfirmationRouteFixture
from .command_cases import COMMAND_CASES, OPTION_CASES
from .catalogue_fixture import catalogue


def test_every_registered_semantic_command_has_exactly_one_basic_route_case():
    actual = {row[0] for row in COMMAND_CASES}
    expected = {
        name
        for name in KoreanChatClefCommandRegistry().command_names()
        if name.isascii()
    }
    assert len(COMMAND_CASES) == len(actual) == 26 and actual == expected


@pytest.mark.parametrize("voice", (False, True), ids=("chat", "final_mic"))
@pytest.mark.parametrize(
    "name,text,expected,confirmation",
    COMMAND_CASES,
    ids=[row[0] for row in COMMAND_CASES],
)
def test_all_commands_submit_exact_canonical_once_after_required_confirmation(
    name, text, expected, confirmation, voice
):
    f = TrustedConfirmationRouteFixture(catalogue())
    response, queued = f.dispatch(text, voice=voice)
    if confirmation:
        assert f.adapter.requests == []
        assert len(response) == 1 and "확인" in response[0]
        response, queued = f.dispatch("확인", voice=voice)
    if name == "stop":
        assert f.adapter.requests == [] and len(f.adapter.stop_requests) == 1
        assert response == []  # Accepted STOP has terminal-only presentation.
    else:
        assert [row.command for row in f.adapter.requests] == [expected]
        assert f.adapter.requests[0].source == (
            "voice_input_final" if voice else "lavi_chat_ui"
        )
        assert len(response) == 1
    assert f.pipeline.provider_calls == 0
    if queued is not None:
        list(f.llm.accept_queued_input(queued, [], "system"))
        assert (
            len(f.adapter.stop_requests if name == "stop" else f.adapter.requests) == 1
        )


@pytest.mark.parametrize("voice", (False, True), ids=("chat", "final_mic"))
@pytest.mark.parametrize("text,expected,confirmation", OPTION_CASES)
def test_list_modes_optional_arguments_and_compatibility_alias_share_trusted_route(
    text, expected, confirmation, voice
):
    f = TrustedConfirmationRouteFixture(catalogue())
    f.dispatch(text, voice=voice)
    if confirmation:
        assert f.adapter.requests == []
        f.dispatch("확인", voice=voice)
    assert [row.command for row in f.adapter.requests] == [expected]
    assert f.pipeline.provider_calls == 0
