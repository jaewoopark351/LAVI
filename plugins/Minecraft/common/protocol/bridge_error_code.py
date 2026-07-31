#20260801_kpopmodder: Define generic Minecraft bridge error codes without backend placeholders.
from __future__ import annotations

from enum import Enum


class BridgeErrorCode(str, Enum):
    NOT_IMPLEMENTED = "not_implemented"
    NOT_CONNECTED = "not_connected"
    BRIDGE_DISABLED = "bridge_disabled"
    INVALID_REQUEST = "invalid_request"
    INVALID_MESSAGE_TYPE = "invalid_message_type"
    UNSUPPORTED_PROTOCOL_VERSION = "unsupported_protocol_version"
    DEADLINE_EXCEEDED = "deadline_exceeded"
    INTERNAL_ERROR = "internal_error"
