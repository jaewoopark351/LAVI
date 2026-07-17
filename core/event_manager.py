#20260717_kpopmodder: Compatibility facade for app-wide event classes and singleton.
from core.event_manager_core.event_manager import EventManager
from core.event_manager_core.event_subscription import EventSubscription
from core.event_manager_core.event_type import EventType


event_manager = EventManager()

__all__ = [
    "EventManager",
    "EventSubscription",
    "EventType",
    "event_manager",
]


# # Subscribe functions to the 'user_registered' event
# event_manager.subscribe('user_registered', on_user_registered)
# event_manager.subscribe('user_registered', send_welcome_email)

# # Trigger the event, which will call both subscribed functions
# event_manager.trigger('user_registered', username='JohnDoe')

# Output:
# User registered: JohnDoe
# Sending welcome email to JohnDoe
