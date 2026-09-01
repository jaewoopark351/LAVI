#20260901_kpopmodder: Prove direct-file CLI startup remains fail closed.
from __future__ import annotations

import json
import os
import subprocess
import sys
import unittest
from pathlib import Path


class AutomaticDepositDirectFileCliTests(unittest.TestCase):
    def test_direct_file_cli_reaches_inconclusive_json_without_submit(self):
        repository_root = Path(__file__).resolve().parents[4]
        script_path = repository_root / (
            "tests/minecraft_chatclef/runtime/automatic_deposit/"
            "run_automatic_deposit_matrix.py"
        )
        environment = dict(os.environ)
        environment.pop("LAVI_MINECRAFT_RUNTIME_TESTS", None)
        environment.pop("LAVI_MINECRAFT_RUNTIME_MUTATING", None)
        environment.pop("LAVI_MINECRAFT_AUTOMATIC_DEPOSIT_ROW_ID", None)

        completed = subprocess.run(
            [sys.executable, "-B", str(script_path)],
            cwd=repository_root,
            env=environment,
            capture_output=True,
            text=True,
            check=False,
            timeout=10,
        )

        self.assertEqual(2, completed.returncode, completed.stderr)
        self.assertEqual("", completed.stderr)
        result = json.loads(completed.stdout)
        self.assertEqual("INCONCLUSIVE", result["verdict"])
        self.assertEqual("LIVE_RUNTIME_OPT_IN_NOT_SELECTED", result["reason"])
        self.assertEqual(0, result["submit_call_count"])


if __name__ == "__main__":
    unittest.main()
