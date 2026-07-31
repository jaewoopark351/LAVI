#20260801_kpopmodder: Define backend-neutral Minecraft bridge message wire values.
from __future__ import annotations

from enum import Enum


class BridgeMessageType(str, Enum):
    HANDSHAKE = "handshake"
    HANDSHAKE_ACK = "handshake_ack"
    COMMAND_REQUEST = "command_request"
    COMMAND_RESULT = "command_result"
    STATUS_REQUEST = "status_request"
    STATUS_SNAPSHOT = "status_snapshot"
    EVENT = "event"
    ERROR = "error"
