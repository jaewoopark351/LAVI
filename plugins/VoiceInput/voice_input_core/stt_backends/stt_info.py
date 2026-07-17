#20260717_kpopmodder: Isolates STT aggregate info DTO from backend protocol.
from dataclasses import dataclass


@dataclass
class STTInfo:
    language: str = "ko"
    language_probability: float = 1.0
    avg_logprob: float = 0.0
    no_speech_prob: float = 0.0
    language_probability_available: bool = True
    avg_logprob_available: bool = True
    no_speech_prob_available: bool = True
