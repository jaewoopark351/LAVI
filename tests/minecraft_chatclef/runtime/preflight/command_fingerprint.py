#20260818_kpopmodder: Produce a stable command fingerprint without retaining command text.
from __future__ import annotations

import hashlib


def command_fingerprint(command: object) -> str:
    return hashlib.sha256(str(command or "").strip().encode("utf-8")).hexdigest()
