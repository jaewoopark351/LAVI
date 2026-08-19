#20260818_kpopmodder: Verify Windows listener probe output is decoded strictly without losing access-denied evidence.
from __future__ import annotations

import subprocess
import unittest

from .windows_listener_probe import read_windows_listener_probe
from .windows_listener_probe_script import build_windows_listener_probe_script


class WindowsListenerProbeTests(unittest.TestCase):
    def test_probe_emits_strict_utc_process_start_ticks(self):
        script = build_windows_listener_probe_script([4316, 47860])

        self.assertIn("CreationDate.ToUniversalTime().Ticks", script)
        self.assertIn("creation_time_utc_ticks=$creationTimeUtcTicks", script)
        self.assertIn("process creation time unavailable", script)

    def test_cp949_error_output_is_preserved_as_a_fail_closed_reason(self):
        def command_runner(*_args, **kwargs):
            self.assertFalse(kwargs["text"])
            self.assertNotIn("encoding", kwargs)
            return subprocess.CompletedProcess(
                args=[],
                returncode=1,
                stdout=b"",
                stderr="액세스가 거부되었습니다".encode("cp949"),
            )

        result = read_windows_listener_probe(
            [4316, 47860],
            command_runner=command_runner,
            platform_name="nt",
        )

        self.assertFalse(result["ok"])
        self.assertIn("액세스가 거부되었습니다", result["reason"])

    def test_utf8_json_output_is_decoded_and_parsed(self):
        def command_runner(*_args, **kwargs):
            self.assertFalse(kwargs["text"])
            return subprocess.CompletedProcess(
                args=[],
                returncode=0,
                stdout=b'{"listeners":[],"processes":[]}',
                stderr=b"",
            )

        result = read_windows_listener_probe(
            [4316, 47860],
            command_runner=command_runner,
            platform_name="nt",
        )

        self.assertTrue(result["ok"], result)
        self.assertEqual([], result["payload"]["listeners"])

    def test_output_that_is_neither_utf8_nor_cp949_fails_without_replacement(self):
        def command_runner(*_args, **_kwargs):
            return subprocess.CompletedProcess(
                args=[],
                returncode=1,
                stdout=b"",
                stderr=b"\x81\x00\x81\x00",
            )

        result = read_windows_listener_probe(
            [4316],
            command_runner=command_runner,
            platform_name="nt",
        )

        self.assertFalse(result["ok"])
        self.assertIn("decode failed", result["reason"])
        self.assertNotIn("�", result["reason"])


if __name__ == "__main__":
    unittest.main()
