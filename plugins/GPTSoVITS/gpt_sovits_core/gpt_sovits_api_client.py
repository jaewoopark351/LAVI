#20260620_kpopmodder: GPTSoVITS helper modules are grouped under gpt_sovits_core without changing behavior.
import io
import os
import uuid

import requests
from pydub import AudioSegment

from core.logger import log_print


class GPTSoVITSApiClient:#20260619_kpopmodder
    def __init__(self, current_module_directory, gpt_sovits_url):
        self.current_module_directory = current_module_directory
        self.gpt_sovits_url = gpt_sovits_url

    def synthesize_to_file(
        self,
        text,
        output_path,
        text_language,
        ref_audio_path,
        prompt_text,
        prompt_language
    ):
        text = text.strip()

        if not text:
            log_print("[GPTSoVITS_TTS] Empty text. Skipping.")
            return None

        if not ref_audio_path:
            raise ValueError("Reference Audio Path가 비어 있습니다.")

        if not os.path.exists(ref_audio_path):
            raise FileNotFoundError(
                f"Reference audio not found: {ref_audio_path}"
            )

        params = {
            "text": text,
            "text_lang": text_language,
            "ref_audio_path": ref_audio_path,
            "prompt_text": prompt_text,
            "prompt_lang": prompt_language,
            "media_type": "wav",
            "streaming_mode": "false"
        }

        log_print(f"[GPTSoVITS_TTS] Requesting GPT-SoVITS: {self.gpt_sovits_url}")

        response = requests.get(
            self.gpt_sovits_url,
            params=params,
            timeout=120
        )

        if response.status_code != 200:
            raise RuntimeError(
                f"GPT-SoVITS API failed: {response.status_code} {response.text}"
            )

        with open(output_path, "wb") as file:
            file.write(response.content)

        log_print(f"[GPTSoVITS_TTS] Output saved: {output_path}")
        return output_path

    def synthesize_to_bytes(
        self,
        text,
        text_language,
        ref_audio_path,
        prompt_text,
        prompt_language
    ):
        wav_filename = os.path.join(
            self.current_module_directory,
            f"gpt_sovits_output_{uuid.uuid4().hex}.wav"
        )

        try:
            self.synthesize_to_file(
                text=text,
                output_path=wav_filename,
                text_language=text_language,
                ref_audio_path=ref_audio_path,
                prompt_text=prompt_text,
                prompt_language=prompt_language
            )

            audio = AudioSegment.from_wav(wav_filename)
            buffer = io.BytesIO()
            audio.export(buffer, format="wav")

            return buffer.getvalue()

        finally:
            try:
                if os.path.exists(wav_filename):
                    os.remove(wav_filename)
                    log_print(f"[GPTSoVITS_TTS] temp deleted: {wav_filename}")
            except Exception as e:
                log_print(f"[GPTSoVITS_TTS] temp delete failed: {e}")
