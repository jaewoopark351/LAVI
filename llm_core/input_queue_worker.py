#20260621_kpopmodder: Queue worker keeps structured LLM inputs so ScreenVision events can skip chat history.
#20260905_kpopmodder: Preserve the LLM input queue worker compatibility facade.

from llm_core.input_queue.llm_input_queue_worker_component_graph import (
    LlmInputQueueWorkerComponentGraph,
)


class LLMInputQueueWorker:#20260621_kpopmodder
    def __init__(
        self,
        response_callback,
        history_callback,
        system_prompt_callback,
        queue_updated_callback,
        queued_response_callback=None,
    ):
        self.response_callback = response_callback
        self.history_callback = history_callback
        self.system_prompt_callback = system_prompt_callback
        self.queue_updated_callback = queue_updated_callback
        self.queued_response_callback = queued_response_callback
        self._components = LlmInputQueueWorkerComponentGraph(self)

    @property
    def input_queue(self):
        return self._components.queue_store.queue

    @input_queue.setter
    def input_queue(self, value) -> None:
        self._components.queue_store.queue = value

    @property
    def input_queue_lock(self):
        return self._components.queue_store.lock

    @input_queue_lock.setter
    def input_queue_lock(self, value) -> None:
        self._components.queue_store.lock = value

    @property
    def input_process_thread(self):
        return self._components.thread_state.thread

    @input_process_thread.setter
    def input_process_thread(self, value) -> None:
        self._components.thread_state.thread = value

    @property
    def queue_runtime(self):
        return self._components.queue_runtime

    @property
    def claim_aware_input_queue_sink(self):
        return self._components.claim_aware_input_queue_sink

    @property
    def _submission_coordinator(self):
        return self._components.submission_coordinator

    @property
    def _pending_input_clearer(self):
        return self._components.pending_input_clearer

    def receive_input(self, text):
        return self._submission_coordinator.submit(text)

    def process_input_queue(self):
        self.queue_runtime.start_once(self.generate_response)

    def generate_response(self):
        self.queue_runtime.drain_queue()

    def clear_pending_inputs(self):#20260621_kpopmodder
        #20260621_kpopmodder: ScreenVision 최신 화면/사용자 인터럽트 우선 처리를 위해 대기 중인 LLM 입력만 제거한다.
        return self._pending_input_clearer.clear()
