#20260907_kpopmodder: Derive all-item lifecycle rendering coverage from live artifacts.
from __future__ import annotations

import json
import unittest
from pathlib import Path
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.intent.chatclef_korean_display_name_repository import (
    ChatClefKoreanDisplayNameRepository,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_equipment_target_composer import (
    ChatClefEquipmentTargetComposer,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_target_catalog import (
    ChatClefTargetCatalog,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_item_phrase_resolver import (
    KoreanItemPhraseResolver,
)
from plugins.Minecraft.fabric.chatclef.presentation.command_lifecycle import (
    CommandLifecyclePresentationDetailLogPolicy,
    CommandLifecyclePresentationDetailProjector,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackDescriptorFactory,
)


class AllItemCommandLifecycleRenderingTests(unittest.TestCase):
    def setUp(self) -> None:
        resource_dir = (
            Path(__file__).resolve().parents[3]
            / "plugins"
            / "Minecraft"
            / "fabric"
            / "chatclef"
            / "intent"
            / "resources"
        )
        self.policy = json.loads(
            (resource_dir / "chatclef_item_command_target_policy.json").read_text(
                encoding="utf-8"
            )
        )
        self.display_names = ChatClefKoreanDisplayNameRepository(
            resource_dir
        ).display_names
        self.factory = CommandFeedbackDescriptorFactory()
        self.renderer = CommandLifecycleResponseRenderer()
        self.catalog_targets = ChatClefTargetCatalog().targets
        self.equipment_targets = (
            ChatClefEquipmentTargetComposer().all_targets()
        )

    def test_source_derived_supported_targets_have_bounded_complete_get_phrases(self):
        targets = self.policy["targets"]
        self.assertEqual(set(targets), set(self.display_names))
        supported = {
            target: values
            for target, values in targets.items()
            if values["classification"] != "UNSUPPORTED"
        }
        self.assertTrue(supported)

        for target, values in supported.items():
            with self.subTest(target=target):
                descriptor = self.factory.decode_registered_command_name_only(
                    f"get {target} 2",
                    command_source="lavi_gui",
                    event_id="a" * 32,
                    provider_id="minecraft_fabric_chatclef_ui",
                    event_kind="minecraft_raw_gui_submit",
                )
                self.assertIsNotNone(descriptor)
                self.assertEqual("verified", descriptor.rollout_state)
                self.assertEqual(values["display_name"], descriptor.spoken_target_label)
                texts = (
                    self.renderer.render_start(descriptor),
                    self.renderer.render_status(
                        SimpleNamespace(state="running", descriptor=descriptor)
                    ),
                    self.renderer.render_terminal(
                        _fact(descriptor, verified=False)
                    ),
                    self.renderer.render_terminal(
                        _fact(descriptor, verified=True)
                    ),
                )
                for text in texts:
                    self.assertTrue(text)
                    self.assertLessEqual(len(text), 256)
                    self.assertNotIn(target, text)

    def test_source_derived_action_domains_render_every_item_consuming_raw_family(self):
        self.assertEqual(set(self.policy["targets"]), set(self.catalog_targets))
        self.assertTrue(self.equipment_targets)
        self.assertLessEqual(self.equipment_targets, set(self.catalog_targets))
        commands = {
            "get": self.catalog_targets,
            "deposit": self.catalog_targets,
            "deposit_all": self.catalog_targets,
            "equip": self.equipment_targets,
            "give": self.catalog_targets,
        }

        for command_name, targets in commands.items():
            for target in targets:
                with self.subTest(command_name=command_name, target=target):
                    command = _raw_item_command(command_name, target)
                    descriptor = self.factory.decode_registered_command_name_only(
                        command,
                        command_source="lavi_gui",
                        event_id="b" * 32,
                        provider_id="minecraft_fabric_chatclef_ui",
                        event_kind="minecraft_raw_gui_submit",
                    )
                    self.assertIsNotNone(descriptor)
                    self.assertEqual(target, descriptor.target_item)
                    self.assertEqual(
                        self.display_names[target],
                        descriptor.spoken_target_label,
                    )
                    cautious = self.renderer.render_terminal(
                        _fact(descriptor, verified=False)
                    )
                    texts = (
                        self.renderer.render_start(descriptor),
                        self.renderer.render_status(
                            SimpleNamespace(
                                state="running",
                                descriptor=descriptor,
                            )
                        ),
                        cautious,
                    )
                    for text in texts:
                        self.assertTrue(text)
                        self.assertLessEqual(len(text), 256)
                        self.assertNotIn(target, text)
                    if command_name != "get":
                        self.assertEqual(
                            cautious,
                            self.renderer.render_terminal(
                                _fact(descriptor, verified=True)
                            ),
                        )

    def test_dynamic_raw_give_uses_target_neutral_fallback_without_admitting_it(self):
        target = "future_mod_item"
        descriptor = self.factory.decode_registered_command_name_only(
            f"give {target} 2",
            command_source="lavi_gui",
            event_id="c" * 32,
            provider_id="minecraft_fabric_chatclef_ui",
            event_kind="minecraft_raw_gui_submit",
        )

        self.assertIsNotNone(descriptor)
        self.assertEqual(target, descriptor.target_item)
        self.assertEqual("요청한 아이템", descriptor.spoken_target_label)
        detail = CommandLifecyclePresentationDetailProjector().project(descriptor)
        self.assertLessEqual(
            len(detail),
            CommandLifecyclePresentationDetailLogPolicy.MAX_LENGTH,
        )
        self.assertEqual(
            [{"count": 2, "id": target}],
            json.loads(detail)["targets"],
        )
        for text in (
            self.renderer.render_start(descriptor),
            self.renderer.render_status(
                SimpleNamespace(state="running", descriptor=descriptor)
            ),
            self.renderer.render_terminal(_fact(descriptor, verified=False)),
        ):
            self.assertTrue(text)
            self.assertNotIn(target, text)
        self.assertFalse(
            KoreanItemPhraseResolver().resolve(target).get("status")
            == "validated"
        )

    def test_unsupported_policy_rows_remain_unavailable_to_korean_item_admission(self):
        unsupported = {
            target: values
            for target, values in self.policy["targets"].items()
            if values["classification"] == "UNSUPPORTED"
        }
        self.assertTrue(unsupported)
        resolver = KoreanItemPhraseResolver()

        for target, values in unsupported.items():
            with self.subTest(target=target):
                resolution = resolver.resolve(values["display_name"])
                self.assertNotEqual("validated", resolution.get("status"))
                raw_descriptor = self.factory.decode_registered_command_name_only(
                    f"get {target} 1",
                    command_source="lavi_gui",
                    event_id="d" * 32,
                    provider_id="minecraft_fabric_chatclef_ui",
                    event_kind="minecraft_raw_gui_submit",
                )
                self.assertIsNotNone(raw_descriptor)
                self.assertEqual(target, raw_descriptor.target_item)


def _fact(descriptor: object, *, verified: bool):
    return SimpleNamespace(
        descriptor=descriptor,
        status="completed",
        verified=verified,
        dispatch_started=True,
    )


def _raw_item_command(command_name: str, target: str) -> str:
    if command_name == "equip":
        return f"equip {target}"
    if command_name == "give":
        return f"give {target} 2"
    return f"{command_name} {target} 2"


if __name__ == "__main__":
    unittest.main()
