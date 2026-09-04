#20260820_kpopmodder: Lock Python Korean command registry axes against ChatClef command drift.
#20260829_openai: Lock raw-only deposit_all metadata and 22-command parity.
#20260905_kpopmodder: Lock H5 metadata, independent readiness, and exact 26-command parity.
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
        self.assertEqual(26, len(registry.command_names()))

    def test_auto_deposit_trust_is_public_with_exact_h5_metadata(self):
        registry = KoreanChatClefCommandRegistry()
        spec = registry.spec("auto_deposit_trust")

        self.assertEqual((), spec.slot_schema)
        self.assertEqual("command_specific", spec.resolver_domain)
        self.assertEqual("immediate", spec.lifecycle_kind)
        self.assertEqual("R2", spec.safety_tier)
        self.assertEqual("none", spec.confirmation_mode)
        self.assertEqual(
            ("lavi_chat_ui", "voice_input_final"),
            spec.allowed_input_sources,
        )
        self.assertEqual("prefixless_auto_deposit_trust", spec.serializer_id)
        self.assertEqual(
            {
                "SOURCE_REGISTERED": True,
                "KOREAN_PARSE_COMPILE_READY": True,
                "PYTHON_ADMISSION_READY": True,
                "BRIDGE_LIFECYCLE_READY": True,
                "GAMEPLAY_EFFECT_VERIFIABLE": False,
                "PUBLIC_KOREAN_ENABLED": True,
            },
            spec.readiness_axes.to_dict(),
        )

    def test_other_h5_registrar_commands_remain_raw_only_shadow_rows(self):
        registry = KoreanChatClefCommandRegistry()
        expectations = {
            "auto_deposit_untrust": (("destinationId?",), "R2"),
            "auto_deposit_trusted_list": ((), "R0"),
            "자동보관등록": ((), "R2"),
        }

        for command, (slot_schema, safety_tier) in expectations.items():
            with self.subTest(command=command):
                spec = registry.spec(command)

                self.assertEqual(slot_schema, spec.slot_schema)
                self.assertEqual("command_specific", spec.resolver_domain)
                self.assertEqual("immediate", spec.lifecycle_kind)
                self.assertEqual(safety_tier, spec.safety_tier)
                self.assertEqual("none", spec.confirmation_mode)
                self.assertEqual((), spec.allowed_input_sources)
                self.assertEqual(f"prefixless_{command}", spec.serializer_id)
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

    def test_public_readiness_memberships_are_independent_and_consistent(self):
        registry = KoreanChatClefCommandRegistry()

        self.assertIsNot(
            registry._PUBLIC_KOREAN_COMMANDS,
            registry._PARSER_READY_COMMANDS,
        )
        self.assertIsNot(
            registry._PUBLIC_KOREAN_COMMANDS,
            registry._PYTHON_ADMISSION_READY_COMMANDS,
        )
        self.assertLessEqual(
            registry.public_korean_command_names(),
            registry._PARSER_READY_COMMANDS
            & registry._PYTHON_ADMISSION_READY_COMMANDS,
        )

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
            ("lavi_chat_ui", "voice_input_final", "direct_typed"),
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
                self.assertEqual(
                    (
                        "lavi_chat_ui",
                        "voice_input_final",
                        "direct_typed",
                        "lavi_gui_korean",
                    ),
                    spec.allowed_input_sources,
                )

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
