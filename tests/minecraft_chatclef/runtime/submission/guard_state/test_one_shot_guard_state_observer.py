#20260901_kpopmodder: Specify fail-closed, read-only one-shot guard observation.
from __future__ import annotations

import unittest
from pathlib import Path

from .one_shot_guard_state import OneShotGuardState
from .one_shot_guard_state_observer import OneShotGuardStateObserver


_INVOCATION_FINGERPRINT = "a" * 64
_COMMAND_FINGERPRINT = "b" * 64


class OneShotGuardStateObserverTests(unittest.TestCase):
    def setUp(self) -> None:
        self.repository_root = Path(__file__).resolve().parents[5]
        self.state_directory = (
            self.repository_root / "tests" / "tmp_guard_state_observer_fixture"
        )

    def test_no_records_is_clear_without_reading_a_record(self):
        reads: list[str] = []
        observer = self._observer(
            {},
            record_reader=lambda path: reads.append(path.name),
        )

        observation = observer.observe()

        self.assertIs(OneShotGuardState.CLEAR, observation.state)
        self.assertEqual([], reads)
        self.assertIsNone(observation.invocation_fingerprint)
        self.assertIsNone(observation.command_fingerprint)

    def test_live_run_block_is_reported_with_its_fingerprints(self):
        observation = self._observer(
            {"live-run-block.json": _block_record()}
        ).observe()

        self.assertIs(
            OneShotGuardState.LIVE_RUN_BLOCK_PRESENT,
            observation.state,
        )
        self.assertEqual(
            _INVOCATION_FINGERPRINT,
            observation.invocation_fingerprint,
        )
        self.assertEqual(_COMMAND_FINGERPRINT, observation.command_fingerprint)

    def test_reconciliation_marker_takes_precedence_when_records_match(self):
        reads: list[str] = []
        records = {
            "live-run-block.json": _block_record(),
            "reconciliation-required.json": _reconciliation_record(),
        }
        observer = self._observer(
            records,
            record_reader=lambda path: reads.append(path.name) or records[path.name],
        )

        observation = observer.observe()

        self.assertIs(
            OneShotGuardState.RECONCILIATION_REQUIRED,
            observation.state,
        )
        self.assertEqual(
            "gameplay_observation_incomplete",
            observation.reason,
        )
        self.assertEqual(
            ["live-run-block.json", "reconciliation-required.json"],
            reads,
        )

    def test_reconciliation_marker_without_a_block_still_takes_precedence(self):
        observation = self._observer(
            {"reconciliation-required.json": _reconciliation_record()}
        ).observe()

        self.assertIs(
            OneShotGuardState.RECONCILIATION_REQUIRED,
            observation.state,
        )

    def test_inconsistent_record_identities_are_unreadable(self):
        marker = {
            **_reconciliation_record(),
            "command_fingerprint": "c" * 64,
        }

        observation = self._observer(
            {
                "live-run-block.json": _block_record(),
                "reconciliation-required.json": marker,
            }
        ).observe()

        self.assertIs(
            OneShotGuardState.GUARD_STATE_UNREADABLE,
            observation.state,
        )
        self.assertIn("identities do not match", observation.reason)
        self.assertIsNone(observation.invocation_fingerprint)
        self.assertIsNone(observation.command_fingerprint)

    def test_malformed_present_record_is_unreadable(self):
        observation = self._observer(
            {"live-run-block.json": {"invocation_fingerprint": "not-a-hash"}}
        ).observe()

        self.assertIs(
            OneShotGuardState.GUARD_STATE_UNREADABLE,
            observation.state,
        )
        self.assertIn("live-run-block.json", observation.reason)

    def test_record_reader_failure_is_unreadable(self):
        def fail_read(_path: Path) -> object:
            raise OSError("fixture is locked")

        observation = self._observer(
            {"live-run-block.json": _block_record()},
            record_reader=fail_read,
        ).observe()

        self.assertIs(
            OneShotGuardState.GUARD_STATE_UNREADABLE,
            observation.state,
        )
        self.assertIn("OSError", observation.reason)
        self.assertNotIn("fixture is locked", observation.reason)

    def test_non_boolean_exists_result_is_unreadable(self):
        observer = OneShotGuardStateObserver(
            str(self.repository_root),
            state_directory=self.state_directory,
            record_exists=lambda _path: "yes",
            record_reader=lambda _path: _block_record(),
        )

        observation = observer.observe()

        self.assertIs(
            OneShotGuardState.GUARD_STATE_UNREADABLE,
            observation.state,
        )

    def test_state_directory_outside_repository_is_rejected(self):
        with self.assertRaises(ValueError):
            OneShotGuardStateObserver(
                str(self.repository_root),
                state_directory=self.repository_root.parent,
            )

    def _observer(
        self,
        records: dict[str, object],
        *,
        record_reader=None,
    ) -> OneShotGuardStateObserver:
        return OneShotGuardStateObserver(
            str(self.repository_root),
            state_directory=self.state_directory,
            record_exists=lambda path: path.name in records,
            record_reader=record_reader or (lambda path: records[path.name]),
        )


def _block_record() -> dict[str, object]:
    return {
        "invocation_fingerprint": _INVOCATION_FINGERPRINT,
        "command_fingerprint": _COMMAND_FINGERPRINT,
        "claimed_at_utc": "2026-09-01T03:16:22.464432+00:00",
    }


def _reconciliation_record() -> dict[str, object]:
    return {
        "invocation_fingerprint": _INVOCATION_FINGERPRINT,
        "command_fingerprint": _COMMAND_FINGERPRINT,
        "reason": "gameplay_observation_incomplete",
        "recorded_at_utc": "2026-09-01T03:16:39.959340+00:00",
    }


if __name__ == "__main__":
    unittest.main()
