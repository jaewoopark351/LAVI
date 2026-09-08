#20260908_kpopmodder: Verify STATUS claims reuse exact admission descriptor truth.
from __future__ import annotations

import unittest
from dataclasses import fields, replace
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.transport.command_feedback.crafting import (
    CraftingFeedbackAdmissionAuthorizer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackDescriptorFactory,
    CommandStatusDescriptorEligibilityValidator,
)


class CommandStatusDescriptorEligibilityValidatorTests(unittest.TestCase):
    def setUp(self) -> None:
        self.descriptor = _production_descriptor()
        self.validator = CommandStatusDescriptorEligibilityValidator()

    def test_accepts_exact_admission_validated_descriptor(self):
        self.assertTrue(self.validator.accepts(self.descriptor))

    def test_rejects_each_registered_profile_identity_contradiction(self):
        contradictions = (
            replace(self.descriptor, command_name="unregistered"),
            replace(self.descriptor, lifecycle_kind="command"),
            replace(self.descriptor, phrase_profile_id="wrong_phrase_v1"),
            replace(self.descriptor, evidence_profile_id="wrong_evidence_v1"),
            replace(self.descriptor, rollout_state="cautious"),
            replace(self.descriptor, requested_family="movement_goto"),
            replace(self.descriptor, response_lifecycle_kind="persistent_task"),
        )
        for descriptor in contradictions:
            with self.subTest(descriptor=descriptor):
                self.assertFalse(self.validator.accepts(descriptor))

    def test_rejects_lookalikes_and_contains_validator_failures(self):
        self.assertFalse(
            self.validator.accepts(
                SimpleNamespace(
                    **{
                        field.name: getattr(self.descriptor, field.name)
                        for field in fields(self.descriptor)
                    }
                )
            )
        )
        throwing = CommandStatusDescriptorEligibilityValidator(
            descriptor_factory=SimpleNamespace(
                accepts_descriptor=lambda _descriptor: (_ for _ in ()).throw(
                    RuntimeError("secret")
                )
            )
        )
        self.assertFalse(throwing.accepts(self.descriptor))

    def test_accepts_only_the_exact_admission_owned_legacy_descriptor(self):
        grant = _legacy_crafting_grant()
        descriptor = grant.descriptor

        self.assertEqual("get_phrase_v1", descriptor.phrase_profile_id)
        self.assertTrue(
            self.validator.accepts(
                descriptor,
                admission_grant=grant,
            )
        )
        self.assertFalse(
            self.validator.accepts(
                descriptor,
                admission_grant=SimpleNamespace(descriptor=descriptor),
            )
        )
        self.assertFalse(
            self.validator.accepts(
                replace(descriptor, target_item="iron_ingot"),
                admission_grant=grant,
            )
        )


def _production_descriptor():
    descriptor = CommandFeedbackDescriptorFactory().decode_registered_command_name_only(
        "get iron_ingot 10",
        command_source="lavi_gui",
        event_id="3" * 32,
        provider_id="minecraft_fabric_chatclef_ui",
        event_kind="minecraft_raw_gui_submit",
    )
    if descriptor is None:
        raise AssertionError("GET descriptor fixture was not accepted")
    return descriptor


def _legacy_crafting_grant():
    proof = object()
    grant = CraftingFeedbackAdmissionAuthorizer(
        live_proof_validator=lambda candidate, _event: candidate is proof,
    ).issue(
        event=SimpleNamespace(
            text="다이아 곡괭이 만들어줘",
            source="lavi_chat_ui",
            provider_id="lavi_chat_ui",
            event_kind="chat_submit",
            event_id="5" * 32,
        ),
        korean_eligibility_proof=proof,
        translation={
            "status": "validated",
            "executable": True,
            "command": "get diamond_pickaxe 1",
            "resolved_target": "diamond_pickaxe",
            "intent": {
                "intent_type": "get_item",
                "quantity": 1,
                "language": "ko",
                "original_text": "다이아 곡괭이 만들어줘",
            },
        },
    )
    if grant is None:
        raise AssertionError("legacy crafting admission fixture was rejected")
    return grant


if __name__ == "__main__":
    unittest.main()
