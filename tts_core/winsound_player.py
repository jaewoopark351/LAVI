import io
import os
import tempfile
import traceback
import wave
import winsound

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
    def __init__(self, interrupt_event, mouth_animator=None, volume_scale_getter=None):
        self.interrupt_event = interrupt_event
        self.mouth_animator = mouth_animator
        self.volume_scale_getter = volume_scale_getter

    def play_from_bytes(self, audio_data):
        if audio_data is None:
            return False

        temp_wav_path = None

        try:
            if self.interrupt_event.is_set():
                return False

            playback_audio_data = self.apply_volume(audio_data)
            temp_wav_path = self.write_temp_wav_file(playback_audio_data)

            log_print(
                f"[TTS playback] using winsound for stability. "
                f"bytes={len(playback_audio_data)}, file={temp_wav_path}"
            )

            if self.mouth_animator is not None:
                self.mouth_animator.start(playback_audio_data)

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

    def apply_volume(self, audio_data):
        scale = self.get_volume_scale()
        if scale == 1.0:
            return audio_data
        return self.scale_wav_audio(audio_data, scale)

    def get_volume_scale(self):
        if self.volume_scale_getter is None:
            return 1.0

        try:
            scale = float(self.volume_scale_getter())
        except Exception as e:
            log_print(f"[TTS playback] volume getter error: {e}")
            return 1.0

        return min(2.0, max(0.0, scale))

    def scale_wav_audio(self, audio_data, scale):
        try:
            with wave.open(io.BytesIO(audio_data), "rb") as source:
                params = source.getparams()
                frames = source.readframes(source.getnframes())

            scaled_frames = self.scale_pcm_frames(
                frames,
                sample_width=params.sampwidth,
                scale=scale,
            )

            output = io.BytesIO()
            with wave.open(output, "wb") as target:
                target.setparams(params)
                target.writeframes(scaled_frames)
            return output.getvalue()

        except Exception as e:
            log_print(f"[TTS playback] volume apply failed: {e}")
            return audio_data

    def scale_pcm_frames(self, frames, sample_width, scale):
        if sample_width not in (1, 2, 3, 4):
            return frames

        output = bytearray()
        frame_end = len(frames) - (len(frames) % sample_width)

        for index in range(0, frame_end, sample_width):
            chunk = frames[index:index + sample_width]
            if sample_width == 1:
                sample = int(chunk[0]) - 128
                scaled = int(round(sample * scale))
                scaled = min(127, max(-128, scaled))
                output.append(scaled + 128)
                continue

            sample = int.from_bytes(chunk, byteorder="little", signed=True)
            max_value = (1 << (sample_width * 8 - 1)) - 1
            min_value = -(1 << (sample_width * 8 - 1))
            scaled = int(round(sample * scale))
            scaled = min(max_value, max(min_value, scaled))
            output.extend(
                int(scaled).to_bytes(
                    sample_width,
                    byteorder="little",
                    signed=True,
                )
            )

        if frame_end < len(frames):
            output.extend(frames[frame_end:])

        return bytes(output)

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
