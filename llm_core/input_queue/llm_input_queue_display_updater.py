#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

import LAV_utils


class LlmInputQueueDisplayUpdater:
    def __init__(self, *, live_textbox, queue_callback) -> None:
        if not callable(getattr(live_textbox, "set", None)):
            raise TypeError("live_textbox.set must be callable")
        if not callable(queue_callback):
            raise TypeError("queue_callback must be callable")
        self._live_textbox = live_textbox
        self._queue_callback = queue_callback

    def update(self) -> None:
        self._live_textbox.set(
            LAV_utils.queue_to_list(self._queue_callback())
        )


__all__ = ("LlmInputQueueDisplayUpdater",)
