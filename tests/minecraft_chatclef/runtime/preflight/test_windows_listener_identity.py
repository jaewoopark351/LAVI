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
        )
        self.assertTrue(result["ok"], result)
        self.assertEqual(4100, result["observed"]["intended_lavi_pid"])

    def test_second_lavi_candidate_fails(self):
        payload = listener_payload_fixture()
        payload["listeners"].append({"local_port": 47862, "process_id": 4200})
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
        )
        self.assertFalse(result["ok"])
        self.assertIn("second LAVI", result["reason"])


if __name__ == "__main__":
    unittest.main()
