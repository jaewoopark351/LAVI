#20260818_kpopmodder: Lock strict UTF-8 then CP949 latest.log decoding.
from __future__ import annotations

import unittest

from .minecraft_log_decoder import decode_log_bytes


class MinecraftLogDecoderTests(unittest.TestCase):
    def test_strict_cp949_is_supported_without_replacement_decode(self):
        decoded = decode_log_bytes("새로운 세계2".encode("cp949"))
        self.assertTrue(decoded["ok"])
        self.assertEqual("cp949", decoded["encoding"])

    def test_bytes_invalid_in_both_encodings_fail_closed(self):
        decoded = decode_log_bytes(b"\x80\x81\x82")
        self.assertFalse(decoded["ok"])
        self.assertIn("neither strict UTF-8 nor strict CP949", decoded["reason"])


if __name__ == "__main__":
    unittest.main()
