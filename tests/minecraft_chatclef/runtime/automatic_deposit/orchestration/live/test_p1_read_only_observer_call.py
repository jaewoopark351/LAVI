#20260901_kpopmodder: Lock every P1 read-only observer call to one bounded result shape.
from __future__ import annotations

import unittest

from .p1_read_only_observer_call import call_p1_read_only_observer


class P1ReadOnlyObserverCallTests(unittest.TestCase):
    def test_returns_one_value_and_bounded_source_reason(self):
        marker = object()

        result = call_p1_read_only_observer(
            "status_reader",
            lambda: (marker, "PRODUCTION_FABRIC_STATUS_OBSERVED"),
        )

        self.assertTrue(result.ok)
        self.assertIs(marker, result.value)
        self.assertEqual("PRODUCTION_FABRIC_STATUS_OBSERVED", result.source_reason)

    def test_none_value_is_explicitly_inconclusive(self):
        result = call_p1_read_only_observer(
            "trusted_fixture_reader",
            lambda: (None, "P1_PLAYER_DATA_STALE"),
        )

        self.assertFalse(result.ok)
        self.assertEqual("P1_LIVE_OBSERVER_REPORTED_INCONCLUSIVE", result.reason)
        self.assertEqual("P1_PLAYER_DATA_STALE", result.source_reason)

    def test_exception_is_fail_closed_without_leaking_message(self):
        def raise_error():
            raise RuntimeError("private path and payload")

        result = call_p1_read_only_observer("log_delta_reader", raise_error)

        self.assertFalse(result.ok)
        self.assertEqual("P1_LIVE_OBSERVER_CALL_FAILED", result.reason)
        self.assertEqual("RuntimeError", result.error_type)
        self.assertNotIn("private", repr(result))

    def test_malformed_result_or_reason_is_rejected(self):
        cases = (
            (lambda: object(), "P1_LIVE_OBSERVER_RESULT_SHAPE_INVALID"),
            (lambda: (object(), ""), "P1_LIVE_OBSERVER_SOURCE_REASON_INVALID"),
            (
                lambda: (object(), "x" * 161),
                "P1_LIVE_OBSERVER_SOURCE_REASON_INVALID",
            ),
            (
                lambda: (object(), "reason\nwith-control"),
                "P1_LIVE_OBSERVER_SOURCE_REASON_INVALID",
            ),
        )
        for reader, expected_reason in cases:
            with self.subTest(expected_reason=expected_reason):
                result = call_p1_read_only_observer("status_reader", reader)

                self.assertFalse(result.ok)
                self.assertEqual(expected_reason, result.reason)


if __name__ == "__main__":
    unittest.main()
