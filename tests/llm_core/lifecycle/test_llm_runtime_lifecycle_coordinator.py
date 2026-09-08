# 20260905_kpopmodder: Verifies interrupt and shutdown ordering behind the LLM facade.
from __future__ import annotations

import unittest

from llm_core.lifecycle import LlmRuntimeLifecycleCoordinator
from llm_core.llm_component import LLM


class LlmRuntimeLifecycleCoordinatorTests(unittest.TestCase):
    def test_shutdown_is_idempotent_and_preserves_owned_cleanup_order(self):
        calls = []
        state = {"shutdown": False, "subscription": _Subscription(calls)}
        coordinator = LlmRuntimeLifecycleCoordinator(
            clear_pending_inputs_callback=lambda: calls.append("clear_queue"),
            request_interrupt_callback=lambda: calls.append("interrupt"),
            clear_listeners_callback=lambda: calls.append("clear_listeners"),
            interrupt_message_callback=lambda: calls.append("message"),
            input_thread_callback=lambda: None,
            shutdown_state_callback=lambda: state["shutdown"],
            mark_shutdown_callback=lambda: state.__setitem__("shutdown", True),
            interrupt_subscription_callback=lambda: state["subscription"],
            clear_interrupt_subscription_callback=lambda: state.__setitem__(
                "subscription",
                None,
            ),
            base_shutdown_callback=lambda: calls.append("base_shutdown"),
            clear_pending_presentations_callback=(
                lambda: calls.append("clear_presentations")
            ),
        )

        coordinator.shutdown()
        coordinator.shutdown()

        self.assertEqual(
            [
                "unsubscribe",
                "clear_queue",
                "clear_presentations",
                "interrupt",
                "clear_listeners",
                "base_shutdown",
            ],
            calls,
        )
        self.assertTrue(state["shutdown"])
        self.assertIsNone(state["subscription"])

    def test_llm_interrupt_and_shutdown_methods_remain_thin_delegates(self):
        calls = []
        llm = LLM.__new__(LLM)
        llm.runtime_lifecycle_coordinator = _LifecycleFake(calls)

        llm.handle_interrupt()
        llm.shutdown()

        self.assertEqual(["interrupt", "shutdown"], calls)


class _Subscription:
    def __init__(self, calls):
        self._calls = calls

    def unsubscribe(self):
        self._calls.append("unsubscribe")


class _LifecycleFake:
    def __init__(self, calls):
        self._calls = calls

    def handle_interrupt(self):
        self._calls.append("interrupt")

    def shutdown(self):
        self._calls.append("shutdown")


if __name__ == "__main__":
    unittest.main()
