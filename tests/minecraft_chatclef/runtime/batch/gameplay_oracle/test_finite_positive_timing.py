#20260819_kpopmodder: Verify gameplay timing accepts only finite positive non-boolean values.
from __future__ import annotations

import unittest

from .finite_positive_timing import finite_positive_seconds


class FinitePositiveTimingTests(unittest.TestCase):
    def test_bool_nan_infinity_zero_and_negative_values_are_rejected(self):
        cases = (
            True,
            False,
            float("nan"),
            float("inf"),
            float("-inf"),
            "nan",
            "inf",
            "-inf",
            0,
            -1,
            None,
        )
        for value in cases:
            with self.subTest(value=value):
                self.assertIsNone(finite_positive_seconds(value))

    def test_finite_positive_numeric_values_are_normalized(self):
        cases = (
            (1, 1.0),
            (0.25, 0.25),
            ("2.5", 2.5),
        )
        for value, expected in cases:
            with self.subTest(value=value):
                self.assertEqual(expected, finite_positive_seconds(value))


if __name__ == "__main__":
    unittest.main()
