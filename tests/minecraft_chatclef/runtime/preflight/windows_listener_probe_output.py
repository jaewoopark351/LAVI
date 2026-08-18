#20260818_kpopmodder: Decode Windows listener probe streams without replacement characters.
from __future__ import annotations


def decode_windows_listener_probe_output(
    value: object,
    *,
    stream_name: str,
) -> str:
    if value is None:
        return ""
    if isinstance(value, str):
        return value
    if not isinstance(value, bytes):
        raise UnicodeError(
            f"listener probe {stream_name} decode failed: unsupported output type"
        )
    for encoding in ("utf-8-sig", "cp949"):
        try:
            return value.decode(encoding, errors="strict")
        except UnicodeDecodeError:
            continue
    raise UnicodeError(
        f"listener probe {stream_name} decode failed for UTF-8 and CP949"
    )
