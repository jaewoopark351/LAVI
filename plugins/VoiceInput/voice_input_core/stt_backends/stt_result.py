#20260717_kpopmodder: Isolates STT transcription result DTO from backend protocol.
from dataclasses import dataclass, field
from typing import List

from .stt_info import STTInfo
from .stt_segment import STTSegment


@dataclass
class STTResult:
    text: str = ""
    segments: List[STTSegment] = field(default_factory=list)
    language: str = "ko"
    language_probability: float = 1.0
    avg_logprob: float = 0.0
    no_speech_prob: float = 0.0
    language_probability_available: bool = True
    avg_logprob_available: bool = True
    no_speech_prob_available: bool = True
    info: STTInfo = field(default_factory=STTInfo)
