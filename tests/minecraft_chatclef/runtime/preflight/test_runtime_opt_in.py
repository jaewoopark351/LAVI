#20260819_kpopmodder: Prove live mutation opt-ins require exact booleans.
from __future__ import annotations

import unittest

from .runtime_opt_in import live_mutating_run_selected


class RuntimeOptInTests(unittest.TestCase):
    def test_only_two_exact_true_values_select_a_mutating_run(self):
        self.assertTrue(
            live_mutating_run_selected(
                {"live_opt_in": True, "mutating_opt_in": True}
            )
        )

    def test_truthy_non_boole_never_select_a_mutating_run(self):
        for value in ("1", "false", 1, 1.0, [], {}):
            with self.subTest(value=value):
                self.assertFalse(
                    live_mutating_run_selected(
                        {"live_opt_in": value, "mutating_opt_in": True}
                    )
                )
                self.assertFalse(
                    live_mutating_run_selected(
                        {"live_opt_in": True, "mutating_opt_in": value}
                    )
                )


if __name__ == "__main__":
    unittest.main()
