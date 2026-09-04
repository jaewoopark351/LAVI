#20260904_kpopmodder: Define the response phase owned by one VTube Studio authentication attempt.
from enum import Enum


class VTubeStudioAuthenticationPhase(Enum):
    IDLE = "IDLE"
    TOKEN_REQUESTED = "TOKEN_REQUESTED"
    TOKEN_RECEIVED = "TOKEN_RECEIVED"
    AUTHENTICATION_REQUESTED = "AUTHENTICATION_REQUESTED"
    AUTHENTICATED = "AUTHENTICATED"
    REJECTED = "REJECTED"
