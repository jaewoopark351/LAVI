#20260915_kpopmodder: Exercise one-use immutable confirmation, revalidation, cleanup and diagnostic non-interference.
from dataclasses import FrozenInstanceError
from copy import deepcopy
import pickle
import pytest
from .owner_fixture import fixture, begin, confirm, request, event


def test_pending_submits_nothing_and_exact_confirmation_receipt_commits_once():
    f = fixture()
    assert begin(f).reason == "minecraft_confirmation_required"
    receipt, decision = confirm(f)
    assert decision is None
    assert receipt.pending.event is f.original
    assert f.owner.accepts_translation(
        receipt, event=f.original, translation=f.translation
    )
    assert receipt.commit(request(receipt))
    assert not receipt.commit(request(receipt))
    assert not receipt.is_live(f.original)
    assert f.owner._pending == {} and f.owner._receipts == {}
    assert "reason=committed" in " ".join(f.messages)
    assert f.original.text not in " ".join(f.messages)


@pytest.mark.parametrize(
    "mutation", ("session", "generation", "busy", "expired", "trust")
)
def test_changes_while_waiting_reject_without_receipt(mutation):
    f = fixture()
    begin(f)
    if mutation == "session":
        f.state.session = "new-session"
    if mutation == "generation":
        f.state.generation = 2
    if mutation == "busy":
        f.state.ready = False
    if mutation == "expired":
        f.state.time = 70
    if mutation == "trust":
        f.state.trusted = False
    receipt, decision = confirm(f)
    assert receipt is None and decision.handled


@pytest.mark.parametrize(
    "field", ("command", "source", "event_id", "provider_id", "event_kind", "final")
)
def test_commit_rechecks_exact_request_binding(field):
    f = fixture()
    begin(f)
    receipt, _ = confirm(f)
    value = request(receipt)
    if field in {"command", "source"}:
        setattr(value, field, "foreign")
    else:
        value.metadata["input_event"][field] = "foreign"
    assert not receipt.commit(value)
    assert not receipt.is_live(f.original)


def test_session_or_trust_change_after_reply_before_commit_is_rejected():
    for change in ("generation", "trusted", "ready"):
        f = fixture()
        begin(f)
        receipt, _ = confirm(f)
        setattr(f.state, change, 2 if change == "generation" else False)
        assert not receipt.commit(request(receipt))


def test_translation_change_is_not_authorized_and_bound_fields_are_immutable():
    f = fixture()
    begin(f)
    receipt, _ = confirm(f)
    changed = deepcopy(f.translation)
    changed["command"] = "follow Steve"
    assert not f.owner.accepts_translation(
        receipt, event=f.original, translation=changed
    )
    with pytest.raises(FrozenInstanceError):
        receipt.pending.command = "gamer"
    with pytest.raises(FrozenInstanceError):
        receipt.pending = object()
    with pytest.raises(TypeError):
        pickle.dumps(receipt)


def test_other_source_cannot_consume_request_and_cancellation_removes_only_own_pending():
    f = fixture()
    begin(f)
    receipt, missing = confirm(f, source="voice_input_final")
    assert receipt is None and missing.reason == "confirmation_missing"
    assert "lavi_chat_ui" in f.owner._pending
    receipt, cancelled = confirm(f, text="취소")
    assert receipt is None and cancelled.reason == "confirmation_cancelled"
    assert confirm(f)[1].reason == "confirmation_missing"


@pytest.mark.parametrize(
    "text",
    (
        "확인?",
        "확인하지 마",
        "확인이 뭐야?",
        "확인 그리고 공격해",
        "@확인",
        "확인\n",
        '"확인"',
        "오늘 날씨 어때",
    ),
)
def test_questions_negation_quotes_and_unrelated_speech_do_not_confirm(text):
    f = fixture()
    begin(f)
    assert confirm(f, text=text) == (None, None)
    assert len(f.owner._pending) == 1


def test_interim_transcript_or_spoofed_proof_never_confirms():
    f = fixture()
    begin(f)
    row = event("확인", identity="b" * 32, final=False)
    receipt, decision = f.owner.reply(event=row, proof=row)
    assert receipt is None and decision.reason == "confirmation_trust_required"
    row = event("확인", identity="c" * 32)
    assert f.owner.reply(event=row, proof=object())[0] is None


def test_duplicate_original_id_rejected_and_stop_clears_all_pending_and_receipts():
    f = fixture()
    begin(f)
    assert (
        confirm(f, identity=f.original.event_id)[1].reason == "confirmation_duplicate"
    )
    begin(f)
    receipt, _ = confirm(f)
    begin(f, event(source="voice_input_final", identity="c" * 32))
    f.owner.cancel_all()
    assert not receipt.is_live(f.original)
    assert f.owner._pending == {} and f.owner._receipts == {}


def test_pending_bound_to_two_sources_and_expiry_does_not_add_unbounded_history():
    f = fixture()
    for index in range(40):
        begin(f, event(identity=f"{index:032x}"))
        begin(f, event(source="voice_input_final", identity=f"{index + 100:032x}"))
        assert len(f.owner._pending) == 2
    f.state.time = 80
    begin(f)
    assert len(f.owner._pending) == 1


def test_diagnostic_disabled_or_failing_preserves_confirmation_and_cleanup():
    def fail(_):
        raise RuntimeError("sink unavailable")

    for callback in (lambda _: None, fail):
        f = fixture(callback)
        begin(f)
        receipt, _ = confirm(f)
        assert receipt.commit(request(receipt))
        assert f.owner._receipts == {}


def test_actual_formatter_utf8_file_sink_carries_decision_values(tmp_path):
    import logging

    path = tmp_path / "confirmation.log"
    logger = logging.Logger("confirmation-test", level=logging.INFO)
    handler = logging.FileHandler(path, encoding="utf-8")
    handler.setFormatter(logging.Formatter("%(levelname)s %(message)s"))
    logger.addHandler(handler)
    f = fixture(logger.info)
    begin(f)
    receipt, _ = confirm(f)
    receipt.commit(request(receipt))
    handler.close()
    output = path.read_text(encoding="utf-8")
    assert "INFO event=korean_confirmation reason=pending" in output
    assert "reason=confirmed" in output and "reason=committed" in output
    assert "command=follow" in output and "pending=0" in output
    assert "receipt_live=true command_match=true source_match=true" in output
    assert "confirmation_binding_match=true input_binding_match=true" in output
    assert "Alex" not in output and f.original.text not in output
