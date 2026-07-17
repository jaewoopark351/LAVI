#20260717_kpopmodder: Isolates app-wide event enum from subscription and manager classes.
from enum import Enum, auto


class EventType(Enum):
    """
    Enum for all the allowed event types in the system.
    """

    INTERRUPT = auto()  # Triggered when you want interrupt the entire pipeline.
    SCREEN_OBSERVATION = auto()  # ScreenVision observation payload has been emitted.
