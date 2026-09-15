#20260915_kpopmodder: Single registration never borrows area authority or survives changed input/session ownership.
from dataclasses import replace, FrozenInstanceError
from types import SimpleNamespace
import logging
import pytest
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.single_registration import (
    SingleContainerTrustOwner,
    SingleContainerTrustReceipt,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_natural_language_service import (
    ChatClefNaturalLanguageService,
)
from tests.minecraft_chatclef.lavi_input.confirmation.owner_fixture import event


def fixture(callback=None):
    state = SimpleNamespace(session="s", generation=1, ready=True, trusted=True)
    row = event("여기를 자동 보관 장소로 등록해 줘")
    translation = ChatClefNaturalLanguageService().translate(row.text).to_dict()
    messages = []

    def inspect(_):
        return SimpleNamespace(
            ready=state.ready,
            status={
                "details": {
                    "commands": {
                        "active_session_id": state.session,
                        "active_generation": state.generation,
                    }
                }
            },
        )

    owner = SingleContainerTrustOwner(
        extension=object(),
        submission_precheck=SimpleNamespace(inspect=inspect),
        live_proof_validator=lambda proof, value: state.trusted and proof is value,
        log_callback=callback or messages.append,
    )
    return SimpleNamespace(
        owner=owner, event=row, state=state, translation=translation, messages=messages
    )


def issue(f):
    receipt, rejection = f.owner.issue(
        event=f.event, proof=f.event, translation=f.translation
    )
    assert rejection is None and receipt is not None
    return receipt


def request(receipt):
    row = receipt.event
    return SimpleNamespace(
        command="auto_deposit_trust",
        source=row.source,
        metadata={
            "korean_single_trust": receipt.binding_metadata(),
            "input_event": {
                "event_id": row.event_id,
                "source": row.source,
                "provider_id": row.provider_id,
                "event_kind": row.event_kind,
                "final": True,
            },
        },
    )


@pytest.mark.parametrize("voice", (False, True))
def test_valid_outer_whitespace_keeps_original_trusted_binding(voice):
    from tests.minecraft_chatclef.lavi_input.confirmation.trusted_route_fixture import (
        TrustedConfirmationRouteFixture,
    )

    f = TrustedConfirmationRouteFixture()
    f.dispatch("  여기를 자동 보관 장소로 등록해 줘  ", voice=voice)
    assert [row.command for row in f.adapter.requests] == ["auto_deposit_trust"]
    row = f.adapter.requests[0]
    assert row.metadata["input_event"]["event_id"] == row.metadata["korean_single_trust"]["original_event_id"]
    assert f.pipeline.provider_calls == 0


def test_receipt_commits_exact_single_command_once_and_cannot_be_forged():
    f = fixture()
    receipt = issue(f)
    forged = SingleContainerTrustReceipt(f.owner, f.event, f.event, ("s", 1))
    assert not forged.matches_admission("auto_deposit_trust", f.event.source)
    assert receipt.matches_admission("auto_deposit_trust", f.event.source)
    with pytest.raises(FrozenInstanceError):
        receipt.session = ("other", 1)
    assert receipt.commit(request(receipt))
    assert not receipt.commit(request(receipt))
    assert f.owner._receipts == {}


@pytest.mark.parametrize(
    "mutation",
    ("command", "event", "source", "binding", "session", "generation", "trust", "busy"),
)
def test_binding_change_is_rejected_and_receipt_retired(mutation):
    f = fixture()
    receipt = issue(f)
    value = request(receipt)
    if mutation == "command":
        value.command = "auto_deposit_trust area 16x16"
    if mutation == "event":
        value.metadata["input_event"]["event_id"] = "other"
    if mutation == "source":
        value.source = "voice_input_final"
    if mutation == "binding":
        value.metadata["korean_single_trust"]["expected_session_id"] = "other"
    if mutation == "session":
        f.state.session = "other"
    if mutation == "generation":
        f.state.generation = 2
    if mutation == "trust":
        f.state.trusted = False
    if mutation == "busy":
        f.state.ready = False
    assert not receipt.commit(value)
    assert f.owner._receipts == {}


@pytest.mark.parametrize(
    "text",
    (
        "자동보관등록 영역 16x16",
        "주변 16x16 자동 보관 장소 등록해 줘",
        "여기를 자동 보관 장소로 등록해 줘?",
        "여기를 자동 보관 장소로 등록하지 마",
    ),
)
def test_other_commands_questions_and_negation_never_issue_single_receipt(text):
    f = fixture()
    row = replace(f.event, text=text)
    receipt, rejection = f.owner.issue(event=row, proof=row, translation=f.translation)
    assert receipt is None and rejection.handled


def test_direct_input_without_trusted_proof_cannot_issue_receipt():
    f = fixture()
    assert (
        f.owner.issue(event=f.event, proof=object(), translation=f.translation)[0]
        is None
    )
    f.event = replace(f.event, source="direct_typed")
    assert (
        f.owner.issue(event=f.event, proof=f.event, translation=f.translation)[0]
        is None
    )


def test_actual_file_sink_and_diagnostic_failure_preserve_decisions(tmp_path):
    path = tmp_path / "single-trust.log"
    logger = logging.Logger("single-trust")
    handler = logging.FileHandler(path, encoding="utf-8")
    handler.setFormatter(logging.Formatter("%(levelname)s %(message)s"))
    logger.addHandler(handler)
    f = fixture(logger.info)
    receipt = issue(f)
    assert receipt.commit(request(receipt))
    handler.close()
    output = path.read_text(encoding="utf-8")
    assert "reason=issued" in output and "reason=committed" in output
    assert "receipt_live=true request_match=true retained=0" in output
    assert f.event.text not in output

    def fail(_):
        raise OSError("sink unavailable")

    f = fixture(fail)
    receipt = issue(f)
    assert receipt.commit(request(receipt)) and f.owner._receipts == {}
