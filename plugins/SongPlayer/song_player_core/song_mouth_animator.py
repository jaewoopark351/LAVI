import io
import threading
import time
import wave

from pydub.utils import audioop

from core.logger import log_print


class SongMouthAnimator:#20260628_kpopmodder
    def __init__(
        self,
        output_callback,
        stop_event,
        target_fps=12,
        join_timeout=0.3,
    ):
        self.output_callback = output_callback
        self.stop_event = stop_event
        self.target_fps = target_fps
        self.join_timeout = join_timeout
        self.thread = None
        self.local_stop_event = threading.Event()

    def start(
        self,
        mouth_path,
        mouth_gain=1.0,
        mouth_floor=0.05,
        offset_ms=0,
    ):
        self.stop()
        self.local_stop_event.clear()

        self.thread = threading.Thread(
            target=self._animation_loop,
            args=(mouth_path, mouth_gain, mouth_floor, offset_ms),
            daemon=True,
        )
        self.thread.start()

    def stop(self):
        try:
            self.local_stop_event.set()
            if (
                self.thread
                and self.thread.is_alive()
                and threading.current_thread() != self.thread
            ):
                try:
                    self.thread.join(timeout=self.join_timeout)
                except KeyboardInterrupt:
                    log_print("[SongPlayer mouth] join skipped during Ctrl+C shutdown.")#20260630_kpopmodder
        except Exception as e:
            log_print(f"[SongPlayer mouth] stop error: {e}")
        finally:
            self.send_output(0)

    def _animation_loop(
        self,
        mouth_path,
        mouth_gain,
        mouth_floor,
        offset_ms,
    ):
        try:
            with open(mouth_path, "rb") as file:
                audio_data = file.read()

            volumes = self.extract_volumes(
                audio_data=audio_data,
                mouth_gain=mouth_gain,
                mouth_floor=mouth_floor,
            )
            if not volumes:
                return

            target_fps = self.normalized_target_fps()
            start_index = 0
            offset_sec = float(offset_ms or 0) / 1000.0

            if offset_sec > 0:
                if self.wait_or_stopped(offset_sec):
                    return
            elif offset_sec < 0:
                start_index = max(0, int(abs(offset_sec) * target_fps))

            #20260628_kpopmodder: Use elapsed time so slow frame sends do not drift later through the song.
            start_time = time.perf_counter()
            last_sent_index = None

            while not self.should_stop():
                elapsed_sec = max(0.0, time.perf_counter() - start_time)
                frame_index = self.frame_index_for_elapsed(
                    elapsed_sec,
                    start_index=start_index,
                    target_fps=target_fps,
                )

                if frame_index >= len(volumes):
                    break

                if frame_index != last_sent_index:
                    self.send_output(volumes[frame_index])
                    last_sent_index = frame_index

                next_frame_sec = (
                    (frame_index - start_index + 1) / target_fps
                )
                wait_sec = max(
                    0.001,
                    min(0.02, next_frame_sec - elapsed_sec),
                )
                if self.should_stop():
                    break
                if self.wait_or_stopped(wait_sec):
                    break

        except Exception as e:
            log_print(f"[SongPlayer mouth] animation error: {e}")
        finally:
            self.send_output(0)

    def extract_volumes(self, audio_data, mouth_gain=1.0, mouth_floor=0.05):
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
                rms_values.append(audioop.rms(chunk_data, sample_width))

            if not rms_values:
                return []

            max_rms = max(rms_values)
            if max_rms <= 0:
                return []

            volumes = []
            gain = max(0.0, float(mouth_gain or 1.0))
            floor = min(1.0, max(0.0, float(mouth_floor or 0.0)))
            for rms in rms_values:
                volume = min(1.0, max(0.0, (rms / max_rms) * gain))
                if volume < floor:
                    volume = 0.0
                volumes.append(volume)

            return volumes

        except Exception as e:
            log_print(f"[SongPlayer mouth] volume extract error: {e}")
            return []

    def normalized_target_fps(self):
        try:
            return max(1.0, float(self.target_fps))
        except Exception:
            return 12.0

    def frame_index_for_elapsed(
        self,
        elapsed_sec,
        start_index=0,
        target_fps=None,
    ):
        target_fps = target_fps or self.normalized_target_fps()
        try:
            elapsed_sec = max(0.0, float(elapsed_sec))
        except Exception:
            elapsed_sec = 0.0
        try:
            start_index = max(0, int(start_index))
        except Exception:
            start_index = 0

        return start_index + int(elapsed_sec * target_fps)

    def should_stop(self):
        external_stop = False
        if self.stop_event is not None:
            external_stop = self.stop_event.is_set()
        return external_stop or self.local_stop_event.is_set()

    def wait_or_stopped(self, seconds):
        end_time = time.time() + max(0.0, float(seconds))
        while time.time() < end_time:
            if self.should_stop():
                return True
            time.sleep(min(0.02, end_time - time.time()))
        return self.should_stop()

    def send_output(self, value):
        try:
            self.output_callback(value)
        except Exception as e:
            log_print(f"[SongPlayer mouth] output callback error: {e}")
