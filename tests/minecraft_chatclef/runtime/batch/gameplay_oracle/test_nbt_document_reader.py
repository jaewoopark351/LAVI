#20260818_kpopmodder: Verify the dependency-free playerdata NBT decoder.
from __future__ import annotations

import gzip
import struct
import unittest

from .nbt_document_reader import decode_nbt_document


class NbtDocumentReaderTests(unittest.TestCase):
    def test_gzip_player_inventory_compounds_are_decoded(self):
        document = decode_nbt_document(
            gzip.compress(
                _player_inventory_nbt(
                    (("minecraft:coal", 5), ("minecraft:coal", 1))
                )
            )
        )

        self.assertEqual(
            [
                {"id": "minecraft:coal", "Count": 5},
                {"id": "minecraft:coal", "Count": 1},
            ],
            document["Inventory"],
        )

    def test_truncated_payload_is_rejected(self):
        with self.assertRaises(ValueError):
            decode_nbt_document(b"\x0a\x00")


def _player_inventory_nbt(items: tuple[tuple[str, int], ...]) -> bytes:
    payload = bytearray(b"\x0a\x00\x00")
    payload.extend(b"\x09" + _nbt_string("Inventory"))
    payload.extend(b"\x0a" + struct.pack(">i", len(items)))
    for item_id, count in items:
        payload.extend(b"\x08" + _nbt_string("id") + _nbt_string(item_id))
        payload.extend(b"\x01" + _nbt_string("Count") + struct.pack(">b", count))
        payload.extend(b"\x00")
    payload.extend(b"\x00")
    return bytes(payload)


def _nbt_string(value: str) -> bytes:
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded


if __name__ == "__main__":
    unittest.main()
