#20260717_kpopmodder: Compatibility facade for common VoiceInput STT backend types.
from .stt_backend_interface import STTBackendInterface
from .stt_info import STTInfo
from .stt_result import STTResult
from .stt_segment import STTSegment

__all__ = [
    "STTBackendInterface",
    "STTInfo",
    "STTResult",
    "STTSegment",
]
