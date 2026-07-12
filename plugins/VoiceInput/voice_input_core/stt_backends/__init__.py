#20260707_kpopmodder: Added STT backend package to decouple VoiceInput from faster-whisper.
from .base import STTBackendInterface, STTInfo, STTResult, STTSegment
from .transformers_whisper_backend import TransformersWhisperBackend

__all__ = [
    "STTBackendInterface",
    "STTInfo",
    "STTResult",
    "STTSegment",
    "TransformersWhisperBackend",
]
