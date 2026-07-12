#20260620_kpopmodder: VoiceInput helper modules are grouped under voice_input_core without changing behavior.
import os
import numpy as np
from resemblyzer import VoiceEncoder, preprocess_wav
from core.logger import log_print, debug_print#20260612_kpopmodder


class SpeakerIdentifier:
    def __init__(self, reference_dir, threshold=0.72, device=None):
        self.reference_dir = reference_dir
        self.threshold = threshold
        self.device = device
        #20260627_kpopmodder: Match speaker encoder device with VoiceInput to avoid cuda:0/cuda:1 tensor mismatch.
        self.encoder = VoiceEncoder(device=device)
        self.speakers = {}

        self.load_reference_voices()

    def load_reference_voices(self):
        if not os.path.exists(self.reference_dir):
            os.makedirs(self.reference_dir, exist_ok=True)
            log_print(f"[SpeakerIdentifier] Created reference dir: {self.reference_dir}")#20260612_kpopmodder
            return

        for filename in os.listdir(self.reference_dir):
            if not filename.lower().endswith(".wav"):
                continue

            speaker_name = os.path.splitext(filename)[0].lower()
            wav_path = os.path.join(self.reference_dir, filename)

            try:
                wav = preprocess_wav(wav_path)
                embed = self.encoder.embed_utterance(wav)
                self.speakers[speaker_name] = embed
                log_print(f"[SpeakerIdentifier] Loaded speaker: {speaker_name}")#20260612_kpopmodder
            except Exception as e:
                log_print(f"[SpeakerIdentifier] Failed to load {filename}: {e}")#20260612_kpopmodder

    def identify(self, wav_path):
        if not self.speakers:
            return "unknown", 0.0

        try:
            wav = preprocess_wav(wav_path)
            current_embed = self.encoder.embed_utterance(wav)
        except Exception as e:
            log_print(f"[SpeakerIdentifier] Failed to identify speaker: {e}")#20260612_kpopmodder
            return "unknown", 0.0

        best_speaker = "unknown"
        best_score = -1.0

        for speaker_name, ref_embed in self.speakers.items():
            score = self.cosine_similarity(current_embed, ref_embed)

            if score > best_score:
                best_score = score
                best_speaker = speaker_name

        if best_score < self.threshold:
            return "unknown", float(best_score)

        return best_speaker, float(best_score)

    @staticmethod
    def cosine_similarity(a, b):
        a = np.asarray(a)
        b = np.asarray(b)

        denominator = np.linalg.norm(a) * np.linalg.norm(b)
        if denominator == 0:
            return 0.0

        return float(np.dot(a, b) / denominator)
