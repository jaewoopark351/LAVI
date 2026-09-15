# 20260915_kpopmodder: Single-container admission must never delegate authority to a forged or overridden owner.
from dataclasses import replace
from types import SimpleNamespace

import pytest

from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission import (
    KoreanCommandSubmissionAdmission,
)
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.single_registration import (
    SingleContainerTrustOwner,
    SingleContainerTrustReceipt,
)
from .test_single_registration_capability import fixture, issue, request


@pytest.mark.parametrize("forgery", ("fake_object", "subclass", "instance_methods"))
def test_direct_admission_and_commit_ignore_forged_single_owner_authority(forgery):
    f = fixture()
    original = issue(f)
    calls = []

    def forged(*_args, **_kwargs):
        calls.append("forged authority invoked")
        return True

    if forgery == "fake_object":
        owner = SimpleNamespace(is_live=forged, commit=forged, abandon=forged)
    elif forgery == "subclass":
        owner_type = type(
            "ForgedOwner",
            (SingleContainerTrustOwner,),
            {
                "is_live": forged,
                "commit": forged,
                "abandon": forged,
            },
        )
        owner = object.__new__(owner_type)
        owner.__dict__.update(f.owner.__dict__)
    else:
        owner = f.owner
        owner.is_live = forged
        owner.commit = forged
        owner.abandon = forged
    counterfeit = SingleContainerTrustReceipt(
        owner, original.event, original.proof, original.session
    )
    admission = KoreanCommandSubmissionAdmission()
    decision = admission.inspect(
        "auto_deposit_trust",
        f.event.source,
        KoreanChatClefCommandRegistry(),
        counterfeit,
    )
    assert not decision.allowed
    assert not admission.commit(
        replace(decision, allowed=True), counterfeit, request(counterfeit)
    ).allowed
    admission.abandon_if_issued(counterfeit)
    assert calls == []


def test_single_instance_override_cannot_skip_commit_revalidation_or_consumption():
    f = fixture()
    receipt = issue(f)
    admission = KoreanCommandSubmissionAdmission()
    decision = admission.inspect(
        "auto_deposit_trust", f.event.source, KoreanChatClefCommandRegistry(), receipt
    )
    assert decision.allowed
    f.owner.is_live = lambda *_: True
    f.owner.commit = lambda *_: True
    f.owner.abandon = lambda *_args, **_kwargs: None
    f.state.trusted = False
    assert not admission.commit(decision, receipt, request(receipt)).allowed
    assert f.owner._receipts == {}


def test_fake_single_owner_with_missing_event_is_rejected_before_slot_access():
    receipt = SingleContainerTrustReceipt(
        SimpleNamespace(is_live=lambda *_: True, commit=lambda *_: True),
        None,
        None,
        ("session", 1),
    )
    admission = KoreanCommandSubmissionAdmission()
    decision = admission.inspect(
        "auto_deposit_trust", "lavi_chat_ui", KoreanChatClefCommandRegistry(), receipt
    )
    assert not decision.allowed
    assert not receipt.commit(object())
