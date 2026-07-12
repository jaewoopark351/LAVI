#20260620_kpopmodder: VoiceInput helper modules are grouped under voice_input_core without changing behavior.
class RecordedAudio:#20260615_kpopmodder
    def __init__(self, wav_bytes: bytes):
        self.wav_bytes = wav_bytes

    def get_wav_data(self) -> bytes:
        return self.wav_bytes
