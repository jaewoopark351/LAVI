#20260717_kpopmodder: Compatibility facade for StarCraft2 GameExtension classes and constants.
from app_core.extensions.starcraft2_core.starcraft2_constants import (
    STARCRAFT2_LOG_EVENT_ORIGIN,
    STARCRAFT2_STATUS_EVENT_CALLBACK_RESOURCE,
    STARCRAFT2_TERMINAL_EVENT_OBSERVER_RESOURCE,
)
from app_core.extensions.starcraft2_core.starcraft2_game_extension import (
    StarCraft2GameExtension,
)
from app_core.extensions.starcraft2_core.starcraft2_status_event_subscription import (
    _StarCraft2StatusEventSubscription,
)

__all__ = [
    "STARCRAFT2_LOG_EVENT_ORIGIN",
    "STARCRAFT2_STATUS_EVENT_CALLBACK_RESOURCE",
    "STARCRAFT2_TERMINAL_EVENT_OBSERVER_RESOURCE",
    "StarCraft2GameExtension",
    "_StarCraft2StatusEventSubscription",
]
