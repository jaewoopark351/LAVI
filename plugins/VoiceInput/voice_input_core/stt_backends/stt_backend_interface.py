#20260717_kpopmodder: Isolates common STT backend protocol.
from typing import Optional, Protocol

from .stt_result import STTResult


class STTBackendInterface(Protocol):
    def transcribe(
        self,
        audio_path: str,
        language: Optional[str] = None,
        task: str = "transcribe",
    ) -> STTResult:
        ...
