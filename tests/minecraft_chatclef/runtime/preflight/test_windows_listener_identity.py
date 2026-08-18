#20260818_kpopmodder: Lock intended single-LAVI listener ownership validation.
from __future__ import annotations

import unittest

from .preflight_fixture_factory import listener_payload_fixture
from .windows_listener_identity import validate_listener_probe_payload


class WindowsListenerIdentityFixtureTests(unittest.TestCase):
    def test_same_approved_lavi_pid_passes(self):
        result = validate_listener_probe_payload(
            listener_payload_fixture(),
            gradio_port=47860,
            fabric_port=4316,
            gradio_range_start=47860,
            gradio_range_end=47959,
            repository_root="C:/Vtuber_Souorce_Code/LAVI",
            gradio_host="127.0.0.1",
            fabric_host="127.0.0.1",
        )
        self.assertTrue(result["ok"], result)
        self.assertEqual(4100, result["observed"]["intended_lavi_pid"])

    def test_second_lavi_candidate_fails(self):
        payload = listener_payload_fixture()
        payload["listeners"].append(
            {
                "local_address": "127.0.0.1",
                "local_port": 47862,
                "process_id": 4200,
            }
        )
        payload["processes"].append(
            {
                "process_id": 4200,
                "parent_process_id": 0,
                "name": "python.exe",
                "creation_date": "20260818120100.000000+540",
                "executable_path": "C:/Vtuber_Souorce_Code/LAVI/venv/Scripts/python.exe",
                "command_line": "python C:/Vtuber_Souorce_Code/LAVI/main.py",
            }
        )
        result = validate_listener_probe_payload(
            payload,
            gradio_port=47860,
            fabric_port=4316,
            gradio_range_start=47860,
            gradio_range_end=47959,
            repository_root="C:/Vtuber_Souorce_Code/LAVI",
            gradio_host="127.0.0.1",
            fabric_host="127.0.0.1",
        )
        self.assertFalse(result["ok"])
        self.assertIn("second LAVI", result["reason"])

    def test_wildcard_gradio_listener_fails(self):
        payload = listener_payload_fixture()
        payload["listeners"][0]["local_address"] = "0.0.0.0"

        result = _validate(payload)

        self.assertFalse(result["ok"])
        self.assertIn("loopback", result["reason"])

    def test_ipv6_wildcard_listener_fails(self):
        payload = listener_payload_fixture()
        payload["listeners"][0]["local_address"] = "::"

        result = _validate(payload)

        self.assertFalse(result["ok"])
        self.assertIn("loopback", result["reason"])

    def test_non_loopback_listener_fails(self):
        payload = listener_payload_fixture()
        payload["listeners"][0]["local_address"] = "192.168.0.10"

        result = _validate(payload)

        self.assertFalse(result["ok"])
        self.assertIn("loopback", result["reason"])

    def test_url_listener_address_family_mismatch_fails(self):
        payload = listener_payload_fixture()
        payload["listeners"][0]["local_address"] = "::1"

        result = _validate(payload)

        self.assertFalse(result["ok"])
        self.assertIn("address", result["reason"])

    def test_repository_prefix_collision_fails(self):
        for suffix in ("-copy", "-old"):
            with self.subTest(suffix=suffix):
                payload = listener_payload_fixture()
                process = payload["processes"][0]
                root = f"C:/Vtuber_Souorce_Code/LAVI{suffix}"
                process["executable_path"] = f"{root}/venv/Scripts/python.exe"
                process["command_line"] = f'python "{root}/main.py"'

                result = _validate(payload)

                self.assertFalse(result["ok"])
                self.assertIn("entrypoint", result["reason"])

    def test_quoted_repository_path_with_spaces_passes_case_insensitively(self):
        payload = listener_payload_fixture()
        process = payload["processes"][0]
        process["executable_path"] = (
            "C:/Vtuber Souorce Code/LAVI/venv/Scripts/Python.exe"
        )
        process["command_line"] = (
            '"C:/Vtuber Souorce Code/LAVI/venv/Scripts/Python.exe" '
            '"c:/vtuber souorce code/lavi/main.py"'
        )

        result = _validate(
            payload,
            repository_root="C:/Vtuber Souorce Code/LAVI",
        )

        self.assertTrue(result["ok"], result)


def _validate(payload, *, repository_root="C:/Vtuber_Souorce_Code/LAVI"):
    return validate_listener_probe_payload(
        payload,
        gradio_port=47860,
        fabric_port=4316,
        gradio_range_start=47860,
        gradio_range_end=47959,
        repository_root=repository_root,
        gradio_host="127.0.0.1",
        fabric_host="127.0.0.1",
    )


if __name__ == "__main__":
    unittest.main()
