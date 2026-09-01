#20260901_kpopmodder: Keep hermetic Fabric JAR bytes independent of wall-clock time.
from __future__ import annotations

import hashlib
import io
import unittest
import zipfile
from pathlib import Path

from .evidence_test_fixture import HermeticEvidenceFileSystem, fabric_mod_jar


class HermeticEvidenceTestFixtureTests(unittest.TestCase):
    def test_fabric_mod_jar_uses_one_deterministic_zip_timestamp(self):
        first = fabric_mod_jar("carryon", "2.1.2.7")
        second = fabric_mod_jar("carryon", "2.1.2.7")

        self.assertEqual(first, second)
        with zipfile.ZipFile(io.BytesIO(first), "r") as archive:
            entries = archive.infolist()
        self.assertEqual(1, len(entries))
        self.assertEqual((1980, 1, 1, 0, 0, 0), entries[0].date_time)

    def test_file_inode_is_derived_deterministically_from_the_canonical_path(self):
        instance = Path("C:/minecraft/instance")
        mods = instance.joinpath("mods")
        jar = mods.joinpath("carryon.jar")
        files = HermeticEvidenceFileSystem(instance, mods)
        canonical = files.add_file(jar, b"jar", in_mods=True)
        key = str(canonical.resolve(strict=False)).casefold()
        expected_inode = (
            int.from_bytes(
                hashlib.sha256(key.encode("utf-8")).digest()[:8],
                "big",
            )
            % 100_000
            + 10
        )

        self.assertEqual(expected_inode, files.read_stat(jar).st_ino)


if __name__ == "__main__":
    unittest.main()
