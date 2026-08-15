#20260815_kpopmodder: Lock representative Phase 0 item command capability policy.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.intent.chatclef_target_catalog import (
    ChatClefTargetCatalog,
)


EQUIP_SET_SHORTCUTS = frozenset(
    {"leather", "iron", "gold", "diamond", "netherite"}
)
EXPLICIT_EQUIPMENT_TARGETS = frozenset(
    {
        "leather_helmet",
        "leather_chestplate",
        "leather_leggings",
        "leather_boots",
        "iron_helmet",
        "iron_chestplate",
        "iron_leggings",
        "iron_boots",
        "golden_helmet",
        "golden_chestplate",
        "golden_leggings",
        "golden_boots",
        "diamond_helmet",
        "diamond_chestplate",
        "diamond_leggings",
        "diamond_boots",
        "netherite_helmet",
        "netherite_chestplate",
        "netherite_leggings",
        "netherite_boots",
    }
)
REPRESENTATIVE_CAPABILITY_TARGETS = {
    "concrete_item": ("diamond", "iron_ingot"),
    "block": ("stone",),
    "equipment": ("iron_chestplate",),
    "generic_aggregate": ("log", "planks"),
    "canonicalized_legacy": ("diamond_pickaxe",),
    "invalid": ("not_a_real_chatclef_target",),
}


class MinecraftChatClefItemCommandCapabilityPolicyTests(unittest.TestCase):
    def test_representative_capability_targets_are_catalog_classified(self):
        catalog = ChatClefTargetCatalog()

        for category, targets in REPRESENTATIVE_CAPABILITY_TARGETS.items():
            for target in targets:
                with self.subTest(category=category, target=target):
                    if category == "invalid":
                        self.assertFalse(catalog.contains(target))
                    else:
                        self.assertTrue(catalog.contains(target))

    def test_equip_set_shortcuts_match_java_contract(self):
        self.assertEqual(
            {"leather", "iron", "gold", "diamond", "netherite"},
            set(EQUIP_SET_SHORTCUTS),
        )
        self.assertNotIn("golden", EQUIP_SET_SHORTCUTS)

    def test_explicit_equip_targets_require_equipment_capability(self):
        self.assertIn("iron_chestplate", EXPLICIT_EQUIPMENT_TARGETS)
        self.assertIn("diamond_boots", EXPLICIT_EQUIPMENT_TARGETS)
        self.assertNotIn("stone", EXPLICIT_EQUIPMENT_TARGETS)
        self.assertNotIn("iron_ingot", EXPLICIT_EQUIPMENT_TARGETS)

    def test_python_v2_does_not_expose_multi_item_equip_initially(self):
        initial_contract = {
            "supports_full_set_shortcut": True,
            "supports_single_explicit_equipment": True,
            "supports_multi_item_equip": False,
        }

        self.assertFalse(initial_contract["supports_multi_item_equip"])

    def test_python_v2_deposit_multi_item_is_intentionally_unsupported_initially(self):
        initial_contract = {
            "supports_bare_deposit": True,
            "supports_single_specific_item": True,
            "supports_multi_item_deposit": False,
        }

        self.assertFalse(initial_contract["supports_multi_item_deposit"])

    def test_give_multi_item_is_not_supported_by_v2_contract(self):
        initial_contract = {
            "supports_single_recipient": True,
            "supports_single_item": True,
            "supports_multi_item_give": False,
        }

        self.assertFalse(initial_contract["supports_multi_item_give"])

    def test_generic_aggregate_targets_remain_unsupported_until_representative_proof(self):
        generic_targets = set(REPRESENTATIVE_CAPABILITY_TARGETS["generic_aggregate"])
        initial_deposit_give_verified_targets = {"diamond", "iron_ingot", "stone"}

        self.assertTrue(generic_targets.isdisjoint(initial_deposit_give_verified_targets))


if __name__ == "__main__":
    unittest.main()
