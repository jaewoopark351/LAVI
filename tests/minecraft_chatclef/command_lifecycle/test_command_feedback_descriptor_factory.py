#20260907_kpopmodder: Prove trusted typed and feedback-only raw descriptor boundaries.
from __future__ import annotations

import unittest
from dataclasses import FrozenInstanceError, replace
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackDescriptor,
    CommandFeedbackDescriptorFactory,
    CommandFeedbackAdmissionCoordinator,
    CommandTerminalEvidenceProfile,
)


class CommandFeedbackDescriptorFactoryTests(unittest.TestCase):
    def setUp(self) -> None:
        self.factory = CommandFeedbackDescriptorFactory()

    def test_all_current_non_stop_public_translations_keep_validated_slots(self):
        cases = (
            (
                "get",
                "get diamond_pickaxe 3",
                "get_item",
                {"quantity": 3, "item_phrase": "다이아 곡괭이"},
                "diamond_pickaxe",
                CommandFeedbackDescriptor.ACQUIRE_DELTA,
            ),
            (
                "deposit",
                "deposit iron_ingot 4",
                "deposit_item",
                {"quantity": 4, "item_phrase": "철"},
                "iron_ingot",
                CommandFeedbackDescriptor.REQUESTED_COUNT,
            ),
            (
                "equip",
                "equip diamond_pickaxe",
                "equip_item",
                {"item_phrase": "다이아 곡괭이"},
                "diamond_pickaxe",
                CommandFeedbackDescriptor.UNKNOWN_QUANTITY,
            ),
            (
                "give",
                "give Steve oak_log 2",
                "give_item",
                {
                    "quantity": 2,
                    "item_phrase": "참나무 원목",
                    "player_name": "Steve",
                },
                "oak_log",
                CommandFeedbackDescriptor.REQUESTED_COUNT,
            ),
            (
                "food",
                "food 8",
                "food",
                {"food_units": 8},
                None,
                CommandFeedbackDescriptor.PROFILE_SPECIFIC_UNITS,
            ),
            (
                "meat",
                "meat 6",
                "meat",
                {"food_units": 6},
                None,
                CommandFeedbackDescriptor.PROFILE_SPECIFIC_UNITS,
            ),
            (
                "goto",
                "goto -10 64 25",
                "goto",
                {"x": -10, "y": 64, "z": 25},
                None,
                CommandFeedbackDescriptor.UNKNOWN_QUANTITY,
            ),
            (
                "store_home",
                "store_home",
                "store_home",
                {},
                None,
                CommandFeedbackDescriptor.UNKNOWN_QUANTITY,
            ),
            (
                "auto_deposit_trust",
                "auto_deposit_trust area 16x16",
                "auto_deposit_trust_area",
                {},
                None,
                CommandFeedbackDescriptor.UNKNOWN_QUANTITY,
            ),
        )

        for command_name, command, intent_kind, slots, target, semantics in cases:
            with self.subTest(command_name=command_name):
                descriptor = self.factory.from_trusted_translation(
                    event=_event(command_name),
                    translation=_translation(
                        command=command,
                        intent_kind=intent_kind,
                        target=target,
                        **slots,
                    ),
                )
                self.assertIsNotNone(descriptor)
                self.assertEqual(command_name, descriptor.command_name)
                self.assertEqual(semantics, descriptor.quantity_semantics)
                self.assertEqual(CommandFeedbackDescriptor.TYPED, descriptor.detail_level)
                self.assertEqual(
                    CommandTerminalEvidenceProfile.VERIFIED
                    if command_name in {"get", "store_home"}
                    else CommandTerminalEvidenceProfile.CAUTIOUS,
                    descriptor.rollout_state,
                )

    def test_typed_descriptor_rejects_slot_or_command_contradictions(self):
        base = _translation(
            command="get diamond_pickaxe 3",
            intent_kind="get_item",
            target="diamond_pickaxe",
            quantity=3,
            item_phrase="다이아 곡괭이",
        )
        cases = (
            {**base, "command": "get dirt 3"},
            {**base, "resolved_target": "dirt"},
            {**base, "intent": {**base["intent"], "quantity": True}},
            {**base, "intent": {**base["intent"], "quantity": 2**31}},
            {**base, "intent": {**base["intent"], "intent_type": "deposit_item"}},
        )

        for translation in cases:
            with self.subTest(translation=translation):
                self.assertIsNone(
                    self.factory.from_trusted_translation(
                        event=_event("get"),
                        translation=translation,
                    )
                )

    def test_spoken_item_label_must_resolve_to_the_same_validated_target(self):
        descriptor = self.factory.from_trusted_translation(
            event=_event("get"),
            translation=_translation(
                command="get dirt 1",
                intent_kind="get_item",
                target="dirt",
                quantity=1,
                item_phrase="다이아 곡괭이",
            ),
        )

        self.assertIsNotNone(descriptor)
        self.assertNotEqual("다이아 곡괭이", descriptor.spoken_target_label)

    def test_registered_name_with_opaque_or_malformed_slots_gets_no_feedback(self):
        for command_name in KoreanChatClefCommandRegistry().command_names():
            with self.subTest(command_name=command_name):
                descriptor = self.factory.decode_registered_command_name_only(
                    f"@{command_name} opaque slots",
                    command_source="lavi_gui",
                    event_id="b" * 32,
                    provider_id="minecraft_gui",
                    event_kind="raw_command_submit",
                )
                self.assertIsNone(descriptor)

    def test_raw_decoder_is_feedback_only_and_fails_closed(self):
        cases = (
            ("@get dirt 1", "lavi_chat_ui", "b" * 32),
            ("@unknown dirt 1", "lavi_gui", "b" * 32),
            ("@@get dirt 1", "lavi_gui", "b" * 32),
            ("@get dirt 1; stop", "lavi_gui", "b" * 32),
            ("@get dirt 1\n", "lavi_gui", "b" * 32),
            ("@get dirt 1\r@give Steve dirt 1", "lavi_gui", "b" * 32),
            ("@get dirt 1 && @stop", "lavi_gui", "b" * 32),
            ("@get dirt 1", "lavi_gui", "not-an-event-id"),
            ("@get " + "x" * 508, "lavi_gui", "b" * 32),
        )

        for command, source, event_id in cases:
            with self.subTest(command=command, source=source):
                self.assertIsNone(
                    self.factory.decode_registered_command_name_only(
                        command,
                        command_source=source,
                        event_id=event_id,
                        provider_id="minecraft_gui",
                        event_kind="raw_command_submit",
                    )
                )

    def test_descriptor_is_immutable(self):
        descriptor = self.factory.decode_registered_command_name_only(
            "@scan dirt",
            command_source="lavi_gui",
            event_id="c" * 32,
            provider_id="minecraft_gui",
            event_kind="raw_command_submit",
        )

        with self.assertRaises(FrozenInstanceError):
            descriptor.command_name = "get"

    def test_admission_revalidates_descriptor_registry_identity(self):
        descriptor = self.factory.decode_registered_command_name_only(
            "@scan dirt",
            command_source="lavi_gui",
            event_id="c" * 32,
            provider_id="minecraft_gui",
            event_kind="raw_command_submit",
        )
        admission = CommandFeedbackAdmissionCoordinator(
            live_proof_validator=lambda _proof, _event: False,
            descriptor_factory=self.factory,
        )

        self.assertIsNotNone(admission.issue_descriptor(descriptor))
        self.assertIsNone(
            admission.issue_descriptor(
                replace(descriptor, evidence_profile_id="get_terminal_evidence_v1")
            )
        )
        self.assertIsNone(
            admission.issue_descriptor(
                replace(
                    descriptor,
                    command_name="stop",
                    phrase_profile_id="stop_phrase_v1",
                    evidence_profile_id="stop_terminal_evidence_v1",
                    requested_family="control",
                    rollout_state="disabled",
                )
            )
        )


def _event(command_name: str):
    source = "lavi_chat_ui"
    return SimpleNamespace(
        text=f"{command_name} 요청",
        source=source,
        provider_id=source,
        event_kind="chat_submit",
        final=True,
        event_id="a" * 32,
    )


def _translation(
    *,
    command: str,
    intent_kind: str,
    target: str | None,
    quantity: int | None = None,
    item_phrase: str = "",
    food_units: int | None = None,
    player_name: str = "",
    x: int | None = None,
    y: int | None = None,
    z: int | None = None,
):
    return {
        "status": "validated",
        "executable": True,
        "command": command,
        "resolved_target": target,
        "intent": {
            "intent_type": intent_kind,
            "quantity": quantity,
            "item_phrase": item_phrase,
            "food_units": food_units,
            "player_name": player_name,
            "x": x,
            "y": y,
            "z": z,
            "language": "ko",
            "original_text": f"{intent_kind} 요청",
        },
    }


if __name__ == "__main__":
    unittest.main()
