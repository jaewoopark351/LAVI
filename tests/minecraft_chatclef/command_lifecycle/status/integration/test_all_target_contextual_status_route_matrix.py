#20260908_kpopmodder: Route every source-derived item action target through contextual STATUS ownership.
from __future__ import annotations

import json
import unittest
from pathlib import Path

from input_core.input_event.adapters import LocalChatInputEventAdapter
from plugins.Minecraft.fabric.chatclef.intent import (
    ChatClefNaturalLanguageService,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_alias_repository import (
    ChatClefKoreanAliasRepository,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean import (
    TrustedKoreanProofValidator,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_equipment_target_composer import (
    ChatClefEquipmentTargetComposer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackDescriptor,
    CommandFeedbackDescriptorFactory,
)

from .contextual_status_route_harness import ContextualStatusRouteHarness
from .trusted_status_input_fixture import issue_trusted_status_input


class AllTargetContextualStatusRouteMatrixTests(unittest.TestCase):
    def test_all_catalog_item_actions_and_equipment_targets_route_exact_status(self):
        resource_path = (
            Path(__file__).resolve().parents[5]
            / "plugins"
            / "Minecraft"
            / "fabric"
            / "chatclef"
            / "intent"
            / "resources"
            / "chatclef_item_command_target_policy.json"
        )
        policy = json.loads(resource_path.read_text(encoding="utf-8"))
        targets = policy["targets"]
        self.assertEqual(591, len(targets))
        self.assertEqual(
            6,
            sum(
                values["classification"] == "UNSUPPORTED"
                for values in targets.values()
            ),
        )
        equipment_targets = ChatClefEquipmentTargetComposer().all_targets()
        self.assertTrue(equipment_targets)
        self.assertLessEqual(equipment_targets, set(targets))

        proof_owner = object()
        _registry, event, proof = issue_trusted_status_input(
            voice=False,
            event_id="a" * 32,
            proof_owner=proof_owner,
        )
        harness = ContextualStatusRouteHarness(
            proof_validator=TrustedKoreanProofValidator(
                owner=proof_owner,
            ).is_live,
        )
        descriptor_factory = CommandFeedbackDescriptorFactory()
        action_domains = {
            "get": tuple(sorted(targets)),
            "deposit": tuple(sorted(targets)),
            "deposit_all": tuple(sorted(targets)),
            "give": tuple(sorted(targets)),
            "equip": tuple(sorted(equipment_targets)),
        }
        routed_pairs = 0
        try:
            for command_name, command_targets in action_domains.items():
                for target in command_targets:
                    with self.subTest(command_name=command_name, target=target):
                        command = (
                            f"equip {target}"
                            if command_name == "equip"
                            else f"{command_name} {target} 2"
                        )
                        descriptor = (
                            descriptor_factory.decode_registered_command_name_only(
                                command,
                                command_source="lavi_gui",
                                event_id="b" * 32,
                                provider_id="minecraft_fabric_chatclef_ui",
                                event_kind="minecraft_raw_gui_submit",
                            )
                        )
                        self.assertIs(type(descriptor), CommandFeedbackDescriptor)
                        self.assertEqual(target, descriptor.target_item)
                        self.assertEqual(
                            targets[target]["display_name"],
                            descriptor.spoken_target_label,
                        )
                        expected = self._expected_status(
                            command_name=command_name,
                            spoken_label=descriptor.spoken_target_label,
                        )

                        decision = harness.route(
                            event=event,
                            proof=proof,
                            descriptor=descriptor,
                        )

                        self.assertTrue(decision.handled)
                        self.assertEqual("command_status_query", decision.route_kind)
                        self.assertFalse(decision.result["command_submitted"])
                        self.assertEqual(
                            expected,
                            harness.renderer.render_status(
                                harness.last_snapshot,
                                harness.last_query,
                            ),
                        )
                        self.assertEqual(expected, decision.response_text)
                        self.assertIs(
                            descriptor,
                            harness.last_snapshot.descriptor,
                        )
                        routed_pairs += 1
        finally:
            proof.close()

        self.assertEqual(
            (4 * 591) + len(equipment_targets),
            routed_pairs,
        )
        self.assertEqual(0, harness.ordinary_route_calls)

    def test_every_fixed_alias_and_direct_canonical_ingress_routes_frozen_status(self):
        resource_path = (
            Path(__file__).resolve().parents[5]
            / "plugins"
            / "Minecraft"
            / "fabric"
            / "chatclef"
            / "intent"
            / "resources"
            / "chatclef_item_command_target_policy.json"
        )
        targets = json.loads(resource_path.read_text(encoding="utf-8"))["targets"]
        aliases = ChatClefKoreanAliasRepository().fixed_item_aliases
        self.assertEqual(564, len(aliases))
        self.assertEqual(591, len(targets))

        proof_owner = object()
        _registry, status_event, proof = issue_trusted_status_input(
            voice=False,
            event_id="c" * 32,
            proof_owner=proof_owner,
        )
        harness = ContextualStatusRouteHarness(
            proof_validator=TrustedKoreanProofValidator(owner=proof_owner).is_live,
        )
        service = ChatClefNaturalLanguageService()
        descriptor_factory = CommandFeedbackDescriptorFactory()
        routed = 0
        try:
            rows = (
                (f"{alias} 2개 가져와줘", target)
                for alias, target in sorted(aliases.items())
            )
            canonical_rows = (
                (f"{target} 2개 가져와줘", target)
                for target in sorted(targets)
            )
            for source_kind, source_rows in (
                ("fixed_alias", rows),
                ("direct_canonical", canonical_rows),
            ):
                for command_text, expected_target in source_rows:
                    with self.subTest(
                        source_kind=source_kind,
                        command_text=command_text,
                    ):
                        descriptor = _trusted_descriptor(
                            descriptor_factory=descriptor_factory,
                            service=service,
                            command_text=command_text,
                        )
                        self.assertEqual(expected_target, descriptor.target_item)
                        expected = (
                            f"{descriptor.spoken_target_label} 2개 구하는 중이야"
                        )

                        decision = harness.route(
                            event=status_event,
                            proof=proof,
                            descriptor=descriptor,
                        )

                        self.assertTrue(decision.handled)
                        self.assertEqual(expected, decision.response_text)
                        self.assertIs(
                            descriptor,
                            harness.last_snapshot.descriptor,
                        )
                        self.assertFalse(decision.result["command_submitted"])
                        routed += 1
        finally:
            proof.close()

        self.assertEqual(564 + 591, routed)
        self.assertEqual(0, harness.ordinary_route_calls)

    def test_composed_equipment_typed_give_and_outside_catalog_keep_exact_slots(self):
        proof_owner = object()
        _registry, status_event, proof = issue_trusted_status_input(
            voice=False,
            event_id="d" * 32,
            proof_owner=proof_owner,
        )
        harness = ContextualStatusRouteHarness(
            proof_validator=TrustedKoreanProofValidator(owner=proof_owner).is_live,
        )
        service = ChatClefNaturalLanguageService()
        descriptor_factory = CommandFeedbackDescriptorFactory()
        try:
            trusted_rows = (
                (
                    "철 흉갑 입어줘",
                    "iron_chestplate",
                    "철 흉갑 장착하는 중이야",
                ),
                (
                    "Steve에게 다이아몬드 3개 줘",
                    "diamond",
                    "Steve에게 다이아몬드 3개 건네는 중이야",
                ),
            )
            for command_text, expected_target, expected_status in trusted_rows:
                with self.subTest(command_text=command_text):
                    descriptor = _trusted_descriptor(
                        descriptor_factory=descriptor_factory,
                        service=service,
                        command_text=command_text,
                    )
                    self.assertEqual(expected_target, descriptor.target_item)
                    decision = harness.route(
                        event=status_event,
                        proof=proof,
                        descriptor=descriptor,
                    )
                    self.assertEqual(expected_status, decision.response_text)
                    self.assertFalse(decision.result["command_submitted"])

            outside = descriptor_factory.decode_registered_command_name_only(
                "give future_mod_item 2",
                command_source="lavi_gui",
                event_id="e" * 32,
                provider_id="minecraft_fabric_chatclef_ui",
                event_kind="minecraft_raw_gui_submit",
            )
            self.assertIs(type(outside), CommandFeedbackDescriptor)
            self.assertEqual("요청한 아이템", outside.spoken_target_label)
            outside_decision = harness.route(
                event=status_event,
                proof=proof,
                descriptor=outside,
            )
            self.assertEqual(
                "요청한 아이템 2개 건네는 중이야",
                outside_decision.response_text,
            )
            self.assertFalse(outside_decision.result["command_submitted"])
        finally:
            proof.close()

        self.assertEqual(0, harness.ordinary_route_calls)

    def test_target_qualified_canonical_alias_quantity_and_mismatches(self):
        proof_owner = object()
        harness = ContextualStatusRouteHarness(
            proof_validator=TrustedKoreanProofValidator(owner=proof_owner).is_live,
        )
        descriptor = _trusted_descriptor(
            descriptor_factory=CommandFeedbackDescriptorFactory(),
            service=ChatClefNaturalLanguageService(),
            command_text="횃불 2개 가져와줘",
        )
        self.assertEqual("torch", descriptor.target_item)
        self.assertEqual("횃불", descriptor.spoken_target_label)

        rows = (
            ("torch 구하는 중이야?", True),
            ("횃불 2개를 구하는 중이야?", True),
            ("횃불 3개 구하는 중이야?", False),
            ("석탄 2개 구하는 중이야?", False),
        )
        for index, (question, expected_handled) in enumerate(rows):
            with self.subTest(question=question):
                _registry, event, proof = issue_trusted_status_input(
                    voice=False,
                    event_id=f"{index + 1:x}" * 32,
                    proof_owner=proof_owner,
                    text=question,
                )
                try:
                    decision = harness.route(
                        event=event,
                        proof=proof,
                        descriptor=descriptor,
                    )
                finally:
                    proof.close()

                self.assertIs(expected_handled, decision.handled)
                if expected_handled:
                    self.assertEqual(
                        "횃불 2개 구하는 중이야",
                        decision.response_text,
                    )
                else:
                    self.assertEqual(
                        "command_status_conversational_fallthrough",
                        decision.reason,
                    )
                self.assertEqual(0, harness.ordinary_route_calls)

    @staticmethod
    def _expected_status(*, command_name: str, spoken_label: str) -> str:
        if command_name == "get":
            return f"{spoken_label} 2개 구하는 중이야"
        if command_name in {"deposit", "deposit_all"}:
            return f"{spoken_label} 2개 보관하는 중이야"
        if command_name == "give":
            return f"{spoken_label} 2개 건네는 중이야"
        return f"{spoken_label} 장착하는 중이야"


def _trusted_descriptor(*, descriptor_factory, service, command_text):
    translation = service.translate(command_text)
    event = LocalChatInputEventAdapter(
        event_id_factory=lambda: "f" * 32,
    ).adapt(command_text)
    descriptor = descriptor_factory.from_trusted_translation(
        event=event,
        translation=translation.to_dict(),
    )
    if type(descriptor) is not CommandFeedbackDescriptor:
        raise AssertionError(f"trusted descriptor was not accepted: {command_text}")
    return descriptor


if __name__ == "__main__":
    unittest.main()
