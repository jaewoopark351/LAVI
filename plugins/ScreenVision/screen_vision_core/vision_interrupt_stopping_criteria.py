#20260717_kpopmodder: Isolates ScreenVision generation interrupt stopping criterion.
from transformers import StoppingCriteria


class VisionInterruptStoppingCriteria(StoppingCriteria):#20260620_kpopmodder
    def __init__(self, stop_event):
        self.stop_event = stop_event

    def __call__(self, input_ids, scores, **kwargs):
        return self.stop_event.is_set()
