#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

import threading


class LlmInputQueueThreadStarter:
    def __init__(self, worker) -> None:
        self._worker = worker

    def start_once(self, target) -> None:
        current_thread = self._worker.input_process_thread
        if current_thread is not None and current_thread.is_alive():
            return
        input_thread = threading.Thread(target=target)
        input_thread.daemon = True
        self._worker.input_process_thread = input_thread
        input_thread.start()


__all__ = ("LlmInputQueueThreadStarter",)
