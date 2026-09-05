#20260905_kpopmodder: Create only process-local Fabric ChatClef runtime identifiers and timestamps.
from __future__ import annotations

import time
import uuid


def new_fabric_chatclef_message_id() -> str:
    return f"lavi-{uuid.uuid4().hex}"


def fabric_chatclef_now_ms() -> int:
    return int(time.time() * 1000)
