#20260706_kpopmodder: Added this helper to keep LLM queue thread/runtime mechanics outside the facade worker.
import threading
from core.logger import log_print


class LLMInputQueueRuntime:
    #20260706_kpopmodder: Small runtime helper for process_input_queue/generate_response delegation.
    def __init__(self, worker):
        self.worker = worker

    def start_once(self, target):
        if (
            self.worker.input_process_thread is not None
            and self.worker.input_process_thread.is_alive()
        ):
            return

        self.worker.input_process_thread = threading.Thread(
            target=target,
        )
        self.worker.input_process_thread.daemon = True
        self.worker.input_process_thread.start()

    def drain_queue(self):
        while True:
            try:
                with self.worker.input_queue_lock:
                    if self.worker.input_queue.empty():
                        break
                    next_input = self.worker.input_queue.get()
            except Exception as e:
                log_print(
                    "[LLMInputQueueRuntime] failed to fetch next item from input queue: "
                    f"{e}"
                )
                continue

            try:
                response_generator = self.worker.response_callback(
                    next_input,
                    self.worker.history_callback(),
                    self.worker.system_prompt_callback(),
                )
                for _ in response_generator:
                    pass
            except Exception as e:
                log_print(
                    "[LLMInputQueueRuntime] response callback failed while draining: "
                    f"{e}"
                )
            finally:
                try:
                    self.worker.queue_updated_callback()
                except Exception as e:
                    log_print(
                        "[LLMInputQueueRuntime] queue update callback failed: "
                        f"{e}"
                    )
