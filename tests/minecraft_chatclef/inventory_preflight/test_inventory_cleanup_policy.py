#20260819_kpopmodder: Lock Python-only targeted cleanup policy before primary submission.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.orchestration import (
    ChatClefInventoryCleanupPolicy,
    InventoryCleanupPostcondition,
    InventorySnapshot,
    InventoryStack,
)


class ChatClefInventoryCleanupPolicyTests(unittest.TestCase):
    def test_unknown_inventory_never_triggers_cleanup(self):
        decision = ChatClefInventoryCleanupPolicy().plan_before_primary(
            snapshot=InventorySnapshot.unknown(),
            primary_target="iron_ingot",
        )

        self.assertFalse(decision.cleanup_allowed)
        self.assertTrue(decision.primary_allowed)
        self.assertIsNone(decision.cleanup_plan)
        self.assertEqual("inventory_unknown_no_cleanup", decision.reason)

    def test_full_inventory_creates_one_targeted_deposit_for_safe_stack(self):
        snapshot = InventorySnapshot.full(
            stacks=(
                InventoryStack("iron_ingot", 10, slot="0"),
                InventoryStack("dirt", 32, slot="1"),
                InventoryStack("torch", 16, slot="2", tags=frozenset({"torch"})),
            )
        )

        decision = ChatClefInventoryCleanupPolicy().plan_before_primary(
            snapshot=snapshot,
            primary_target="iron_ingot",
        )

        self.assertTrue(decision.cleanup_allowed)
        self.assertFalse(decision.primary_allowed)
        self.assertIsNotNone(decision.cleanup_plan)
        self.assertEqual("deposit dirt 32", decision.cleanup_plan.command)

    def test_full_inventory_without_safe_target_blocks_primary(self):
        snapshot = InventorySnapshot.full(
            stacks=(
                InventoryStack("iron_ingot", 10, slot="0"),
                InventoryStack("torch", 16, slot="1", tags=frozenset({"torch"})),
                InventoryStack("diamond", 1, slot="2", protected=True),
            )
        )

        decision = ChatClefInventoryCleanupPolicy().plan_before_primary(
            snapshot=snapshot,
            primary_target="iron_ingot",
        )

        self.assertFalse(decision.cleanup_allowed)
        self.assertFalse(decision.primary_allowed)
        self.assertEqual("no_safe_cleanup_target", decision.reason)

    def test_cleanup_completed_alone_does_not_allow_primary(self):
        postcondition = InventoryCleanupPostcondition(
            cleanup_request_accepted=True,
            cleanup_terminal_completed=True,
            active_command_cleared=True,
            operation_id_matches=True,
            cleanup_attempt_count=1,
            primary_was_accepted=False,
            post_cleanup_snapshot=InventorySnapshot.unknown(),
            required_free_slots=1,
            protected_item_preservation_verified=True,
        )

        decision = ChatClefInventoryCleanupPolicy().may_submit_primary_after_cleanup(
            postcondition
        )

        self.assertFalse(decision.primary_allowed)
        self.assertEqual("post_cleanup_inventory_not_available", decision.reason)

    def test_post_cleanup_primary_requires_protected_item_verification(self):
        postcondition = InventoryCleanupPostcondition(
            cleanup_request_accepted=True,
            cleanup_terminal_completed=True,
            active_command_cleared=True,
            operation_id_matches=True,
            cleanup_attempt_count=1,
            primary_was_accepted=False,
            post_cleanup_snapshot=InventorySnapshot.available(free_slots=1),
            required_free_slots=1,
            protected_item_preservation_verified=None,
        )

        decision = ChatClefInventoryCleanupPolicy().may_submit_primary_after_cleanup(
            postcondition
        )

        self.assertFalse(decision.primary_allowed)
        self.assertEqual("protected_preservation_unverified", decision.reason)

    def test_verified_post_cleanup_allows_primary_once(self):
        postcondition = InventoryCleanupPostcondition(
            cleanup_request_accepted=True,
            cleanup_terminal_completed=True,
            active_command_cleared=True,
            operation_id_matches=True,
            cleanup_attempt_count=1,
            primary_was_accepted=False,
            post_cleanup_snapshot=InventorySnapshot.available(free_slots=2),
            required_free_slots=1,
            protected_item_preservation_verified=True,
        )

        decision = ChatClefInventoryCleanupPolicy().may_submit_primary_after_cleanup(
            postcondition
        )

        self.assertFalse(decision.cleanup_allowed)
        self.assertTrue(decision.primary_allowed)
        self.assertEqual("post_cleanup_verified", decision.reason)


if __name__ == "__main__":
    unittest.main()
