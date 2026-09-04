#20260904_kpopmodder: Model the low-level websocket-client API used by the production session adapter.


class FakeLowLevelWebSocket:
    def __init__(self, messages):
        self.messages = list(messages)
        self.timeouts = []
        self.sent_payloads = []
        self.shutdown_calls = 0

    def settimeout(self, timeout):
        self.timeouts.append(timeout)

    def recv(self):
        return self.messages.pop(0)

    def send(self, payload):
        self.sent_payloads.append(payload)
        return len(payload)

    def shutdown(self):
        self.shutdown_calls += 1
