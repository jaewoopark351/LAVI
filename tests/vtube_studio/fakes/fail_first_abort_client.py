#20260904_kpopmodder: Model one failed low-level WebSocket abort followed by successful cleanup.
from .close_only_client import CloseOnlyClient


class FailFirstAbortClient(CloseOnlyClient):
    def __init__(self, event_log):
        super().__init__(event_log)
        self.shutdown_calls = 0

    def shutdown(self):
        self.shutdown_calls += 1
        if self.shutdown_calls == 1:
            raise OSError("controlled abort failure")
        self.event_log.append("client_shutdown")
