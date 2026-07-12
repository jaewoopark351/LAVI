#20260621_kpopmodder: Interrupt 때 TTS queue generation을 올려 기존 합성/재생 결과를 폐기한다.
from queue import Empty
import threading

from core.logger import log_print


class TTSInterruptController:#20260621_kpopmodder
    def __init__(self, owner):
        self.owner = owner

    # def handle_interrupt(self):#20260621_kpopmodder
    #     tts = self.owner
    #     new_generation = tts.bump_queue_generation()#20260621_kpopmodder
    #     tts.interrupt_event.set()
    #     log_print(f"Interrupting pipeline. TTS generation={new_generation}")
    #     self.stop_audio_safely()
    #     self.stop_mouth_safely()
    #     self.clear_queues()
    #     self.close_mouth_safely()
    #     self.join_audio_thread_safely()
    #     if not self.is_audio_thread_alive():
    #         tts.interrupt_event.clear()

    def handle_interrupt(self):#20260621_kpopmodder
        tts = self.owner
        new_generation = tts.bump_queue_generation()#20260621_kpopmodder
        tts.interrupt_event.set()
        log_print(f"Interrupting pipeline. TTS generation={new_generation}")

        self.stop_audio_safely()
        self.stop_mouth_safely()
        self.clear_queues()
        self.close_mouth_safely()
        self.join_audio_thread_safely()

        if not self.is_audio_thread_alive():
            tts.interrupt_event.clear()

    def cancel_pending(self, reason=""):#20260711_kpopmodder
        #20260711_kpopmodder: Cancel current/pending TTS without raising the
        # app-wide interrupt flag, so a new post-boundary sentence can restart
        # after the stale worker exits even when synthesis takes over 0.3 sec.
        tts = self.owner
        new_generation = tts.bump_queue_generation()
        reason_text = str(reason or "").strip()
        reason_suffix = f" reason={reason_text}" if reason_text else ""
        log_print(
            "Cancelling current and pending TTS. "
            f"generation={new_generation}{reason_suffix}"
        )

        self.stop_audio_safely()
        self.stop_mouth_safely()
        self.clear_queues()
        self.close_mouth_safely()
        self.join_audio_thread_safely()
        return new_generation

    def stop_audio_safely(self):
        tts = self.owner
        try:
            tts.audio_player.stop()
        except Exception as e:
            log_print(f"[TTS interrupt] audio stop error: {e}")

    def stop_mouth_safely(self):
        tts = self.owner
        try:
            tts.mouth_animator.stop()
        except Exception as e:
            log_print(f"[TTS interrupt] mouth animator stop error: {e}")

    # def clear_queues(self):#20260621_kpopmodder
    #     tts = self.owner
    #     try:
    #         with tts.queue_lock:
    #             self.clear_queue(tts.input_queue)
    #             # 과거 구조 호환용. audio_data_queue가 없으면 그냥 무시한다.
    #             audio_data_queue = getattr(tts, "audio_data_queue", None)
    #             if audio_data_queue is not None:
    #                 self.clear_queue(audio_data_queue)
    #             try:
    #                 tts.process_queue_live_textbox.set(tts.get_queue_display_items())
    #             except Exception:
    #                 pass
    #     except Exception as e:
    #         log_print(f"[TTS interrupt] queue clear error: {e}")

    def clear_queues(self):#20260621_kpopmodder
        tts = self.owner
        try:
            with tts.queue_lock:
                self.clear_queue(tts.input_queue)

                audio_data_queue = getattr(tts, "audio_data_queue", None)
                if audio_data_queue is not None:
                    self.clear_queue(audio_data_queue)

                try:
                    tts.process_queue_live_textbox.set(
                        tts.get_queue_display_items()
                    )
                except Exception:
                    pass

        except Exception as e:
            log_print(f"[TTS interrupt] queue clear error: {e}")

    def clear_queue(self, queue):
        while True:
            try:
                queue.get_nowait()
            except Empty:
                break
            except Exception as e:
                log_print(f"[TTS interrupt] queue get error: {e}")
                break

    def close_mouth_safely(self):
        tts = self.owner
        try:
            tts.send_output(0)
        except Exception as e:
            log_print(f"[TTS interrupt] mouth close output error: {e}")

    def join_audio_thread_safely(self):
        tts = self.owner
        try:
            audio_process_thread = tts.audio_process_thread
            if (
                audio_process_thread
                and audio_process_thread.is_alive()
                and threading.current_thread() != audio_process_thread
            ):
                audio_process_thread.join(timeout=0.3)
        except Exception as e:
            log_print(f"[TTS interrupt] join error: {e}")

    def is_audio_thread_alive(self):
        tts = self.owner
        audio_process_thread = tts.audio_process_thread
        return bool(
            audio_process_thread
            and audio_process_thread.is_alive()
        )

# from queue import Empty
# import threading

# from core.logger import log_print


# class TTSInterruptController:#20260621_kpopmodder
#     def __init__(self, owner):
#         self.owner = owner

#     def handle_interrupt(self):
#         tts = self.owner

#         tts.interrupt_event.set()
#         log_print("Interrupting pipeline")

#         self.stop_audio_safely()
#         self.stop_mouth_safely()
#         self.clear_queues()
#         self.close_mouth_safely()
#         self.join_audio_thread_safely()

#         if not self.is_audio_thread_alive():
#             tts.interrupt_event.clear()

#     def stop_audio_safely(self):
#         tts = self.owner

#         try:
#             tts.audio_player.stop()
#         except Exception as e:
#             log_print(f"[TTS interrupt] audio stop error: {e}")

#     def stop_mouth_safely(self):
#         tts = self.owner

#         try:
#             tts.mouth_animator.stop()
#         except Exception as e:
#             log_print(f"[TTS interrupt] mouth animator stop error: {e}")

#     def clear_queues(self):
#         tts = self.owner

#         try:
#             with tts.queue_lock:
#                 self.clear_queue(tts.input_queue)

#                 # 과거 구조 호환용.
#                 # audio_data_queue가 없으면 그냥 무시한다.
#                 audio_data_queue = getattr(tts, "audio_data_queue", None)

#                 if audio_data_queue is not None:
#                     self.clear_queue(audio_data_queue)

#         except Exception as e:
#             log_print(f"[TTS interrupt] queue clear error: {e}")

#     def clear_queue(self, queue):
#         while True:
#             try:
#                 queue.get_nowait()
#             except Empty:
#                 break
#             except Exception as e:
#                 log_print(f"[TTS interrupt] queue get error: {e}")
#                 break

#     def close_mouth_safely(self):
#         tts = self.owner

#         try:
#             tts.send_output(0)
#         except Exception as e:
#             log_print(f"[TTS interrupt] mouth close output error: {e}")

#     def join_audio_thread_safely(self):
#         tts = self.owner

#         try:
#             audio_process_thread = tts.audio_process_thread

#             if (
#                 audio_process_thread
#                 and audio_process_thread.is_alive()
#                 and threading.current_thread() != audio_process_thread
#             ):
#                 audio_process_thread.join(timeout=0.3)

#         except Exception as e:
#             log_print(f"[TTS interrupt] join error: {e}")

#     def is_audio_thread_alive(self):
#         tts = self.owner
#         audio_process_thread = tts.audio_process_thread

#         return bool(
#             audio_process_thread
#             and audio_process_thread.is_alive()
#         )
