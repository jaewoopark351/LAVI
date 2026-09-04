#20260904_kpopmodder: Model mouth-worker ownership separately from generic avatar controllers.
from .controller_fake import ControllerFake


class MouthControllerFake(ControllerFake):
    def __init__(self):
        super().__init__()
        self.worker = self

    def close_mouth_force(self):
        return None
