#20260819_kpopmodder: Lock untrusted submit-result mirrors behind one canonical fail-closed parser.
from __future__ import annotations

import copy
import unittest

from plugins.Minecraft.fabric.chatclef.input.routing.submission.submission_result_normalizer import (
    MinecraftChatClefSubmissionResultNormalizer,
)


class SubmissionResultNormalizerTests(unittest.TestCase):
    def setUp(self):
        self.normalizer = MinecraftChatClefSubmissionResultNormalizer()

    def test_valid_result_is_rebuilt_with_fresh_nested_mappings(self):
        payload = _submit_payload(
            data={"command": "get stone 1", "nested": {"count": 1}}
        )

        normalized = self.normalizer.normalize(
            payload,
            expected_request_id="request-1",
        )

        self.assertEqual(payload, normalized)
        self.assertIsNot(payload, normalized)
        self.assertIsNot(payload["status"], normalized["status"])
        self.assertIsNot(payload["status"]["data"], normalized["status"]["data"])
        self.assertIsNot(payload["details"], normalized["details"])
        payload["status"]["data"]["nested"]["count"] = 99
        self.assertEqual(1, normalized["status"]["data"]["nested"]["count"])

    def test_valid_rejected_and_unknown_results_remain_explicit(self):
        rejected = self.normalizer.normalize(
            _submit_payload(
                ok=False,
                status="rejected",
                error_code="invalid_request",
                message="rejected",
            ),
            expected_request_id="request-1",
        )
        unknown = self.normalizer.normalize(
            _submit_payload(
                ok=False,
                status="unknown",
                error_code="internal_error",
                message="outcome unknown",
                data={
                    "submission_outcome": "submission_outcome_unknown",
                    "reconciliation_required": True,
                },
            ),
            expected_request_id="request-1",
        )

        self.assertEqual("rejected", rejected["status"]["status"])
        self.assertFalse(rejected["ok"])
        self.assertEqual("unknown", unknown["status"]["status"])
        self.assertTrue(unknown["details"]["reconciliation_required"])

    def test_outer_and_nested_boolean_contradictions_are_unknown(self):
        cases = {}

        outer_true_nested_false = _submit_payload()
        outer_true_nested_false["status"]["ok"] = False
        cases["outer_true_nested_false"] = outer_true_nested_false

        outer_false_nested_true = _submit_payload()
        outer_false_nested_true["ok"] = False
        cases["outer_false_nested_true"] = outer_false_nested_true

        rejected_nested_true = _submit_payload(
            ok=False,
            status="rejected",
            error_code="invalid_request",
            message="rejected",
        )
        rejected_nested_true["status"]["ok"] = True
        cases["rejected_nested_true"] = rejected_nested_true

        for name, payload in cases.items():
            with self.subTest(name=name):
                self._assert_unknown(payload)

    def test_string_and_integer_boolean_values_are_unknown(self):
        for location in ("outer", "nested"):
            for value in ("false", 0, 1):
                with self.subTest(location=location, value=value):
                    payload = _submit_payload()
                    if location == "outer":
                        payload["ok"] = value
                    else:
                        payload["status"]["ok"] = value
                    self._assert_unknown(payload)

    def test_success_with_error_code_is_unknown(self):
        self._assert_unknown(
            _submit_payload(error_code="internal_error")
        )

    def test_unknown_status_or_error_enum_is_unknown(self):
        unknown_status = _submit_payload()
        unknown_status["status"]["status"] = "invented"
        unknown_error = _submit_payload(
            ok=False,
            status="rejected",
            error_code="invented_error",
            message="rejected",
        )

        self._assert_unknown(unknown_status)
        self._assert_unknown(unknown_error)

    def test_request_identity_mismatch_is_unknown(self):
        top_level_mismatch = _submit_payload()
        top_level_mismatch["request_id"] = "request-2"

        self._assert_unknown(top_level_mismatch)
        result = self.normalizer.normalize(
            _submit_payload(),
            expected_request_id="request-2",
        )
        self.assertEqual("unknown", result["status"]["status"])
        self.assertEqual("request-2", result["status"]["request_id"])

    def test_error_message_and_details_mirror_mismatches_are_unknown(self):
        error_mismatch = _submit_payload(
            ok=False,
            status="rejected",
            error_code="invalid_request",
            message="rejected",
        )
        error_mismatch["error"] = "internal_error"

        message_mismatch = _submit_payload()
        message_mismatch["message"] = "different"

        details_mismatch = _submit_payload(data={"command": "get stone 1"})
        details_mismatch["details"] = {"command": "get coal 1"}

        for name, payload in {
            "error": error_mismatch,
            "message": message_mismatch,
            "details": details_mismatch,
        }.items():
            with self.subTest(name=name):
                self._assert_unknown(payload)

    def test_malformed_nested_status_is_unknown(self):
        for status in (None, "accepted", [], {}):
            with self.subTest(status=status):
                payload = _submit_payload()
                payload["status"] = status
                self._assert_unknown(payload)

    def test_missing_required_mirror_fields_are_unknown(self):
        cases = {}
        for location, field in (
            ("outer", "error"),
            ("outer", "message"),
            ("outer", "details"),
            ("nested", "error_code"),
            ("nested", "message"),
            ("nested", "data"),
        ):
            payload = _submit_payload()
            target = payload if location == "outer" else payload["status"]
            del target[field]
            cases[f"{location}_{field}"] = payload

        for name, payload in cases.items():
            with self.subTest(name=name):
                self._assert_unknown(payload)

    def test_non_string_or_padded_request_identity_is_unknown(self):
        for request_id in (1, False, " request-1", "request-1 ", ""):
            with self.subTest(request_id=request_id):
                self._assert_unknown(_submit_payload(request_id=request_id))

    def test_contradictory_submission_outcome_is_unknown(self):
        payload = _submit_payload(
            data={
                "submission_outcome": "submission_outcome_unknown",
                "reconciliation_required": True,
            }
        )

        self._assert_unknown(payload)

    def test_request_id_can_be_bound_from_nested_status_for_one_shot(self):
        normalized = self.normalizer.normalize(
            _submit_payload(request_id="runtime-generated"),
            expected_request_id=None,
        )

        self.assertEqual("runtime-generated", normalized["status"]["request_id"])
        self.assertEqual("accepted", normalized["status"]["status"])

    def _assert_unknown(self, payload):
        normalized = self.normalizer.normalize(
            copy.deepcopy(payload),
            expected_request_id="request-1",
        )
        self.assertFalse(normalized["ok"])
        self.assertEqual("unknown", normalized["status"]["status"])
        self.assertFalse(normalized["status"]["ok"])
        self.assertEqual("internal_error", normalized["status"]["error_code"])
        self.assertEqual("internal_error", normalized["error"])
        self.assertEqual(
            "submission_outcome_unknown",
            normalized["details"]["submission_outcome"],
        )
        self.assertTrue(normalized["details"]["reconciliation_required"])


def _submit_payload(
    *,
    request_id="request-1",
    ok=True,
    status="accepted",
    error_code=None,
    message="accepted",
    data=None,
):
    nested_data = copy.deepcopy(data or {})
    return {
        "ok": ok,
        "status": {
            "request_id": request_id,
            "ok": ok,
            "status": status,
            "error_code": error_code,
            "message": message,
            "data": nested_data,
        },
        "error": error_code,
        "message": message,
        "details": copy.deepcopy(nested_data),
    }


if __name__ == "__main__":
    unittest.main()
