import os
import tempfile
import winsound
import traceback

from core.logger import log_print


def play_wav_file_async(path):
    #20260717_kpopmodder: Shared Windows async WAV playback path for non-TTS audio adapters too.
    winsound.PlaySound(
        path,
        winsound.SND_FILENAME | winsound.SND_ASYNC,
    )


def stop_winsound_playback():
    #20260717_kpopmodder: Keep direct winsound stop calls behind one helper.
    winsound.PlaySound(None, 0)


class WinSoundAudioPlayer:#20260617_kpopmodder
    def __init__(self, interrupt_event, mouth_animator=None):
        self.interrupt_event = interrupt_event
        self.mouth_animator = mouth_animator

    def play_from_bytes(self, audio_data):
        if audio_data is None:
            return False

        temp_wav_path = None

        try:
            if self.interrupt_event.is_set():
                return False

            temp_wav_path = self.write_temp_wav_file(audio_data)

            log_print(
                f"[TTS playback] using winsound for stability. "
                f"bytes={len(audio_data)}, file={temp_wav_path}"
            )

            if self.mouth_animator is not None:
                self.mouth_animator.start(audio_data)

            winsound.PlaySound(
                temp_wav_path,
                winsound.SND_FILENAME,
            )

            if self.interrupt_event.is_set():
                return False

            log_print("[TTS playback] winsound play done")
            return True

        except Exception as e:
            log_print(f"[TTS playback error] {e}")
            log_print(traceback.format_exc())
            return False

        finally:
            if self.mouth_animator is not None:
                self.mouth_animator.stop()

            if self.interrupt_event.is_set():
                self.stop()

            self.delete_temp_wav_file_safely(temp_wav_path)

    def stop(self):
        try:
            stop_winsound_playback()

        except Exception as e:
            log_print(f"[TTS playback] winsound stop error: {e}")

    def write_temp_wav_file(self, audio_data):
        fd, temp_wav_path = tempfile.mkstemp(
            prefix="lav_tts_",
            suffix=".wav",
        )

        with os.fdopen(fd, "wb") as file:
            file.write(audio_data)

        return temp_wav_path

    def delete_temp_wav_file_safely(self, temp_wav_path):
        if not temp_wav_path:
            return

        try:
            if os.path.exists(temp_wav_path):
                os.remove(temp_wav_path)

        except Exception as e:
            log_print(f"[TTS playback] temp wav delete error: {e}")
