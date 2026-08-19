#20260819_kpopmodder: Lock authoritative Windows argv parsing for the strict listener identity gate.
from __future__ import annotations

import os
import unittest
from unittest.mock import patch

from .windows_listener.process_identity import windows_command_line
from .windows_listener.process_identity.windows_command_line import (
    parse_windows_command_line,
)


class WindowsCommandLineTests(unittest.TestCase):
    def test_simple_and_quoted_exact_paths_parse(self):
        expected = (
            "python.exe",
            "C:\\Vtuber_Souorce_Code\\LAVI\\main.py",
        )

        self.assertEqual(
            expected,
            parse_windows_command_line(
                "python.exe C:\\Vtuber_Souorce_Code\\LAVI\\main.py"
            ),
        )
        self.assertEqual(
            expected,
            parse_windows_command_line(
                'python.exe "C:\\Vtuber_Souorce_Code\\LAVI\\main.py"'
            ),
        )

    def test_native_quote_backslash_semantics_do_not_invent_exact_main(self):
        command_line = (
            'python.exe "C:\\\\"""Vtuber_Souorce_Code"'
            '\\LAVI\\\\""main.py"'
        )

        parsed = parse_windows_command_line(command_line)

        self.assertNotEqual(
            (
                "python.exe",
                "C:\\Vtuber_Souorce_Code\\LAVI\\main.py",
            ),
            parsed,
        )

    def test_unterminated_quote_fails_closed(self):
        self.assertEqual(
            (),
            parse_windows_command_line(
                'python.exe "C:\\Vtuber_Souorce_Code\\LAVI\\main.py'
            ),
        )

    @unittest.skipUnless(os.name == "nt", "native parser is Windows-only")
    def test_native_parser_unavailability_fails_closed(self):
        with patch.object(
            windows_command_line.ctypes,
            "WinDLL",
            side_effect=OSError("native parser unavailable"),
        ):
            self.assertEqual(
                (),
                parse_windows_command_line(
                    "python.exe C:\\Vtuber_Souorce_Code\\LAVI\\main.py"
                ),
            )


if __name__ == "__main__":
    unittest.main()
