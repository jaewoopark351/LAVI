#20260901_kpopmodder: Verify bounded exact-chunk Anvil reads reject unsafe evidence.
from __future__ import annotations

import os
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace

from ._fixture_builders import (
    SECTOR_BYTES,
    chunk_nbt,
    container_block_entity,
    write_region_file,
)
from .anvil_chunk_reader import read_stable_anvil_chunk


class AnvilChunkReaderTests(unittest.TestCase):
    def test_exact_chunk_is_read_from_stable_region(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "r.0.-1.mca"
            payload = chunk_nbt(
                chunk_x=0,
                chunk_z=-1,
                block_entities=(container_block_entity(),),
            )
            write_region_file(
                path,
                chunk_x=0,
                chunk_z=-1,
                nbt_payload=payload,
            )

            chunk, reason = read_stable_anvil_chunk(
                path.resolve(), block_position=(12, 64, -9)
            )

            self.assertEqual("ANVIL_CHUNK_OBSERVED", reason)
            self.assertIsNotNone(chunk)
            assert chunk is not None
            self.assertEqual((0, -1), (chunk.chunk_x, chunk.chunk_z))
            self.assertEqual(2, chunk.compression_type)
            self.assertEqual(1_777_777_777, chunk.chunk_timestamp)
            self.assertGreater(chunk.region_mtime_ns, 0)
            self.assertEqual(64, chunk.nbt["block_entities"][0]["y"])

    def test_wrong_region_chunk_and_unsafe_compression_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "r.0.-1.mca"
            wrong_payload = chunk_nbt(
                chunk_x=1,
                chunk_z=-1,
                block_entities=(container_block_entity(),),
            )
            write_region_file(
                path,
                chunk_x=0,
                chunk_z=-1,
                nbt_payload=wrong_payload,
            )
            chunk, reason = read_stable_anvil_chunk(
                path.resolve(), block_position=(12, 64, -9)
            )
            self.assertIsNone(chunk)
            self.assertEqual("ANVIL_CHUNK_COORDINATE_MISMATCH", reason)

            valid_payload = chunk_nbt(
                chunk_x=0,
                chunk_z=-1,
                block_entities=(container_block_entity(),),
            )
            for compression, expected in (
                (4, "ANVIL_CHUNK_COMPRESSION_UNSUPPORTED"),
                (130, "ANVIL_EXTERNAL_CHUNK_UNSUPPORTED"),
            ):
                with self.subTest(compression=compression):
                    write_region_file(
                        path,
                        chunk_x=0,
                        chunk_z=-1,
                        nbt_payload=valid_payload,
                        compression_type=compression,
                    )
                    chunk, reason = read_stable_anvil_chunk(
                        path.resolve(), block_position=(12, 64, -9)
                    )
                    self.assertIsNone(chunk)
                    self.assertEqual(expected, reason)

    def test_trailing_bytes_after_nbt_document_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "r.0.-1.mca"
            payload = chunk_nbt(
                chunk_x=0,
                chunk_z=-1,
                block_entities=(container_block_entity(),),
            )
            write_region_file(
                path,
                chunk_x=0,
                chunk_z=-1,
                nbt_payload=payload + b"trailing",
                compression_type=3,
            )

            chunk, reason = read_stable_anvil_chunk(
                path.resolve(), block_position=(12, 64, -9)
            )

            self.assertIsNone(chunk)
            self.assertEqual("ANVIL_CHUNK_NBT_INVALID", reason)

    def test_out_of_bounds_location_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "r.0.-1.mca"
            path.write_bytes(bytes(SECTOR_BYTES * 2))
            payload = bytearray(path.read_bytes())
            index = (0 & 31) + ((-1 & 31) * 32)
            payload[index * 4 : index * 4 + 4] = (
                (9).to_bytes(3, "big") + b"\x01"
            )
            path.write_bytes(payload)

            chunk, reason = read_stable_anvil_chunk(
                path.resolve(), block_position=(12, 64, -9)
            )

            self.assertIsNone(chunk)
            self.assertEqual("ANVIL_CHUNK_LOCATION_OUT_OF_BOUNDS", reason)

    def test_stale_or_changed_region_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "r.0.-1.mca"
            write_region_file(
                path,
                chunk_x=0,
                chunk_z=-1,
                nbt_payload=chunk_nbt(
                    chunk_x=0,
                    chunk_z=-1,
                    block_entities=(container_block_entity(),),
                ),
            )
            current = path.stat()
            chunk, reason = read_stable_anvil_chunk(
                path.resolve(),
                block_position=(12, 64, -9),
                minimum_mtime_ns=current.st_mtime_ns + 1,
            )
            self.assertIsNone(chunk)
            self.assertEqual("ANVIL_REGION_STALE", reason)

            reads = iter(
                (
                    _stat_like(current, mtime_ns=current.st_mtime_ns),
                    _stat_like(current, mtime_ns=current.st_mtime_ns + 1),
                )
            )
            chunk, reason = read_stable_anvil_chunk(
                path.resolve(),
                block_position=(12, 64, -9),
                stat_reader=lambda _path: next(reads),
            )
            self.assertIsNone(chunk)
            self.assertEqual("ANVIL_STABLE_FILE_CHANGED_DURING_READ", reason)


def _stat_like(value: os.stat_result, *, mtime_ns: int) -> object:
    return SimpleNamespace(
        st_dev=value.st_dev,
        st_ino=value.st_ino,
        st_mode=value.st_mode,
        st_size=value.st_size,
        st_mtime_ns=mtime_ns,
    )
