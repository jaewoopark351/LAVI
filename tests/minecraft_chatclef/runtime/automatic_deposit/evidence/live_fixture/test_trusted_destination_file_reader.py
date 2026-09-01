#20260901_kpopmodder: Verify exact stable trusted-registry evidence fails closed.
from __future__ import annotations

import hashlib
import os
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace

from ._fixture_builders import trusted_destination, write_trusted_destinations
from .trusted_destination_file_reader import read_exact_trusted_destination


class TrustedDestinationFileReaderTests(unittest.TestCase):
    def test_exact_enabled_destination_is_hashed_and_bound(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "automatic-deposit-trusted-destinations.json"
            write_trusted_destinations(
                path,
                [
                    trusted_destination(),
                    trusted_destination(
                        world_key="singleplayer:other",
                        x=90,
                        enabled=False,
                    ),
                ],
            )

            snapshot, reason = read_exact_trusted_destination(
                path.resolve(),
                expected_world_key="singleplayer:P1 fixture",
                expected_dimension="OVERWORLD",
            )

            self.assertEqual("TRUSTED_DESTINATION_OBSERVED", reason)
            self.assertIsNotNone(snapshot)
            assert snapshot is not None
            self.assertEqual((12, 64, -9), snapshot.position)
            canonical = "singleplayer:P1 fixture|OVERWORLD|12|64|-9"
            identity_sha = hashlib.sha256(canonical.encode("utf-8")).hexdigest()
            self.assertEqual(identity_sha, snapshot.destination_fingerprint)
            self.assertEqual(f"td_{identity_sha[:24]}", snapshot.destination_id)
            self.assertEqual(
                hashlib.sha256(path.read_bytes()).hexdigest(),
                snapshot.registry_sha256,
            )
            self.assertGreater(snapshot.registry_mtime_ns, 0)

    def test_duplicate_identity_is_rejected_before_selection(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "trusted.json"
            destination = trusted_destination()
            write_trusted_destinations(path, [destination, dict(destination)])

            snapshot, reason = read_exact_trusted_destination(
                path.resolve(),
                expected_world_key="singleplayer:P1 fixture",
                expected_dimension="OVERWORLD",
            )

            self.assertIsNone(snapshot)
            self.assertEqual("TRUSTED_DESTINATION_DUPLICATE_IDENTITY", reason)

    def test_malformed_or_ambiguous_registry_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "trusted.json"
            cases = (
                (b"not-json", "TRUSTED_DESTINATION_JSON_INVALID"),
                (
                    b'{"schemaVersion":2,"destinations":[]}',
                    "TRUSTED_DESTINATION_SCHEMA_INVALID",
                ),
                (
                    b'{"schemaVersion":true,"destinations":[]}',
                    "TRUSTED_DESTINATION_SCHEMA_INVALID",
                ),
                (
                    b'{"schemaVersion":1,"destinations":[{"worldKey":"singleplayer:P1 fixture","dimension":"OVERWORLD","x":12,"y":64,"z":-9,"enabled":"true"}]}',
                    "TRUSTED_DESTINATION_ENTRY_INVALID",
                ),
            )
            for payload, expected_reason in cases:
                with self.subTest(expected_reason=expected_reason):
                    path.write_bytes(payload)
                    snapshot, reason = read_exact_trusted_destination(
                        path.resolve(),
                        expected_world_key="singleplayer:P1 fixture",
                        expected_dimension="OVERWORLD",
                    )
                    self.assertIsNone(snapshot)
                    self.assertEqual(expected_reason, reason)

            write_trusted_destinations(
                path,
                [trusted_destination(), trusted_destination(x=13)],
            )
            snapshot, reason = read_exact_trusted_destination(
                path.resolve(),
                expected_world_key="singleplayer:P1 fixture",
                expected_dimension="OVERWORLD",
            )
            self.assertIsNone(snapshot)
            self.assertEqual("TRUSTED_DESTINATION_NOT_EXCLUSIVE", reason)

    def test_stale_or_changed_registry_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "trusted.json"
            write_trusted_destinations(path, [trusted_destination()])
            current = path.stat()

            snapshot, reason = read_exact_trusted_destination(
                path.resolve(),
                expected_world_key="singleplayer:P1 fixture",
                expected_dimension="OVERWORLD",
                minimum_mtime_ns=current.st_mtime_ns + 1,
            )
            self.assertIsNone(snapshot)
            self.assertEqual("TRUSTED_DESTINATION_REGISTRY_STALE", reason)

            reads = iter(
                (
                    _stat_like(current, mtime_ns=current.st_mtime_ns),
                    _stat_like(current, mtime_ns=current.st_mtime_ns + 1),
                )
            )
            snapshot, reason = read_exact_trusted_destination(
                path.resolve(),
                expected_world_key="singleplayer:P1 fixture",
                expected_dimension="OVERWORLD",
                stat_reader=lambda _path: next(reads),
            )
            self.assertIsNone(snapshot)
            self.assertEqual(
                "TRUSTED_DESTINATION_STABLE_FILE_CHANGED_DURING_READ", reason
            )


def _stat_like(value: os.stat_result, *, mtime_ns: int) -> object:
    return SimpleNamespace(
        st_dev=value.st_dev,
        st_ino=value.st_ino,
        st_mode=value.st_mode,
        st_size=value.st_size,
        st_mtime_ns=mtime_ns,
    )
