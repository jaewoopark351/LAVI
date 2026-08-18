#20260818_kpopmodder: Produce a stable invocation fingerprint without retaining its text.
from __future__ import annotations

import hashlib


def invocation_fingerprint(invocation_id: object) -> str:
    return hashlib.sha256(str(invocation_id or "").strip().encode("utf-8")).hexdigest()
