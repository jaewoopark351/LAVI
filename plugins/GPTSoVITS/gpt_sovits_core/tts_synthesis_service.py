#20260620_kpopmodder: GPTSoVITS helper modules are grouped under gpt_sovits_core without changing behavior.
import io
import os
import uuid

from pydub import AudioSegment

from core.logger import log_print


class TTSSynthesisService:#20260617_kpopmodder
    def __init__(self, current_module_directory, gpt_sovits, rvc=None):
        self.current_module_directory = current_module_directory
        self.gpt_sovits = gpt_sovits
        self.rvc = rvc

    def synthesize(self, text, use_rvc=False):
        text = (text or "").strip()

        if not text:
            log_print("[TTSSynthesisService] Empty text. Skipping synthesis.")
            return None

        wav_filename = os.path.join(
            self.current_module_directory,
            f"gpt_sovits_output_{uuid.uuid4().hex}.wav"
        )

        try:
            self.gpt_sovits.synthesize_to_file(text, wav_filename)

            if use_rvc and self.rvc is not None:
                audio_bytes = self.rvc.convert_file_to_bytes(wav_filename)

                if audio_bytes is not None:
                    return audio_bytes

                log_print(
                    "[TTSSynthesisService] RVC failed. "
                    "Fallback to GPT-SoVITS output."
                )

            elif use_rvc and self.rvc is None:
                log_print(
                    "[TTSSynthesisService] RVC requested but removed. "
                    "Fallback to GPT-SoVITS output."
                )

            return self.wav_file_to_bytes(wav_filename)

        except Exception as e:
            log_print(f"[TTSSynthesisService] synthesize error: {e}")
            return None

        finally:
            self.delete_temp_file(wav_filename)

    def wav_file_to_bytes(self, wav_filename):
        audio = AudioSegment.from_wav(wav_filename)

        buffer = io.BytesIO()
        audio.export(buffer, format="wav")

        return buffer.getvalue()

    def delete_temp_file(self, file_path):
        try:
            if os.path.exists(file_path):
                os.remove(file_path)
                log_print(f"[TTSSynthesisService] temp deleted: {file_path}")

        except Exception as e:
            log_print(f"[TTSSynthesisService] temp delete failed: {e}")
