#20260915_kpopmodder: Verify confirmation through actual Chat/final-mic ingress and the single ordinary submission path.
import pytest
from .trusted_route_fixture import TrustedConfirmationRouteFixture


@pytest.mark.parametrize("voice", (False, True))
def test_follow_requires_confirmation_then_submits_original_binding_once(voice):
    f = TrustedConfirmationRouteFixture()
    first, _ = f.dispatch("Alex 따라가줘", voice=voice)
    assert len(first) == 1 and "확인" in first[0]
    assert f.adapter.requests == []
    second, queued = f.dispatch("확인", voice=voice)
    assert len(f.adapter.requests) == 1
    request = f.adapter.requests[0]
    assert request.command == "follow Alex"
    assert request.source == ("voice_input_final" if voice else "lavi_chat_ui")
    assert request.metadata["natural_language"]["original_text"] == "Alex 따라가줘"
    binding = request.metadata["korean_confirmation"]
    assert binding["original_event_id"] == request.metadata["input_event"]["event_id"]
    assert binding["confirmation_event_id"] != binding["original_event_id"]
    assert binding["expected_session_id"] == f.adapter.session
    assert binding["expected_generation"] == f.adapter.generation
    assert len(second) == 1 and len(f.outputs) == 2
    assert f.pipeline.provider_calls == 0
    if queued is not None:
        list(f.llm.accept_queued_input(queued, [], "system"))
    f.dispatch("확인", voice=voice)
    assert len(f.adapter.requests) == 1


@pytest.mark.parametrize("voice", (False, True))
@pytest.mark.parametrize(
    "changed", ("session", "generation", "busy", "disconnected", "cancel", "stop")
)
def test_changed_conditions_cancel_or_stop_never_submit_waiting_command(voice, changed):
    f = TrustedConfirmationRouteFixture()
    f.dispatch("Alex 따라가줘", voice=voice)
    if changed == "session":
        f.adapter.session = "new-session"
    if changed == "generation":
        f.adapter.generation = 2
    if changed == "busy":
        f.adapter.busy = "another-request"
    if changed == "disconnected":
        f.adapter.connected = False
    if changed == "cancel":
        f.dispatch("취소", voice=voice)
    if changed == "stop":
        f.dispatch("멈춰줘", voice=voice)
    f.dispatch("확인", voice=voice)
    assert f.adapter.requests == []
    assert f.pipeline.provider_calls == 0


def test_confirmation_from_other_source_does_not_consume_chat_request():
    f = TrustedConfirmationRouteFixture()
    f.dispatch("Alex 따라가줘")
    f.dispatch("확인", voice=True)
    assert f.adapter.requests == []
    f.dispatch("확인")
    assert [row.command for row in f.adapter.requests] == ["follow Alex"]


def test_same_words_in_new_events_can_be_confirmed_again():
    f = TrustedConfirmationRouteFixture()
    for _ in range(2):
        f.dispatch("Alex 따라가줘")
        f.dispatch("확인")
    assert [row.command for row in f.adapter.requests] == ["follow Alex", "follow Alex"]
    assert (
        len({row.metadata["input_event"]["event_id"] for row in f.adapter.requests})
        == 2
    )


@pytest.mark.parametrize("voice", (False, True))
@pytest.mark.parametrize("text", ("멈춰?", "멈추지 마"))
def test_guarded_stop_question_or_negation_does_not_cancel_pending(voice, text):
    f = TrustedConfirmationRouteFixture()
    f.dispatch("Alex 따라가줘", voice=voice)
    f.dispatch(text, voice=voice)
    assert f.adapter.requests == []
    assert f.adapter.stop_requests == []
    f.dispatch("확인", voice=voice)
    assert [row.command for row in f.adapter.requests] == ["follow Alex"]
