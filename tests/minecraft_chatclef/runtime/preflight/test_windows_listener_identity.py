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
                "creation_time_utc_ticks": 638911008600000000,
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
                "creation_time_utc_ticks": 638911008600000000,
                "executable_path": "C:/Vtuber_Souorce_Code/LAVI/venv/Scripts/python.exe",
                "command_line": "python.exe C:/Vtuber_Souorce_Code/LAVI/main.py",
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

    def test_pythonw_entrypoint_fails_even_with_exact_main(self):
        payload = listener_payload_fixture()
        process = payload["processes"][0]
        process["name"] = "pythonw.exe"
        process["executable_path"] = (
            "C:/Vtuber_Souorce_Code/LAVI/venv/Scripts/pythonw.exe"
        )
        process["command_line"] = (
            "pythonw.exe C:/Vtuber_Souorce_Code/LAVI/main.py"
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

    def test_absolute_main_with_exact_launcher_ancestor_is_bound(self):
        payload = listener_payload_fixture()
        owner = payload["processes"][0]
        owner["parent_process_id"] = 4000
        owner["command_line"] = (
            "C:/Vtuber_Souorce_Code/LAVI/venv/Scripts/python.exe "
            "C:/Vtuber_Souorce_Code/LAVI/main.py"
        )
        payload["processes"].append(_approved_launcher_process())

        result = _validate(payload)

        self.assertTrue(result["ok"], result)
        observed = result["observed"]
        self.assertEqual(
            "exact_repository_script",
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

    def test_relative_main_with_exact_launcher_ancestor_still_fails(self):
        payload = listener_payload_fixture()
        owner = payload["processes"][0]
        owner["parent_process_id"] = 4000
        owner["command_line"] = "python.exe main.py"
        payload["processes"].append(_approved_launcher_process())

        result = _validate(payload)

        self.assertFalse(result["ok"], result)
        self.assertIn("entrypoint", result["reason"])

    def test_relative_main_with_incomplete_launcher_identity_fails(self):
        payload = listener_payload_fixture()
        owner = payload["processes"][0]
        owner["parent_process_id"] = 4000
        owner["command_line"] = (
            "python.exe C:/Vtuber_Souorce_Code/LAVI/main.py"
        )
        launcher = _approved_launcher_process()
        launcher["creation_date"] = ""
        payload["processes"].append(launcher)

        result = _validate(payload)

        self.assertFalse(result["ok"], result)
        self.assertIn("entrypoint", result["reason"])

    def test_absolute_main_with_later_launcher_start_fails(self):
        payload = listener_payload_fixture()
        owner = payload["processes"][0]
        owner["parent_process_id"] = 4000
        owner["command_line"] = (
            "python.exe C:/Vtuber_Souorce_Code/LAVI/main.py"
        )
        launcher = _approved_launcher_process()
        launcher["creation_date"] = "20260818120100.000000+540"
        launcher["creation_time_utc_ticks"] = 638911008600000000
        payload["processes"].append(launcher)

        result = _validate(payload)

        self.assertFalse(result["ok"], result)
        self.assertIn("entrypoint", result["reason"])

    def test_absolute_main_with_rogue_cmd_path_fails(self):
        payload = listener_payload_fixture()
        owner = payload["processes"][0]
        owner["parent_process_id"] = 4000
        owner["command_line"] = (
            "python.exe C:/Vtuber_Souorce_Code/LAVI/main.py"
        )
        launcher = _approved_launcher_process()
        launcher["executable_path"] = "C:/Temp/cmd.exe"
        launcher["command_line"] = (
            '"C:/Temp/cmd.exe" /c '
            '"C:/Vtuber_Souorce_Code/LAVI/run_lav_dev.cmd"'
        )
        payload["processes"].append(launcher)

        result = _validate(payload)

        self.assertFalse(result["ok"], result)
        self.assertIn("entrypoint", result["reason"])

    def test_absolute_main_with_ordinary_cmd_parent_passes_without_claim(self):
        payload = listener_payload_fixture()
        payload["processes"][0]["parent_process_id"] = 4000
        parent = _approved_launcher_process()
        parent["command_line"] = "C:/Windows/System32/cmd.exe"
        payload["processes"].append(parent)

        result = _validate(payload)

        self.assertTrue(result["ok"], result)
        self.assertIsNone(result["observed"]["approved_ancestor"])

    def test_approved_launcher_rejects_trailing_or_compound_commands(self):
        for command_line in (
            "C:/Windows/System32/cmd.exe /c "
            "C:/Vtuber_Souorce_Code/LAVI/run.bat evil",
            "C:/Windows/System32/cmd.exe /c "
            '"C:/Vtuber_Souorce_Code/LAVI/run.bat" & evil',
        ):
            with self.subTest(command_line=command_line):
                payload = listener_payload_fixture()
                payload["processes"][0]["parent_process_id"] = 4000
                launcher = _approved_launcher_process()
                launcher["command_line"] = command_line
                payload["processes"].append(launcher)

                result = _validate(payload)

                self.assertFalse(result["ok"], result)
                self.assertIn("entrypoint", result["reason"])

    def test_absolute_main_with_missing_parent_evidence_fails(self):
        payload = listener_payload_fixture()
        payload["processes"][0]["parent_process_id"] = 4000

        result = _validate(payload)

        self.assertFalse(result["ok"], result)
        self.assertIn("entrypoint", result["reason"])

    def test_python_identity_fields_and_argv0_must_agree(self):
        cases = (
            (
                "name_pythonw",
                "pythonw.exe",
                "C:/Vtuber_Souorce_Code/LAVI/venv/Scripts/python.exe",
                "python.exe C:/Vtuber_Souorce_Code/LAVI/main.py",
            ),
            (
                "argv0_pythonw",
                "python.exe",
                "C:/Vtuber_Souorce_Code/LAVI/venv/Scripts/python.exe",
                "pythonw.exe C:/Vtuber_Souorce_Code/LAVI/main.py",
            ),
            (
                "argv0_other_absolute_python",
                "python.exe",
                "C:/Vtuber_Souorce_Code/LAVI/venv/Scripts/python.exe",
                "C:/Temp/python.exe C:/Vtuber_Souorce_Code/LAVI/main.py",
            ),
            (
                "argv0_relative_python_path",
                "python.exe",
                "C:/Vtuber_Souorce_Code/LAVI/venv/Scripts/python.exe",
                "venv/Scripts/python.exe C:/Vtuber_Souorce_Code/LAVI/main.py",
            ),
        )
        for label, name, executable_path, command_line in cases:
            with self.subTest(label=label):
                payload = listener_payload_fixture()
                owner = payload["processes"][0]
                owner["name"] = name
                owner["executable_path"] = executable_path
                owner["command_line"] = command_line

                result = _validate(payload)

                self.assertFalse(result["ok"], result)
                self.assertIn("entrypoint", result["reason"])

    def test_python_direct_form_rejects_options_and_trailing_arguments(self):
        entrypoint = "C:/Vtuber_Souorce_Code/LAVI/main.py"
        for command_line in (
            f"python.exe -O {entrypoint}",
            f"python.exe -- {entrypoint}",
            f"python.exe {entrypoint} extra",
            f"python.exe {entrypoint} main.py",
        ):
            with self.subTest(command_line=command_line):
                payload = listener_payload_fixture()
                payload["processes"][0]["command_line"] = command_line

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
        for field in (
            "executable_path",
            "creation_date",
            "creation_time_utc_ticks",
            "command_line",
        ):
            with self.subTest(field=field):
                payload = listener_payload_fixture()
                payload["processes"][0][field] = (
                    0 if field == "creation_time_utc_ticks" else ""
                )

                result = _validate(payload)

                self.assertFalse(result["ok"], result)
                self.assertIn("entrypoint", result["reason"])

    def test_process_start_ticks_require_a_positive_exact_integer(self):
        for invalid_value in (None, True, "638911008000000000", 1.0, 0, -1):
            with self.subTest(invalid_value=invalid_value):
                payload = listener_payload_fixture()
                payload["processes"][0]["creation_time_utc_ticks"] = invalid_value

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

    def test_unapproved_module_lavi_on_fallback_port_fails(self):
        for command_line in (
            "python.exe -m lavi",
            "python.exe -m lavi.app",
            "python.exe -mlavi",
        ):
            with self.subTest(command_line=command_line):
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
                        "creation_time_utc_ticks": 638911008600000000,
                        "executable_path": "C:/Python311/python.exe",
                        "command_line": command_line,
                    }
                )

                result = _validate(payload)

                self.assertFalse(result["ok"], result)
                self.assertIn("second LAVI", result["reason"])

    def test_pythonw_main_on_fallback_port_fails_as_hidden_candidate(self):
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
                "name": "pythonw.exe",
                "creation_date": "20260818120100.000000+540",
                "creation_time_utc_ticks": 638911008600000000,
                "executable_path": "C:/Python311/pythonw.exe",
                "command_line": (
                    "pythonw.exe C:/Vtuber_Souorce_Code/LAVI/main.py"
                ),
            }
        )

        result = _validate(payload)

        self.assertFalse(result["ok"], result)
        self.assertIn("second LAVI", result["reason"])

    def test_unrelated_python_script_on_fallback_port_is_not_lavi(self):
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
                "creation_time_utc_ticks": 638911008600000000,
                "executable_path": "C:/Python311/python.exe",
                "command_line": "python.exe C:/Other/http_server.py",
            }
        )

        result = _validate(payload)

        self.assertTrue(result["ok"], result)

    def test_malformed_fallback_python_is_ambiguous_and_fails(self):
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
                "creation_time_utc_ticks": 638911008600000000,
                "executable_path": "C:/Python311/python.exe",
                "command_line": "python.exe --unsupported C:/Other/server.py",
            }
        )

        result = _validate(payload)

        self.assertFalse(result["ok"], result)
        self.assertIn("ambiguous", result["reason"])

    def test_malformed_main_candidate_on_fallback_port_fails(self):
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
                "creation_time_utc_ticks": 638911008600000000,
                "executable_path": "C:/Python311/python.exe",
                "command_line": "python.exe --unsupported main.py",
            }
        )

        result = _validate(payload)

        self.assertFalse(result["ok"], result)
        self.assertIn("second LAVI", result["reason"])

    def test_fallback_listener_with_missing_process_evidence_fails(self):
        payload = listener_payload_fixture()
        payload["listeners"].append(
            {
                "local_address": "127.0.0.1",
                "local_port": 47862,
                "process_id": 4200,
            }
        )

        result = _validate(payload)

        self.assertFalse(result["ok"], result)
        self.assertIn("ambiguous", result["reason"])

    def test_incomplete_fallback_process_identity_fails_ambiguous(self):
        base_process = {
            "process_id": 4200,
            "parent_process_id": 0,
            "name": "python.exe",
            "creation_date": "20260818120100.000000+540",
            "creation_time_utc_ticks": 638911008600000000,
            "executable_path": "C:/Python311/python.exe",
            "command_line": "python.exe C:/Other/server.py",
        }
        for field in (
            "parent_process_id",
            "name",
            "creation_date",
            "creation_time_utc_ticks",
            "executable_path",
            "command_line",
        ):
            with self.subTest(field=field):
                payload = listener_payload_fixture()
                payload["listeners"].append(
                    {
                        "local_address": "127.0.0.1",
                        "local_port": 47862,
                        "process_id": 4200,
                    }
                )
                process = dict(base_process)
                process[field] = None
                payload["processes"].append(process)

                result = _validate(payload)

                self.assertFalse(result["ok"], result)
                self.assertIn("ambiguous", result["reason"])


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
        "creation_time_utc_ticks": 638911007400000000,
        "executable_path": "C:/Windows/System32/cmd.exe",
        "command_line": (
            '"C:/Windows/System32/cmd.exe" /c '
            '"C:/Vtuber_Souorce_Code/LAVI/run_lav_dev.cmd"'
        ),
    }


if __name__ == "__main__":
    unittest.main()
