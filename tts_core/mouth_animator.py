import io
import threading
import time
import wave

from pydub.utils import audioop

from core.logger import log_print


class TTSMouthAnimator:#20260617_kpopmodder
    def __init__(
        self,
        output_callback,
        interrupt_event,
        disabled_callback=None,
        target_fps=12,
        join_timeout=0.3,
    ):
        self.output_callback = output_callback
        self.interrupt_event = interrupt_event
        self.disabled_callback = disabled_callback

        self.target_fps = target_fps
        self.join_timeout = join_timeout

        self.thread = None
        self.stop_event = threading.Event()

    def is_disabled(self):
        if self.disabled_callback is None:
            return False

        try:
            return bool(self.disabled_callback())
        except Exception as e:
            log_print(f"[TTS mouth] disabled check error: {e}")
            return False

    def start(self, audio_data):
        if self.is_disabled():
            return

        self.stop()
        self.stop_event.clear()

        self.thread = threading.Thread(
            target=self._animation_loop,
            args=(audio_data,),
        )
        self.thread.daemon = True
        self.thread.start()

    def stop(self):
        try:
            self.stop_event.set()

            if (
                self.thread
                and self.thread.is_alive()
                and threading.current_thread() != self.thread
            ):
                self.thread.join(timeout=self.join_timeout)

        except Exception as e:
            log_print(f"[TTS mouth] stop error: {e}")

        finally:
            self.send_output(0)

    def _animation_loop(self, audio_data):
        try:
            volumes = self.extract_volumes(audio_data)

            if not volumes:
                return

            sleep_time = 1.0 / float(self.target_fps)

            for volume in volumes:
                if self.stop_event.is_set():
                    break

                if self.interrupt_event.is_set():
                    break

                self.send_output(volume)
                time.sleep(sleep_time)

        except Exception as e:
            log_print(f"[TTS mouth] animation error: {e}")

        finally:
            self.send_output(0)

    def extract_volumes(self, audio_data):
        try:
            with wave.open(io.BytesIO(audio_data), "rb") as wav_file:
                channels = wav_file.getnchannels()
                sample_width = wav_file.getsampwidth()
                frame_rate = wav_file.getframerate()
                frame_count = wav_file.getnframes()

                if frame_rate <= 0 or frame_count <= 0:
                    return []

                frames_per_chunk = max(1, int(frame_rate / self.target_fps))
                bytes_per_frame = channels * sample_width
                bytes_per_chunk = frames_per_chunk * bytes_per_frame

                raw_data = wav_file.readframes(frame_count)

            rms_values = []

            for i in range(0, len(raw_data), bytes_per_chunk):
                chunk_data = raw_data[i:i + bytes_per_chunk]

                if not chunk_data:
                    continue

                rms = audioop.rms(chunk_data, sample_width)
                rms_values.append(rms)

            if not rms_values:
                return []

            max_rms = max(rms_values)

            if max_rms <= 0:
                return []

            volumes = []

            for rms in rms_values:
                volume = rms / max_rms
                volume = min(1.0, max(0.0, volume))

                if volume < 0.05:
                    volume = 0.0

                volumes.append(volume)

            return volumes

        except Exception as e:
            log_print(f"[TTS mouth] volume extract error: {e}")
            return []

    def send_output(self, value):
        try:
            self.output_callback(value)
        except Exception as e:
            log_print(f"[TTS mouth] output callback error: {e}")