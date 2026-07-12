#20260621_kpopmodder: Queue worker keeps structured LLM inputs so ScreenVision events can skip chat history.
from queue import Queue, Empty
import threading

from llm_core.input_queue_runtime import LLMInputQueueRuntime


class LLMInputQueueWorker:#20260621_kpopmodder
    def __init__(
        self,
        response_callback,
        history_callback,
        system_prompt_callback,
        queue_updated_callback,
    ):
        self.response_callback = response_callback
        self.history_callback = history_callback
        self.system_prompt_callback = system_prompt_callback
        self.queue_updated_callback = queue_updated_callback
        self.input_queue = Queue()
        self.input_process_thread = None
        self.input_queue_lock = threading.Lock()#20260617_kpopmodder
        self.queue_runtime = LLMInputQueueRuntime(self)#20260706_kpopmodder

    def receive_input(self, text):
        with self.input_queue_lock:
            self.input_queue.put(text)
            self.queue_updated_callback()
        self.process_input_queue()

    def process_input_queue(self):
        self.queue_runtime.start_once(self.generate_response)

    def generate_response(self):
        self.queue_runtime.drain_queue()

    def clear_pending_inputs(self):#20260621_kpopmodder
        #20260621_kpopmodder: ScreenVision 최신 화면/사용자 인터럽트 우선 처리를 위해 대기 중인 LLM 입력만 제거한다.
        with self.input_queue_lock:
            while True:
                try:
                    self.input_queue.get_nowait()
                except Empty:
                    break
                except Exception:
                    break
            self.queue_updated_callback()

# #from queue import Queue#20260621_kpopmodder
# from queue import Queue, Empty#20260621_kpopmodder
# import threading


# class LLMInputQueueWorker:#20260621_kpopmodder
#     def __init__(
#         self,
#         response_callback,
#         history_callback,
#         system_prompt_callback,
#         queue_updated_callback
#     ):
#         self.response_callback = response_callback
#         self.history_callback = history_callback
#         self.system_prompt_callback = system_prompt_callback
#         self.queue_updated_callback = queue_updated_callback

#         self.input_queue = Queue()
#         self.input_process_thread = None
#         self.input_queue_lock = threading.Lock()#20260617_kpopmodder

#     def receive_input(self, text):
#         with self.input_queue_lock:
#             self.input_queue.put(text)

#         self.queue_updated_callback()
#         self.process_input_queue()

#     def process_input_queue(self):
#         if (
#             self.input_process_thread is not None
#             and self.input_process_thread.is_alive()
#         ):
#             return

#         self.input_process_thread = threading.Thread(
#             target=self.generate_response,
#         )
#         self.input_process_thread.daemon = True
#         self.input_process_thread.start()

#     def generate_response(self):
#         while True:
#             with self.input_queue_lock:
#                 if self.input_queue.empty():
#                     break

#                 next_input = self.input_queue.get()

#             response_generator = self.response_callback(
#                 next_input,
#                 self.history_callback(),
#                 self.system_prompt_callback(),
#             )

#             for _ in response_generator:
#                 pass

#             self.queue_updated_callback()

#     def clear_pending_inputs(self):#20260621_kpopmodder
#         #20260621_kpopmodder: ScreenVision 최신 화면 우선 처리를 위해 대기 중인 LLM 입력만 제거한다.
#         with self.input_queue_lock:
#             while True:
#                 try:
#                     self.input_queue.get_nowait()
#                 except Empty:
#                     break
#                 except Exception:
#                     break

#         self.queue_updated_callback()
