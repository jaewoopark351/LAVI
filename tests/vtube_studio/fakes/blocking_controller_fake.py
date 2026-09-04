#20260904_kpopmodder: Hold one controller start operation open for shutdown-race tests.
import threading

from .thread_state_fake import ThreadStateFake


class BlockingControllerFake:
    def __init__(self):
        self.thread = ThreadStateFake()
        self.start_entered = threading.Event()
        self.release_start = threading.Event()
        self.start_calls = 0
        self.stop_calls = 0

    def start_if_needed(self):
        self.start_calls += 1
        self.start_entered.set()
        self.release_start.wait()
        self.thread.alive = True
        return True

    def stop(self):
        self.stop_calls += 1
        self.thread.alive = False
        return True
