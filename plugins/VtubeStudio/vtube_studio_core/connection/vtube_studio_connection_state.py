#20260904_kpopmodder: Define transport-only VTube Studio connection states in one type file.
from enum import Enum


class VTubeStudioConnectionState(str, Enum):
    NOT_STARTED = "NOT_STARTED"
    CONNECTING = "CONNECTING"
    DISCONNECTED_WAIT = "DISCONNECTED_WAIT"
    CONNECTED = "CONNECTED"
    AUTHENTICATING = "AUTHENTICATING"
    AUTHENTICATED = "AUTHENTICATED"
    STOPPING = "STOPPING"
    STOPPED = "STOPPED"
