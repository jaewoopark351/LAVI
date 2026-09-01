#20260901_kpopmodder: Lock exact approval of the selected one-shot endpoint kind.
from __future__ import annotations

import unittest

from ...submission.command_submission_transport import (
    CommandSubmissionTransport,
)
from .approved_command_transport_contract import (
    validate_approved_command_transport,
)


class ApprovedCommandTransportContractTests(unittest.TestCase):
    def test_exact_approved_transport_matches_selected_enum(self):
        for selected in CommandSubmissionTransport:
            with self.subTest(selected=selected):
                self.assertEqual(
                    "",
                    validate_approved_command_transport(
                        selected.value,
                        selected,
                    ),
                )

    def test_route_mismatch_is_rejected(self):
        self.assertEqual(
            "approved command transport does not match selected transport",
            validate_approved_command_transport(
                CommandSubmissionTransport.KOREAN.value,
                CommandSubmissionTransport.RAW,
            ),
        )

    def test_malformed_approved_or_selected_transport_is_rejected(self):
        for approved, selected, expected in (
            (None, CommandSubmissionTransport.KOREAN, "approved"),
            (" raw", CommandSubmissionTransport.RAW, "approved"),
            ("other", CommandSubmissionTransport.RAW, "approved"),
            ("raw", "raw", "selected"),
            ("raw", None, "selected"),
        ):
            with self.subTest(approved=approved, selected=selected):
                self.assertIn(
                    expected,
                    validate_approved_command_transport(approved, selected),
                )


if __name__ == "__main__":
    unittest.main()
