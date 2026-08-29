#20260820_kpopmodder: Lock Python Korean command registry axes against ChatClef command drift.
#20260829_openai: Lock raw-only deposit_all metadata and 22-command parity.
from __future__ import annotations

import json
import unittest
from pathlib import Path

from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)


ARTIFACT_ROOT = Path(__file__).resolve().parent
REGISTERED_COMMANDS_ARTIFACT = (
    ARTIFACT_ROOT / "chatclef_registered_commands.snapshot.json"
)


class PythonKoreanCommandRegistryTests(unittest.TestCase):
    def test_registry_contains_exactly_the_registered_chatclef_commands(self):
        registry = KoreanChatClefCommandRegistry()
        expected = {
            row["command"]
            for row in json.loads(
                REGISTERED_COMMANDS_ARTIFACT.read_text(encoding="utf-8")
            )["commands"]
        }

        self.assertEqual(expected, set(registry.command_names()))
        self.assertEqual(22, len(registry.command_names()))

    def test_deposit_all_is_raw_only_shadow_metadata(self):
        registry = KoreanChatClefCommandRegistry()
        spec = registry.spec("deposit_all")

        self.assertEqual(("items?",), spec.slot_schema)
        self.assertEqual("command_specific", spec.resolver_domain)
        self.assertEqual("task", spec.lifecycle_kind)
        self.assertEqual("R2", spec.safety_tier)
        self.assertEqual("none", spec.confirmation_mode)
        self.assertEqual((), spec.allowed_input_sources)
        self.assertEqual("prefixless_deposit_all", spec.serializer_id)
        self.assertEqual(
            {
                "SOURCE_REGISTERED": True,
                "KOREAN_PARSE_COMPILE_READY": False,
                "PYTHON_ADMISSION_READY": False,
                "BRIDGE_LIFECYCLE_READY": False,
                "GAMEPLAY_EFFECT_VERIFIABLE": False,
                "PUBLIC_KOREAN_ENABLED": False,
            },
            spec.readiness_axes.to_dict(),
        )
        self.assertNotIn("deposit_all", registry.public_korean_command_names())

    def test_store_home_is_ready_for_public_korean_submission(self):
        spec = KoreanChatClefCommandRegistry().spec("store_home")

        self.assertEqual((), spec.slot_schema)
        self.assertEqual("command_specific", spec.resolver_domain)
        self.assertEqual("task", spec.lifecycle_kind)
        self.assertEqual("R2", spec.safety_tier)
        self.assertEqual("none", spec.confirmation_mode)
        self.assertEqual(
            ("lavi_chat_mic_router", "direct_typed"),
            spec.allowed_input_sources,
        )
        self.assertEqual("prefixless_store_home", spec.serializer_id)
        self.assertTrue(spec.readiness_axes.source_registered)
        self.assertTrue(spec.readiness_axes.korean_parse_compile_ready)
        self.assertTrue(spec.readiness_axes.python_admission_ready)
        self.assertTrue(spec.readiness_axes.bridge_lifecycle_ready)
        self.assertTrue(spec.readiness_axes.gameplay_effect_verifiable)
        self.assertTrue(spec.readiness_axes.public_korean_enabled)

    def test_item_action_commands_are_public_but_have_command_specific_slots(self):
        registry = KoreanChatClefCommandRegistry()

        expectations = {
            "get": ("item_target", ("item", "count")),
            "equip": ("equipment_item_target", ("equipment_item",)),
            "deposit": ("item_target", ("item", "count")),
            "give": ("player_and_item_target", ("player", "item", "count")),
        }
        for command, (resolver_domain, slot_schema) in expectations.items():
            with self.subTest(command=command):
                spec = registry.spec(command)

                self.assertTrue(spec.readiness_axes.source_registered)
                self.assertTrue(spec.readiness_axes.korean_parse_compile_ready)
                self.assertTrue(spec.readiness_axes.python_admission_ready)
                self.assertTrue(spec.readiness_axes.public_korean_enabled)
                self.assertEqual(resolver_domain, spec.resolver_domain)
                self.assertEqual(slot_schema, spec.slot_schema)
                self.assertEqual(f"prefixless_{command}", spec.serializer_id)

    def test_high_risk_commands_are_registered_without_public_korean_enablement(self):
        registry = KoreanChatClefCommandRegistry()

        for command in {
            "attack",
            "chatclef",
            "follow",
            "gamer",
            "hero",
            "idle",
            "reload_settings",
            "resetmemory",
        }:
            with self.subTest(command=command):
                spec = registry.spec(command)

                self.assertTrue(spec.readiness_axes.source_registered)
                self.assertFalse(spec.readiness_axes.public_korean_enabled)
                self.assertIn(spec.safety_tier, {"R3", "R4"})
                self.assertEqual(
                    "direct_typed_confirmation_required",
                    spec.confirmation_mode,
                )

    def test_control_or_lifecycle_open_commands_are_parse_ready_but_not_public(self):
        registry = KoreanChatClefCommandRegistry()

        expectations = {
            "follow": ("direct_typed_confirmation_required", ("direct_typed_only",)),
            "idle": ("direct_typed_confirmation_required", ("direct_typed_only",)),
            "stop": ("none", ("direct_typed",)),
        }
        for command, (confirmation_mode, allowed_sources) in expectations.items():
            with self.subTest(command=command):
                spec = registry.spec(command)

                self.assertTrue(spec.readiness_axes.source_registered)
                self.assertTrue(spec.readiness_axes.korean_parse_compile_ready)
                self.assertFalse(spec.readiness_axes.python_admission_ready)
                self.assertFalse(spec.readiness_axes.public_korean_enabled)
                self.assertEqual(confirmation_mode, spec.confirmation_mode)
                self.assertEqual(allowed_sources, spec.allowed_input_sources)


if __name__ == "__main__":
    unittest.main()
