#20260831_kpopmodder: Lock exclusive evidence creation without filesystem writes.
from __future__ import annotations

import unittest
from pathlib import Path

from .exclusive_evidence_directory import (
    AutomaticDepositExclusiveEvidenceDirectory,
)


class AutomaticDepositExclusiveEvidenceDirectoryTests(unittest.TestCase):
    def test_existing_run_directory_fails_without_fallback(self):
        created: list[str] = []
        directory = AutomaticDepositExclusiveEvidenceDirectory(
            _repository_root(),
            exists=lambda _path: True,
            ensure_parent=lambda _path: None,
            create_exclusive=lambda path: created.append(str(path)),
        )

        result = directory.create("run-001")

        self.assertFalse(result.ok)
        self.assertEqual("EVIDENCE_DIRECTORY_ALREADY_EXISTS", result.reason)
        self.assertEqual([], created)

    def test_new_run_directory_uses_exact_canonical_target_once(self):
        created: list[str] = []
        directory = AutomaticDepositExclusiveEvidenceDirectory(
            _repository_root(),
            exists=lambda _path: False,
            ensure_parent=lambda _path: None,
            create_exclusive=lambda path: created.append(str(path)),
        )

        result = directory.create("run-001")

        self.assertTrue(result.ok)
        self.assertEqual(1, len(created))
        self.assertEqual(result.path, created[0])

    def test_different_absolute_root_is_rejected_without_write(self):
        created: list[str] = []
        directory = AutomaticDepositExclusiveEvidenceDirectory(
            "C:/different/repository",
            exists=lambda _path: False,
            ensure_parent=lambda _path: None,
            create_exclusive=lambda path: created.append(str(path)),
        )

        result = directory.create("run-001")

        self.assertFalse(result.ok)
        self.assertEqual("EVIDENCE_REPOSITORY_ROOT_NOT_ACTIVE", result.reason)
        self.assertEqual([], created)


def _repository_root():
    return str(Path(__file__).resolve().parents[5])


if __name__ == "__main__":
    unittest.main()
