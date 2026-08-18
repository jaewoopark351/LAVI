#20260819_kpopmodder: Reject coercible command and invocation identities in a batch plan.
from __future__ import annotations

import unittest

from .batch_plan_validator import validate_batch_plan


class BatchPlanValidatorTests(unittest.TestCase):
    def test_exact_strings_are_accepted(self):
        plan, error = validate_batch_plan(
            [{"command": "철 1개 캐줘", "invocation_id": "batch-iron"}]
        )

        self.assertEqual("", error)
        self.assertEqual("철 1개 캐줘", plan[0]["command"])
        self.assertIsInstance(plan, tuple)
        with self.assertRaises(TypeError):
            plan[0]["command"] = "변조된 명령"

    def test_coercible_or_padded_values_are_rejected(self):
        cases = (
            ("command", 123),
            ("command", " 철 1개 캐줘"),
            ("invocation_id", 123),
            ("invocation_id", "batch-iron "),
        )
        for field, value in cases:
            with self.subTest(field=field, value=value):
                item = {"command": "철 1개 캐줘", "invocation_id": "batch-iron"}
                item[field] = value

                _plan, error = validate_batch_plan([item])

                self.assertTrue(error)


if __name__ == "__main__":
    unittest.main()
