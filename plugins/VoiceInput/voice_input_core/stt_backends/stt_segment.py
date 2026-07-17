#20260717_kpopmodder: Isolates STT segment DTO from backend protocol.
from dataclasses import dataclass


@dataclass
class STTSegment:
    text: str
    start: float = 0.0
    end: float = 0.0
    avg_logprob: float = 0.0
    no_speech_prob: float = 0.0
    avg_logprob_available: bool = True
    no_speech_prob_available: bool = True
