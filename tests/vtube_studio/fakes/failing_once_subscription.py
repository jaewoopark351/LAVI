#20260904_kpopmodder: Fail one unsubscribe so retryable event cleanup can be verified.


class FailingOnceSubscription:
    def __init__(self):
        self.unsubscribe_calls = 0

    def unsubscribe(self):
        self.unsubscribe_calls += 1
        if self.unsubscribe_calls == 1:
            raise RuntimeError("controlled unsubscribe failure")
        return True
