#20260907_kpopmodder: Verify only bounded descriptor slots reach UI detail.
from __future__ import annotations

import ast
import json
import unittest
from pathlib import Path

from plugins.Minecraft.fabric.chatclef.presentation.command_lifecycle import (
    CommandLifecyclePresentationDetailLogPolicy,
    CommandLifecyclePresentationDetailProjector,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleResponseRenderer,
    CommandLifecycleStartResponseFactory,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackDescriptor,
    CommandFeedbackTarget,
)


class CommandLifecyclePresentationDetailTests(unittest.TestCase):
    def test_policy_rejects_printable_raw_text_and_unknown_json_fields(self):
        for value in (
            "get diamond_pickaxe 1 SECRET_RAW",
            '{"command_name":"get","form_kind":"target_count",'
            '"raw_text":"SECRET_RAW"}',
        ):
            with self.subTest(value=value):
                with self.assertRaises(ValueError):
                    CommandLifecyclePresentationDetailLogPolicy.validate(value)

    def test_projector_uses_only_validated_slots_and_never_raw_command(self):
        descriptor = _descriptor(
            command="get diamond_pickaxe 2 SECRET_RAW",
            target_item="diamond_pickaxe",
            requested_count=2,
            player_name="Steve",
            coordinate_values=(10, 64, -5),
            setting_value="unsafe raw\nvalue",
        )

        detail_log = CommandLifecyclePresentationDetailProjector().project(
            descriptor
        )
        detail = json.loads(detail_log)

        self.assertEqual("get", detail["command_name"])
        self.assertEqual("target_count", detail["form_kind"])
        self.assertEqual("diamond_pickaxe", detail["target"])
        self.assertEqual(2, detail["requested_count"])
        self.assertEqual("Steve", detail["player"])
        self.assertEqual([10, 64, -5], detail["coordinates"])
        self.assertNotIn("setting_value", detail)
        self.assertNotIn("SECRET_RAW", detail_log)
        self.assertNotIn("unsafe raw", detail_log)

    def test_many_targets_are_truncated_only_at_entry_boundaries(self):
        targets = tuple(
            CommandFeedbackTarget(
                canonical_target=f"canonical_item_{index:03d}",
                requested_count=index + 1,
                quantity_semantics="acquire_delta",
                spoken_label="요청한 아이템",
            )
            for index in range(100)
        )
        descriptor = _descriptor(targets=targets, target_item=None)

        detail_log = CommandLifecyclePresentationDetailProjector().project(
            descriptor
        )
        detail = json.loads(detail_log)

        self.assertLessEqual(
            len(detail_log),
            CommandLifecyclePresentationDetailLogPolicy.MAX_LENGTH,
        )
        self.assertEqual(100, detail["target_entry_count"])
        self.assertTrue(detail["targets_truncated"])
        self.assertLess(len(detail["targets"]), 100)
        self.assertTrue(
            all(set(target) == {"count", "id"} for target in detail["targets"])
        )

    def test_start_factory_keeps_canonical_id_out_of_body(self):
        descriptor = _descriptor(
            target_item="diamond_pickaxe",
            requested_count=1,
        )

        response = CommandLifecycleStartResponseFactory(
            response_renderer=CommandLifecycleResponseRenderer()
        ).build(descriptor)

        self.assertNotIn("diamond_pickaxe", response.text)
        self.assertEqual(
            "diamond_pickaxe",
            json.loads(response.presentation_detail_log)["target"],
        )

    def test_plugin_presentation_paths_do_not_import_llm_core(self):
        project_root = Path(__file__).resolve().parents[3]
        roots = (
            project_root
            / "plugins/Minecraft/fabric/chatclef/presentation/command_lifecycle",
            project_root
            / "plugins/Minecraft/fabric/chatclef/response/command_lifecycle",
        )

        for root in roots:
            for path in root.rglob("*.py"):
                tree = ast.parse(path.read_text(encoding="utf-8"))
                imports = {
                    node.module
                    for node in ast.walk(tree)
                    if isinstance(node, ast.ImportFrom)
                    and type(node.module) is str
                }
                imports.update(
                    alias.name
                    for node in ast.walk(tree)
                    if isinstance(node, ast.Import)
                    for alias in node.names
                )
                with self.subTest(path=path):
                    self.assertFalse(
                        any(name.startswith("llm_core") for name in imports)
                    )


def _descriptor(
    *,
    command="get diamond_pickaxe 1",
    target_item="diamond_pickaxe",
    requested_count=1,
    player_name="",
    coordinate_values=(),
    setting_value="",
    targets=(),
):
    return CommandFeedbackDescriptor(
        command_name="get",
        command=command,
        command_source="lavi_gui",
        lifecycle_kind="task",
        phrase_profile_id="get_phrase_v1",
        evidence_profile_id="get_effect_v1",
        rollout_state="verified",
        event_id="a" * 32,
        input_source="lavi_gui",
        provider_id="minecraft_fabric_chatclef_ui",
        event_kind="minecraft_raw_gui_submit",
        requested_family="item_get",
        target_item=target_item,
        requested_count=requested_count,
        quantity_semantics="acquire_delta",
        acquisition_verb_class="craft",
        spoken_target_label="다이아 곡괭이",
        detail_level="raw_typed",
        form_kind="target_count",
        targets=targets,
        player_name=player_name,
        coordinate_values=coordinate_values,
        setting_value=setting_value,
    )


if __name__ == "__main__":
    unittest.main()
