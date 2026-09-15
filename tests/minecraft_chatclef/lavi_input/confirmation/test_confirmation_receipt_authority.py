# 20260915_kpopmodder: Direct admission must use the concrete issuing owner, not caller-supplied authority methods.
from dataclasses import replace
from types import SimpleNamespace

import pytest

from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission import (
    KoreanCommandSubmissionAdmission,
)
from plugins.Minecraft.fabric.chatclef.input.confirmation.confirmation_receipt import (
    KoreanCommandConfirmationReceipt,
)
from plugins.Minecraft.fabric.chatclef.input.confirmation.korean_command_confirmation_owner import (
    KoreanCommandConfirmationOwner,
)
from .owner_fixture import fixture, begin, confirm, request


@pytest.mark.parametrize("forgery", ("fake_object", "subclass", "instance_methods"))
def test_direct_admission_and_commit_ignore_forged_owner_authority(forgery):
    f = fixture()
    begin(f)
    original, _ = confirm(f)
    calls = []

    def forged(*_args, **_kwargs):
        calls.append("forged authority invoked")
        return True

    if forgery == "fake_object":
        owner = SimpleNamespace(is_live_receipt=forged, commit=forged, abandon=forged)
    elif forgery == "subclass":
        owner_type = type(
            "ForgedOwner",
            (KoreanCommandConfirmationOwner,),
            {
                "is_live_receipt": forged,
                "commit": forged,
                "abandon": forged,
            },
        )
        owner = object.__new__(owner_type)
        owner.__dict__.update(f.owner.__dict__)
    else:
        owner = f.owner
        owner.is_live_receipt = forged
        owner.commit = forged
        owner.abandon = forged
    counterfeit = KoreanCommandConfirmationReceipt(
        owner=owner,
        pending=original.pending,
        confirmation_event=original.confirmation_event,
        proof=original._proof,
    )
    admission = KoreanCommandSubmissionAdmission()
    decision = admission.inspect(
        "follow", f.original.source, KoreanChatClefCommandRegistry(), counterfeit
    )
    assert not decision.allowed
    assert not admission.commit(
        replace(decision, allowed=True), counterfeit, request(counterfeit)
    ).allowed
    admission.abandon_if_issued(counterfeit)
    assert calls == []


def test_instance_override_cannot_skip_commit_revalidation_or_consumption():
    f = fixture()
    begin(f)
    receipt, _ = confirm(f)
    admission = KoreanCommandSubmissionAdmission()
    decision = admission.inspect(
        "follow", f.original.source, KoreanChatClefCommandRegistry(), receipt
    )
    assert decision.allowed
    f.owner.is_live_receipt = lambda *_: True
    f.owner.commit = lambda *_: True
    f.owner.abandon = lambda *_args, **_kwargs: None
    f.state.trusted = False
    assert not admission.commit(decision, receipt, request(receipt)).allowed
    assert receipt._spent and f.owner._receipts == {}


def test_fake_owner_with_missing_pending_is_rejected_before_slot_access():
    receipt = KoreanCommandConfirmationReceipt(
        owner=SimpleNamespace(is_live_receipt=lambda *_: True, commit=lambda *_: True),
        pending=None,
        confirmation_event=None,
        proof=None,
    )
    admission = KoreanCommandSubmissionAdmission()
    decision = admission.inspect(
        "follow", "lavi_chat_ui", KoreanChatClefCommandRegistry(), receipt
    )
    assert not decision.allowed
    assert not receipt.commit(object())
