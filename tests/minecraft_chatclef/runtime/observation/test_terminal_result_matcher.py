#20260819_kpopmodder: Reject coerced request identities and terminal statuses.
from __future__ import annotations

import unittest

from .terminal_result_matcher import is_matching_terminal_result


class TerminalResultMatcherTests(unittest.TestCase):
    def test_exact_request_identity_and_status_match(self):
        self.assertTrue(
            is_matching_terminal_result(
                {"request_id": "request-1", "status": "completed"},
                "request-1",
            )
        )

    def test_non_string_request_identity_is_not_coerced(self):
        for request_id in (123, True, None):
            with self.subTest(request_id=request_id):
                self.assertFalse(
                    is_matching_terminal_result(
                        {"request_id": request_id, "status": "completed"},
                        "123",
                    )
                )

    def test_padded_submitted_request_identity_is_invalid(self):
        self.assertFalse(
            is_matching_terminal_result(
                {"request_id": " request-1 ", "status": "completed"},
                " request-1 ",
            )
        )

    def test_terminal_status_must_be_an_exact_documented_value(self):
        for status in (" completed", "COMPLETED", 1, True, None):
            with self.subTest(status=status):
                self.assertFalse(
                    is_matching_terminal_result(
                        {"request_id": "request-1", "status": status},
                        "request-1",
                    )
                )


if __name__ == "__main__":
    unittest.main()
