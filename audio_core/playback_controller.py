import threading
import time

import sounddevice as sd

from core.logger import log_print


class AudioPlaybackController:#20260620_kpopmodder
    def __init__(self):
        self.play_lock = threading.RLock()
        self.is_playing = False

    def stop_before_device_change(self):
        with self.play_lock:
            try:
                if self.is_playing:
                    log_print(
                        "[AudioDeviceManager] changing output during playback. "
                        "stopping first."
                    )
                    sd.stop()
                    time.sleep(0.2)
                    self.is_playing = False
            except Exception as e:
                log_print(
                    f"[AudioDeviceManager] stop before device change error: {e}"
                )

    def play(self, audio_data, sample_rate):
        with self.play_lock:
            try:
                self.is_playing = True

                # 안정화 테스트:
                # 특정 output_device_id를 직접 지정하지 않고
                # Windows 기본 출력 장치로 재생한다.
                sd.play(
                    audio_data,
                    sample_rate,
                )

                sd.wait()

            except Exception as e:
                log_print(f"[AudioDeviceManager] playback error: {e}")

            finally:
                self.is_playing = False

    def stop(self):
        with self.play_lock:
            try:
                sd.stop()
                self.is_playing = False
            except Exception as e:
                log_print(f"[AudioDeviceManager] stop error: {e}")
