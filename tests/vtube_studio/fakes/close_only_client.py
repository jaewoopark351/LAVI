#20260904_kpopmodder: Model a close-only low-level WebSocket client for session lifecycle tests.


class CloseOnlyClient:
    def __init__(self, event_log):
        self.event_log = event_log
        self.close_calls = 0
        self.timeouts = []

    def settimeout(self, timeout):
        self.timeouts.append(timeout)

    def close(self):
        self.close_calls += 1
        self.event_log.append("client_closed")
