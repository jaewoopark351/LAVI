#20260831_kpopmodder: Lock bounded and ambiguity-free diagnostic parsing.
from __future__ import annotations

import unittest

from .diagnostic_event_parser import parse_bounded_diagnostic_event


class AutomaticDepositDiagnosticEventParserTests(unittest.TestCase):
    def test_parses_one_complete_bounded_key_value_record(self):
        parsed = parse_bounded_diagnostic_event(
            "[12:00:00] [LAVI ChatClefDiag] operationId=op-1 "
            "event=terminal diagnosticCaptureStatus=complete"
        )

        self.assertTrue(parsed.ok)
        self.assertEqual("op-1", parsed.as_mapping()["operationId"])

    def test_duplicate_key_is_inconclusive_instead_of_guessed(self):
        parsed = parse_bounded_diagnostic_event(
            "[LAVI ChatClefDiag] event=start event=terminal"
        )

        self.assertFalse(parsed.ok)
        self.assertEqual("DIAGNOSTIC_DUPLICATE_KEY", parsed.reason)

    def test_unstructured_space_containing_value_is_rejected(self):
        parsed = parse_bounded_diagnostic_event(
            "[LAVI ChatClefDiag] reason=no safe surplus"
        )

        self.assertFalse(parsed.ok)
        self.assertEqual("DIAGNOSTIC_TOKEN_AMBIGUOUS", parsed.reason)

    def test_oversized_record_is_rejected_before_parsing(self):
        parsed = parse_bounded_diagnostic_event(
            "[LAVI ChatClefDiag] value=" + ("x" * 100),
            max_utf8_bytes=32,
        )

        self.assertFalse(parsed.ok)
        self.assertEqual("DIAGNOSTIC_RECORD_EXCEEDS_BYTE_BOUND", parsed.reason)

    def test_known_complete_no_candidate_capture_status_is_accepted(self):
        parsed = parse_bounded_diagnostic_event(
            "[LAVI ChatClefBoundary] event=timeout "
            "diagnosticCaptureStatus=complete_no_active_candidate"
        )

        self.assertTrue(parsed.ok)

    def test_decodes_percent_encoded_whitespace_equals_and_unicode(self):
        parsed = parse_bounded_diagnostic_event(
            "[LAVI ChatClefBoundary] threadName=Render%20thread "
            "detail=a%3Db%20%ED%95%9C%EA%B8%80 diagnosticCaptureStatus=complete"
        )

        self.assertTrue(parsed.ok)
        self.assertEqual("Render thread", parsed.as_mapping()["threadName"])
        self.assertEqual("a=b 한글", parsed.as_mapping()["detail"])

    def test_rejects_malformed_percent_encoding(self):
        parsed = parse_bounded_diagnostic_event(
            "[LAVI ChatClefBoundary] detail=bad%2 diagnosticCaptureStatus=complete"
        )

        self.assertFalse(parsed.ok)
        self.assertEqual("DIAGNOSTIC_VALUE_ENCODING_INVALID", parsed.reason)

    def test_decodes_the_canonical_empty_value_without_accepting_control_bytes(self):
        parsed = parse_bounded_diagnostic_event(
            "[LAVI ChatClefBoundary] detail=~EMPTY~ "
            "diagnosticCaptureStatus=complete"
        )
        nul = parse_bounded_diagnostic_event(
            "[LAVI ChatClefBoundary] detail=%00 "
            "diagnosticCaptureStatus=complete"
        )

        self.assertTrue(parsed.ok)
        self.assertEqual("", parsed.as_mapping()["detail"])
        self.assertFalse(nul.ok)
        self.assertEqual("DIAGNOSTIC_VALUE_ENCODING_INVALID", nul.reason)


if __name__ == "__main__":
    unittest.main()
