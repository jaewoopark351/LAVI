#20260819_kpopmodder: Reject coercible or non-finite live preflight inputs.
from __future__ import annotations

import unittest

from .preflight_environment_validator import validate_preflight_environment
from .preflight_fixture_factory import live_environment_fixture


class PreflightEnvironmentValidatorTests(unittest.TestCase):
    def test_fixture_is_valid(self):
        self.assertEqual("", validate_preflight_environment(live_environment_fixture()))

    def test_required_text_fields_are_exact_strings(self):
        for field in (
            "gradio_url",
            "command",
            "expected_backend",
            "expected_instance",
            "expected_world",
            "invocation_id",
            "approval_json",
            "repository_root",
        ):
            for value in (1, True, " padded "):
                with self.subTest(field=field, value=value):
                    environment = live_environment_fixture()
                    environment[field] = value
                    self.assertTrue(validate_preflight_environment(environment))

    def test_duration_fields_reject_bool_strings_and_non_finite_values(self):
        for field in ("timeout_sec", "poll_sec"):
            for value in (True, "1", float("inf"), float("nan"), 0, -1.0):
                with self.subTest(field=field, value=value):
                    environment = live_environment_fixture()
                    environment[field] = value
                    self.assertTrue(validate_preflight_environment(environment))

    def test_ports_require_exact_integers(self):
        for field in ("fabric_port", "gradio_range_start", "gradio_range_end"):
            for value in (True, "4316", 4316.9, 0, 65536):
                with self.subTest(field=field, value=value):
                    environment = live_environment_fixture()
                    environment[field] = value
                    self.assertTrue(validate_preflight_environment(environment))


if __name__ == "__main__":
    unittest.main()
