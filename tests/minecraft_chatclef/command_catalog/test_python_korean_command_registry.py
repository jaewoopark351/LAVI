#20260820_kpopmodder: Lock Python Korean command registry axes against ChatClef command drift.
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
        self.assertEqual(20, len(registry.command_names()))

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
            "gamer",
            "hero",
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


if __name__ == "__main__":
    unittest.main()
