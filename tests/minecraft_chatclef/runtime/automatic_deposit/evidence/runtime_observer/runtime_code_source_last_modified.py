# 20260901_kpopmodder: Parse the exact UTC FileTime representation emitted by Java.
from __future__ import annotations

import re
from datetime import datetime


_JAVA_FILE_TIME = re.compile(
    r"(?P<date>[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2})"
    r"(?:\.(?P<fraction>[0-9]{1,9}))?Z\Z",
    re.ASCII,
)
_EPOCH = datetime(1970, 1, 1)


def parse_java_file_time_epoch_ns(value: object) -> int | None:
    if not isinstance(value, str):
        return None
    match = _JAVA_FILE_TIME.fullmatch(value)
    if match is None:
        return None
    try:
        timestamp = datetime.strptime(match.group("date"), "%Y-%m-%dT%H:%M:%S")
    except ValueError:
        return None
    delta = timestamp - _EPOCH
    seconds = delta.days * 86_400 + delta.seconds
    fraction = match.group("fraction") or ""
    nanoseconds = int(fraction.ljust(9, "0")) if fraction else 0
    return seconds * 1_000_000_000 + nanoseconds
