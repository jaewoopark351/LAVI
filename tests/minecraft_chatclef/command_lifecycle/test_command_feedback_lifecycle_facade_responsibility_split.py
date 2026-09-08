#20260907_kpopmodder: Pin the lifecycle facade to compatibility-only delegation.
from __future__ import annotations

import inspect
import unittest

from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.binding.command_feedback_binding_coordinator import (
    CommandFeedbackBindingCoordinator,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.command_feedback_lifecycle_facade import (
    CommandFeedbackLifecycleFacade,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.publication.command_feedback_publication_lifecycle_coordinator import (
    CommandFeedbackPublicationLifecycleCoordinator,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.result.command_feedback_result_state_coordinator import (
    CommandFeedbackResultStateCoordinator,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.state.command_feedback_lifecycle_retirement_coordinator import (
    CommandFeedbackLifecycleRetirementCoordinator,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.status.command_feedback_status_publication_coordinator import (
    CommandFeedbackStatusPublicationCoordinator,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.terminal.command_feedback_terminal_lifecycle_coordinator import (
    CommandFeedbackTerminalLifecycleCoordinator,
)


class CommandFeedbackLifecycleFacadeResponsibilitySplitTests(unittest.TestCase):
    def test_facade_preserves_public_api_without_mutating_shared_state(self):
        source = inspect.getsource(CommandFeedbackLifecycleFacade)
        public_api = {
            "reserve",
            "bind_reserved",
            "abandon_reservation",
            "claim_start",
            "matches_result",
            "record_nonterminal",
            "dispatch_started_observed",
            "inspect",
            "inspect_for_publication",
            "claim_terminal",
            "specialized_stop_owns_terminal",
            "discard_terminal",
            "stage_terminal",
            "acknowledge_publication",
            "select_coalesced_terminal",
            "matches_before_snapshot",
            "clear_if_owner",
            "clear",
        }

        self.assertTrue(
            public_api.issubset(CommandFeedbackLifecycleFacade.__dict__)
        )
        for forbidden in (
            "self._state",
            "self._publications",
            "_accepted_submission_matches",
            "_retire_reservation",
            "_retire_active_lifecycle",
            "_reset_active_state",
        ):
            with self.subTest(forbidden=forbidden):
                self.assertNotIn(forbidden, source)

    def test_internal_state_owners_are_separate_and_add_no_lock(self):
        components = (
            CommandFeedbackBindingCoordinator,
            CommandFeedbackResultStateCoordinator,
            CommandFeedbackStatusPublicationCoordinator,
            CommandFeedbackTerminalLifecycleCoordinator,
            CommandFeedbackPublicationLifecycleCoordinator,
            CommandFeedbackLifecycleRetirementCoordinator,
        )

        self.assertEqual(
            len(components),
            len({component.__module__ for component in components}),
        )
        for component in components:
            source = inspect.getsource(component)
            with self.subTest(component=component.__name__):
                self.assertNotIn("threading", source)
                self.assertNotIn("Lock(", source)
                self.assertNotIn("RLock(", source)


if __name__ == "__main__":
    unittest.main()
