#20260620_kpopmodder: VoiceInput helper modules are grouped under voice_input_core without changing behavior.
import os
import tempfile

from core.logger import log_print


class SpeakerService:#20260616_kpopmodder
    def __init__(self, speaker_identifier):
        self.speaker_identifier = speaker_identifier

    def identify(self, audio, prefix="normal"):
        temp_path = None

        try:
            with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as temp_file:
                temp_path = temp_file.name
                temp_file.write(audio.get_wav_data())

            return self.speaker_identifier.identify(temp_path)

        except Exception as e:
            log_print(f"[SpeakerService {prefix} speaker identify error] {e}")
            return "unknown", 0.0

        finally:
            if temp_path:
                try:
                    os.remove(temp_path)
                except Exception:
                    pass
