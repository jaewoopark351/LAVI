#20260818_kpopmodder: Decode test-world NBT without adding a runtime dependency.
from __future__ import annotations

import gzip
import struct
from collections.abc import Mapping


_TAG_END = 0
_TAG_COMPOUND = 10
_MAX_COLLECTION_LENGTH = 1_000_000
_MAX_DEPTH = 128
_MAX_STRING_BYTES = 1_048_576


class NbtDocumentReader:
    def __init__(self, payload: bytes):
        self._payload = memoryview(payload)
        self._offset = 0

    def read_document(self) -> dict[str, object]:
        tag_type = self._read_unsigned_byte()
        if tag_type != _TAG_COMPOUND:
            raise ValueError("NBT root tag must be a compound")
        self._read_string()
        root = self._read_payload(tag_type, depth=0)
        if not isinstance(root, Mapping):
            raise ValueError("NBT root payload is not a compound")
        if self._offset != len(self._payload):
            raise ValueError("NBT document contains trailing bytes")
        return dict(root)

    def _read_payload(self, tag_type: int, *, depth: int) -> object:
        if depth > _MAX_DEPTH:
            raise ValueError("NBT nesting exceeds the safety limit")
        if tag_type == 1:
            return self._unpack(">b")
        if tag_type == 2:
            return self._unpack(">h")
        if tag_type == 3:
            return self._unpack(">i")
        if tag_type == 4:
            return self._unpack(">q")
        if tag_type == 5:
            return self._unpack(">f")
        if tag_type == 6:
            return self._unpack(">d")
        if tag_type == 7:
            return self._read_bytes(self._read_length("byte array"))
        if tag_type == 8:
            return self._read_string()
        if tag_type == 9:
            return self._read_list(depth=depth + 1)
        if tag_type == _TAG_COMPOUND:
            return self._read_compound(depth=depth + 1)
        if tag_type == 11:
            return [self._unpack(">i") for _ in range(self._read_length("int array"))]
        if tag_type == 12:
            return [self._unpack(">q") for _ in range(self._read_length("long array"))]
        raise ValueError(f"unsupported NBT tag type: {tag_type}")

    def _read_list(self, *, depth: int) -> list[object]:
        element_type = self._read_unsigned_byte()
        length = self._read_length("list")
        if element_type == _TAG_END and length:
            raise ValueError("non-empty NBT list cannot use TAG_End elements")
        return [
            self._read_payload(element_type, depth=depth)
            for _ in range(length)
        ]

    def _read_compound(self, *, depth: int) -> dict[str, object]:
        compound: dict[str, object] = {}
        while True:
            tag_type = self._read_unsigned_byte()
            if tag_type == _TAG_END:
                return compound
            name = self._read_string()
            compound[name] = self._read_payload(tag_type, depth=depth)

    def _read_string(self) -> str:
        length = self._unpack(">H")
        if length > _MAX_STRING_BYTES:
            raise ValueError("NBT string exceeds the safety limit")
        return self._read_bytes(length).decode("utf-8", errors="strict")

    def _read_length(self, label: str) -> int:
        length = self._unpack(">i")
        if length < 0 or length > _MAX_COLLECTION_LENGTH:
            raise ValueError(f"NBT {label} length is invalid: {length}")
        return length

    def _read_unsigned_byte(self) -> int:
        return int(self._unpack(">B"))

    def _unpack(self, format_string: str) -> object:
        size = struct.calcsize(format_string)
        raw = self._read_bytes(size)
        return struct.unpack(format_string, raw)[0]

    def _read_bytes(self, length: int) -> bytes:
        end = self._offset + length
        if length < 0 or end > len(self._payload):
            raise ValueError("NBT payload ended unexpectedly")
        value = self._payload[self._offset:end].tobytes()
        self._offset = end
        return value


def decode_nbt_document(raw_payload: bytes) -> dict[str, object]:
    payload = (
        gzip.decompress(raw_payload)
        if raw_payload.startswith(b"\x1f\x8b")
        else raw_payload
    )
    return NbtDocumentReader(payload).read_document()
