#20260801_kpopmodder: Define backend-neutral Minecraft bridge lifecycle states.
from __future__ import annotations

from enum import Enum


class BridgeLifecycleState(str, Enum):
    NOT_IMPLEMENTED = "not_implemented"
    DISABLED = "disabled"
    STOPPED = "stopped"
    STARTING = "starting"
    CONNECTING = "connecting"
    CONNECTED = "connected"
    DISCONNECTED = "disconnected"
    STOPPING = "stopping"
    FAILED = "failed"
