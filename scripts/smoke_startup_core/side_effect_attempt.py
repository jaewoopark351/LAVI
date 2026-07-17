#20260717_kpopmodder: Split smoke startup helper from legacy multi-class script for AGENTS 29.1.

from .smoke_error import SmokeError

class SideEffectAttempt(SmokeError):
    pass
