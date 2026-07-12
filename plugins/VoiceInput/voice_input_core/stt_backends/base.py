#20260707_kpopmodder: Common STT result types keep VoiceInput filters backend-neutral.
from dataclasses import dataclass, field
from typing import List, Optional, Protocol


@dataclass
class STTSegment:
    text: str
    start: float = 0.0
    end: float = 0.0
    avg_logprob: float = 0.0
    no_speech_prob: float = 0.0
    avg_logprob_available: bool = True
    no_speech_prob_available: bool = True


@dataclass
class STTInfo:
    language: str = "ko"
    language_probability: float = 1.0
    avg_logprob: float = 0.0
    no_speech_prob: float = 0.0
    language_probability_available: bool = True
    avg_logprob_available: bool = True
    no_speech_prob_available: bool = True


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


class STTBackendInterface(Protocol):
    def transcribe(
        self,
        audio_path: str,
        language: Optional[str] = None,
        task: str = "transcribe",
    ) -> STTResult:
        ...
