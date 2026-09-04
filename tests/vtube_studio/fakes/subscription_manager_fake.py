#20260904_kpopmodder: Return one controlled event subscription from the runtime test boundary.
from .failing_once_subscription import FailingOnceSubscription


class SubscriptionManagerFake:
    def __init__(self, subscription=None):
        self.subscription = subscription or FailingOnceSubscription()
        self.subscribe_calls = 0

    def subscribe(self, event_type, callback):
        self.subscribe_calls += 1
        return self.subscription
