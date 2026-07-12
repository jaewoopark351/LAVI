#20260621_kpopmodder: TTS queue generation으로 인터럽트 전/후 문장 섞임을 막는다.
import threading
import time

from core.global_state import global_state, GlobalKeys
from core.logger import log_print, debug_print


class TTSQueueWorker:#20260621_kpopmodder
    def __init__(self, owner):
        self.owner = owner

    def start_if_needed(self, synthesize_function):
        tts = self.owner
        if (
            tts.audio_process_thread is not None
            and tts.audio_process_thread.is_alive()
        ):
            return

        generation = tts.get_queue_generation()
        tts.audio_process_thread = threading.Thread(
            target=self.worker_loop,
            args=(synthesize_function, generation),
        )
        tts.audio_process_thread.daemon = True
        tts.audio_process_thread.start()

    def worker_loop(self, synthesize_function, worker_generation):
        tts = self.owner
        self.start_speaking_state()
        try:
            while True:
                if self.is_stale_generation(worker_generation):
                    log_print("[TTS QUEUE] stale worker generation stopped")
                    break

                #20260622_kpopmodder: 이전 인터럽트가 남아 있으면 새 세대의 첫 문장을 큐에서 꺼내기 전에 worker를 종료한다.
                if tts.interrupt_event.is_set():
                    log_print("[TTS QUEUE] interrupt detected before dequeue")
                    break

                queued_item = self.get_next_input_item(worker_generation)
                if queued_item is None:
                    break
                input_text = queued_item["text"]
                response_generation = queued_item.get("response_generation")

                # dequeue 직후 새 인터럽트가 발생한 경우에는 현재 문장을 폐기한다.
                if tts.interrupt_event.is_set():
                    log_print("[TTS QUEUE] interrupt detected before synthesize")
                    break

                if self.is_stale_response_generation(response_generation):#20260623_kpopmodder
                    log_print(
                        "[TTS QUEUE] dropped stale response sentence before synthesize: "
                        f"response_generation={response_generation}, text={input_text}"
                    )
                    continue

                log_print(f"[TTS QUEUE] start sentence: {input_text}")
                audio_result = self.synthesize_with_retry(
                    synthesize_function,
                    input_text,
                    worker_generation,
                    response_generation=response_generation,
                    max_retries=3,
                )
                if audio_result is None:
                    log_print(
                        f"[TTS QUEUE] synthesize finally failed or stale: {input_text}"
                    )
                    continue

                if tts.interrupt_event.is_set() or self.is_stale_generation(worker_generation):
                    log_print("[TTS QUEUE] interrupt/stale detected after synthesize")
                    break
                if self.is_stale_response_generation(response_generation):#20260623_kpopmodder
                    log_print(
                        "[TTS QUEUE] stale response detected after synthesize: "
                        f"response_generation={response_generation}, text={input_text}"
                    )
                    continue

                tts.update_subtitle_file(input_text)
                playback_ok = self.playback_with_retry(
                    audio_result,
                    input_text,
                    worker_generation,
                    response_generation=response_generation,
                    max_retries=2,
                )

                if tts.interrupt_event.is_set() or self.is_stale_generation(worker_generation):
                    log_print("[TTS QUEUE] interrupt/stale detected during playback")
                    break
                if self.is_stale_response_generation(response_generation):#20260623_kpopmodder
                    log_print(
                        "[TTS QUEUE] stale response detected during playback: "
                        f"response_generation={response_generation}, text={input_text}"
                    )
                    break

                if not playback_ok:
                    log_print(
                        f"[TTS QUEUE] playback finally failed. force skip: {input_text}"
                    )
                    continue

                log_print(f"[TTS QUEUE] finished sentence: {input_text}")
                tts.process_queue_live_textbox.set(tts.get_queue_display_items())
        except Exception as e:
            log_print(f"[TTS process_input_queue error] {e}")
        finally:
            self.finish_speaking_state(worker_generation, synthesize_function)

    def is_stale_generation(self, worker_generation):
        return worker_generation != self.owner.get_queue_generation()

    def is_stale_response_generation(self, response_generation):#20260623_kpopmodder
        is_stale_response_generation = getattr(
            self.owner,
            "is_stale_response_generation",
            None,
        )
        if is_stale_response_generation is None:
            return False
        return is_stale_response_generation(response_generation)

    def start_speaking_state(self):
        global_state.set_value(GlobalKeys.IS_AI_SPEAKING, True)
        debug_print("[TTS QUEUE] IS_AI_SPEAKING=True")

    def finish_speaking_state(self, worker_generation, synthesize_function):
        tts = self.owner

        # 현재 worker가 끝났음을 먼저 표시한다.
        try:
            if threading.current_thread() == tts.audio_process_thread:
                tts.audio_process_thread = None
        except Exception:
            pass

        # stale worker가 아닌 경우에만 interrupt flag를 정리한다.
        if worker_generation == tts.get_queue_generation():
            tts.interrupt_event.clear()

        global_state.set_value(GlobalKeys.IS_AI_SPEAKING, False)
        global_state.set_value(
            GlobalKeys.LAST_AI_SPEAK_END_TIME,
            time.time(),
        )
        tts.send_output(0)
        debug_print("[TTS QUEUE] IS_AI_SPEAKING=False")

        #20260621_kpopmodder: 인터럽트 직후 새 사용자 발화가 큐에 들어왔는데 이전 worker가 살아 있어서
        # start_if_needed가 무시했던 경우를 복구한다.
        try:
            if not tts.input_queue.empty() and not tts.interrupt_event.is_set():
                self.start_if_needed(synthesize_function)
        except Exception as e:
            log_print(f"[TTS QUEUE] restart check failed: {e}")

    def get_next_input_item(self, worker_generation):
        tts = self.owner
        while True:
            with tts.queue_lock:
                if tts.input_queue.empty():
                    return None
                item = tts.input_queue.get()

            item_generation, response_generation, input_text = (
                tts.parse_queue_item(
                    item,
                    default_queue_generation=worker_generation,
                )
            )

            if item_generation != worker_generation:
                log_print(
                    "[TTS QUEUE] dropped stale queued sentence: "
                    f"item_generation={item_generation}, worker_generation={worker_generation}, text={input_text}"
                )
                continue

            if self.is_stale_response_generation(response_generation):#20260623_kpopmodder
                log_print(
                    "[TTS QUEUE] dropped stale response sentence: "
                    f"response_generation={response_generation}, text={input_text}"
                )
                continue

            input_text = str(input_text).strip()
            if not input_text:
                continue
            return {
                "text": input_text,
                "response_generation": response_generation,
            }

    def synthesize_with_retry(
        self,
        synthesize_function,
        input_text,
        worker_generation,
        response_generation=None,
        max_retries=3,
    ):
        tts = self.owner
        audio_result = None
        for retry_count in range(max_retries):
            if (
                tts.interrupt_event.is_set()
                or self.is_stale_generation(worker_generation)
                or self.is_stale_response_generation(response_generation)
            ):
                return None
            try:
                with tts.synth_lock:
                    if (
                        tts.interrupt_event.is_set()
                        or self.is_stale_generation(worker_generation)
                        or self.is_stale_response_generation(response_generation)
                    ):
                        return None
                    audio_result = synthesize_function(input_text)
                if audio_result is not None:
                    return audio_result
                log_print(
                    f"[TTS QUEUE] synthesize returned None. "
                    f"retry={retry_count + 1}/{max_retries} text={input_text}"
                )
            except Exception as e:
                log_print(
                    f"[TTS QUEUE] synthesize failed. "
                    f"retry={retry_count + 1}/{max_retries} "
                    f"text={input_text}, error={e}"
                )
            time.sleep(1.0)
        return None

    def playback_with_retry(
        self,
        audio_result,
        input_text,
        worker_generation,
        response_generation=None,
        max_retries=2,
    ):
        tts = self.owner
        for retry_count in range(max_retries):
            if (
                tts.interrupt_event.is_set()
                or self.is_stale_generation(worker_generation)
                or self.is_stale_response_generation(response_generation)
            ):
                return False
            try:
                with tts.audio_lock:
                    if (
                        tts.interrupt_event.is_set()
                        or self.is_stale_generation(worker_generation)
                        or self.is_stale_response_generation(response_generation)
                    ):
                        return False
                    playback_ok = tts.play_sound_from_bytes(audio_result)
                if playback_ok:
                    return True
                if (
                    tts.interrupt_event.is_set()
                    or self.is_stale_generation(worker_generation)
                    or self.is_stale_response_generation(response_generation)
                ):
                    return False
                log_print(
                    f"[TTS QUEUE] playback returned False. "
                    f"retry={retry_count + 1}/{max_retries} "
                    f"text={input_text}"
                )
            except Exception as e:
                log_print(
                    f"[TTS QUEUE] playback failed. "
                    f"retry={retry_count + 1}/{max_retries} "
                    f"text={input_text}, error={e}"
                )
            time.sleep(0.5)
        return False

# import threading
# import time

# import LAV_utils
# from core.global_state import global_state, GlobalKeys
# from core.logger import log_print, debug_print


# class TTSQueueWorker:#20260621_kpopmodder
#     def __init__(self, owner):
#         self.owner = owner

#     def start_if_needed(self, synthesize_function):
#         tts = self.owner

#         if (
#             tts.audio_process_thread is not None
#             and tts.audio_process_thread.is_alive()
#         ):
#             return

#         tts.audio_process_thread = threading.Thread(
#             target=self.worker_loop,
#             args=(synthesize_function,),
#         )
#         tts.audio_process_thread.daemon = True
#         tts.audio_process_thread.start()

#     def worker_loop(self, synthesize_function):
#         tts = self.owner
#         self.start_speaking_state()

#         try:
#             while True:
#                 input_text = self.get_next_input_text()

#                 if input_text is None:
#                     break

#                 if tts.interrupt_event.is_set():
#                     log_print("[TTS QUEUE] interrupt detected before synthesize")
#                     break

#                 log_print(f"[TTS QUEUE] start sentence: {input_text}")

#                 audio_result = self.synthesize_with_retry(
#                     synthesize_function,
#                     input_text,
#                     max_retries=3,
#                 )

#                 if audio_result is None:
#                     log_print(
#                         f"[TTS QUEUE] synthesize finally failed after retries: {input_text}"
#                     )
#                     continue

#                 if tts.interrupt_event.is_set():
#                     log_print("[TTS QUEUE] interrupt detected after synthesize")
#                     break

#                 tts.update_subtitle_file(input_text)

#                 playback_ok = self.playback_with_retry(
#                     audio_result,
#                     input_text,
#                     max_retries=2,
#                 )

#                 if tts.interrupt_event.is_set():
#                     log_print("[TTS QUEUE] interrupt detected during playback")
#                     break

#                 if not playback_ok:
#                     log_print(
#                         f"[TTS QUEUE] playback finally failed. force skip: {input_text}"
#                     )
#                     continue

#                 log_print(f"[TTS QUEUE] finished sentence: {input_text}")

#                 tts.process_queue_live_textbox.set(
#                     LAV_utils.queue_to_list(tts.input_queue)
#                 )

#         except Exception as e:
#             log_print(f"[TTS process_input_queue error] {e}")

#         finally:
#             self.finish_speaking_state()

#     def start_speaking_state(self):
#         global_state.set_value(GlobalKeys.IS_AI_SPEAKING, True)
#         debug_print("[TTS QUEUE] IS_AI_SPEAKING=True")

#     def finish_speaking_state(self):
#         tts = self.owner

#         tts.interrupt_event.clear()

#         global_state.set_value(GlobalKeys.IS_AI_SPEAKING, False)
#         global_state.set_value(
#             GlobalKeys.LAST_AI_SPEAK_END_TIME,
#             time.time(),
#         )

#         tts.send_output(0)
#         debug_print("[TTS QUEUE] IS_AI_SPEAKING=False")

#     def get_next_input_text(self):
#         tts = self.owner

#         with tts.queue_lock:
#             if tts.input_queue.empty():
#                 return None

#             input_text = tts.input_queue.get()

#         input_text = str(input_text).strip()

#         if not input_text:
#             return None

#         return input_text

#     def synthesize_with_retry(self, synthesize_function, input_text, max_retries=3):
#         tts = self.owner
#         audio_result = None

#         for retry_count in range(max_retries):
#             try:
#                 with tts.synth_lock:
#                     audio_result = synthesize_function(input_text)

#                 if audio_result is not None:
#                     return audio_result

#                 log_print(
#                     f"[TTS QUEUE] synthesize returned None. "
#                     f"retry={retry_count + 1}/{max_retries} text={input_text}"
#                 )

#             except Exception as e:
#                 log_print(
#                     f"[TTS QUEUE] synthesize failed. "
#                     f"retry={retry_count + 1}/{max_retries} "
#                     f"text={input_text}, error={e}"
#                 )

#             time.sleep(1.0)

#         return None

#     def playback_with_retry(self, audio_result, input_text, max_retries=2):
#         tts = self.owner

#         for retry_count in range(max_retries):
#             if tts.interrupt_event.is_set():
#                 return False

#             try:
#                 with tts.audio_lock:
#                     playback_ok = tts.play_sound_from_bytes(audio_result)

#                 if playback_ok:
#                     return True

#                 if tts.interrupt_event.is_set():
#                     return False

#                 log_print(
#                     f"[TTS QUEUE] playback returned False. "
#                     f"retry={retry_count + 1}/{max_retries} "
#                     f"text={input_text}"
#                 )

#             except Exception as e:
#                 log_print(
#                     f"[TTS QUEUE] playback failed. "
#                     f"retry={retry_count + 1}/{max_retries} "
#                     f"text={input_text}, error={e}"
#                 )

#             time.sleep(0.5)

#         return False
