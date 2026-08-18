#20260818_kpopmodder: Lock intended single-LAVI listener ownership validation.
#20260819_kpopmodder: Reject false entrypoint matches and bind approved provenance.
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

    def test_missing_required_listener_fails_closed(self):
        for missing_port in (47860, 4316):
            with self.subTest(missing_port=missing_port):
                payload = listener_payload_fixture()
                payload["listeners"] = [
                    listener
                    for listener in payload["listeners"]
                    if listener["local_port"] != missing_port
                ]

                result = _validate(payload)

                self.assertFalse(result["ok"], result)
                self.assertIn("one listener owner", result["reason"])

    def test_gradio_and_fabric_different_owner_pids_fail_closed(self):
        payload = listener_payload_fixture()
        payload["listeners"][1]["process_id"] = 4200
        payload["processes"].append(
            {
                **payload["processes"][0],
                "process_id": 4200,
                "creation_date": "20260818120100.000000+540",
            }
        )

        result = _validate(payload)

        self.assertFalse(result["ok"], result)
        self.assertIn("different owners", result["reason"])

    def test_duplicate_process_evidence_fails_closed(self):
        payload = listener_payload_fixture()
        payload["processes"].append(dict(payload["processes"][0]))

        result = _validate(payload)

        self.assertFalse(result["ok"], result)
        self.assertIn("duplicate process", result["reason"])

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

    def test_exact_absolute_repository_main_is_structurally_bound(self):
        result = _validate(listener_payload_fixture())

        self.assertTrue(result["ok"], result)
        observed = result["observed"]
        self.assertEqual("python_script", observed["process_invocation_mode"])
        self.assertEqual(
            "c:\\vtuber_souorce_code\\lavi\\main.py",
            observed["resolved_entrypoint_path"],
        )
        self.assertEqual(
            "exact_repository_script",
            observed["entrypoint_provenance"],
        )
        self.assertEqual(
            "c:\\vtuber_souorce_code\\lavi",
            observed["repository_root"],
        )
        self.assertIsNone(observed["approved_ancestor"])
        self.assertTrue(observed["process_identity_fingerprint"])

    def test_relative_main_without_cwd_or_launcher_proof_fails(self):
        payload = listener_payload_fixture()
        payload["processes"][0]["command_line"] = (
            "C:/Vtuber_Souorce_Code/LAVI/venv/Scripts/python.exe main.py"
        )

        result = _validate(payload)

        self.assertFalse(result["ok"], result)
        self.assertIn("entrypoint", result["reason"])

    def test_python_code_mode_with_trailing_main_argument_fails(self):
        payload = listener_payload_fixture()
        payload["processes"][0]["command_line"] = (
            'python.exe -c "print(1)" main.py'
        )

        result = _validate(payload)

        self.assertFalse(result["ok"], result)
        self.assertIn("entrypoint", result["reason"])

    def test_descendant_main_file_is_not_the_repository_entrypoint(self):
        payload = listener_payload_fixture()
        payload["processes"][0]["command_line"] = (
            "python.exe C:/Vtuber_Souorce_Code/LAVI/tmp/main.py"
        )

        result = _validate(payload)

        self.assertFalse(result["ok"], result)
        self.assertIn("entrypoint", result["reason"])

    def test_module_mode_without_resolved_module_provenance_fails(self):
        for command_line in ("python.exe -m lavi", "python.exe -m lavi app"):
            with self.subTest(command_line=command_line):
                payload = listener_payload_fixture()
                payload["processes"][0]["command_line"] = command_line

                result = _validate(payload)

                self.assertFalse(result["ok"], result)
                self.assertIn("entrypoint", result["reason"])

    def test_relative_main_with_exact_launcher_ancestor_passes(self):
        payload = listener_payload_fixture()
        owner = payload["processes"][0]
        owner["parent_process_id"] = 4000
        owner["command_line"] = (
            "C:/Vtuber_Souorce_Code/LAVI/venv/Scripts/python.exe main.py"
        )
        payload["processes"].append(_approved_launcher_process())

        result = _validate(payload)

        self.assertTrue(result["ok"], result)
        observed = result["observed"]
        self.assertEqual(
            "approved_launcher_relative_script",
            observed["entrypoint_provenance"],
        )
        self.assertEqual(4000, observed["approved_ancestor"]["process_id"])
        self.assertEqual(
            "20260818115900.000000+540",
            observed["approved_ancestor"]["creation_date"],
        )
        self.assertEqual(
            "c:\\windows\\system32\\cmd.exe",
            observed["approved_ancestor"]["executable_path"],
        )
        self.assertEqual(
            "cmd_launcher",
            observed["approved_ancestor"]["invocation_mode"],
        )
        self.assertEqual(
            "c:\\vtuber_souorce_code\\lavi\\run_lav_dev.cmd",
            observed["approved_ancestor"]["resolved_entrypoint_path"],
        )

    def test_relative_main_with_incomplete_launcher_identity_fails(self):
        payload = listener_payload_fixture()
        owner = payload["processes"][0]
        owner["parent_process_id"] = 4000
        owner["command_line"] = "python.exe main.py"
        launcher = _approved_launcher_process()
        launcher["creation_date"] = ""
        payload["processes"].append(launcher)

        result = _validate(payload)

        self.assertFalse(result["ok"], result)
        self.assertIn("entrypoint", result["reason"])

    def test_approved_launcher_does_not_approve_an_arbitrary_child_script(self):
        payload = listener_payload_fixture()
        owner = payload["processes"][0]
        owner["parent_process_id"] = 4000
        owner["command_line"] = (
            "python.exe C:/Vtuber_Souorce_Code/LAVI/scripts/preflight.py"
        )
        payload["processes"].append(_approved_launcher_process())

        result = _validate(payload)

        self.assertFalse(result["ok"], result)
        self.assertIn("entrypoint", result["reason"])

    def test_required_listener_owner_process_evidence_fails_closed(self):
        for field in ("executable_path", "creation_date", "command_line"):
            with self.subTest(field=field):
                payload = listener_payload_fixture()
                payload["processes"][0][field] = ""

                result = _validate(payload)

                self.assertFalse(result["ok"], result)
                self.assertIn("entrypoint", result["reason"])

    def test_launcher_token_as_unrelated_argument_does_not_approve_relative_main(self):
        payload = listener_payload_fixture()
        owner = payload["processes"][0]
        owner["parent_process_id"] = 4000
        owner["command_line"] = "python.exe main.py"
        launcher = _approved_launcher_process()
        launcher["command_line"] = (
            'C:/Windows/System32/cmd.exe /c "echo not-lavi" '
            '"C:/Vtuber_Souorce_Code/LAVI/run_lav_dev.cmd"'
        )
        payload["processes"].append(launcher)

        result = _validate(payload)

        self.assertFalse(result["ok"], result)
        self.assertIn("entrypoint", result["reason"])


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


def _approved_launcher_process() -> dict[str, object]:
    return {
        "process_id": 4000,
        "parent_process_id": 0,
        "name": "cmd.exe",
        "creation_date": "20260818115900.000000+540",
        "executable_path": "C:/Windows/System32/cmd.exe",
        "command_line": (
            '"C:/Windows/System32/cmd.exe" /c '
            '"C:/Vtuber_Souorce_Code/LAVI/run_lav_dev.cmd"'
        ),
    }


if __name__ == "__main__":
    unittest.main()
