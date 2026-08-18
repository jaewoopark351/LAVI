#20260818_kpopmodder: Verify batch approval JSON parsing has no validation side effects.
from __future__ import annotations

import unittest

from .batch_approval_parser import parse_batch_approval_record


class BatchApprovalParserTests(unittest.TestCase):
    def test_json_object_is_parsed(self):
        approval, error = parse_batch_approval_record(
            '{"approval_source":"operator"}'
        )

        self.assertEqual("", error)
        self.assertEqual("operator", approval["approval_source"])

    def test_non_object_json_is_rejected(self):
        approval, error = parse_batch_approval_record("[]")

        self.assertEqual({}, approval)
        self.assertIn("JSON object", error)


if __name__ == "__main__":
    unittest.main()
