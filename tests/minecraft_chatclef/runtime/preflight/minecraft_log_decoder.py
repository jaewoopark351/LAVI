#20260818_kpopmodder: Decode Minecraft log bytes with strict UTF-8 then strict CP949.
from __future__ import annotations


def decode_log_bytes(raw: bytes) -> dict[str, object]:
    for encoding in ("utf-8", "cp949"):
        try:
            return {
                "ok": True,
                "reason": "decoded",
                "text": raw.decode(encoding, errors="strict"),
                "encoding": encoding,
            }
        except UnicodeDecodeError:
            continue
    return {
        "ok": False,
        "reason": "latest.log is neither strict UTF-8 nor strict CP949",
        "text": "",
        "encoding": "",
    }
