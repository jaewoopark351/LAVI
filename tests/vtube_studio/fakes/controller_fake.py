#20260904_kpopmodder: Model one restartable avatar controller for runtime lifecycle tests.
from .thread_state_fake import ThreadStateFake


class ControllerFake:
    def __init__(self, fail_start_count=0):
        self.thread = ThreadStateFake()
        self.fail_start_count = fail_start_count
        self.start_calls = 0
        self.stop_calls = 0

    def start_if_needed(self):
        self.start_calls += 1
        if self.start_calls <= self.fail_start_count:
            raise RuntimeError("controlled controller startup failure")
        self.thread.alive = True
        return True

    def stop(self):
        self.stop_calls += 1
        self.thread.alive = False
        return True
