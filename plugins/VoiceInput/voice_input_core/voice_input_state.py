#20260620_kpopmodder: VoiceInput helper modules are grouped under voice_input_core without changing behavior.
from dataclasses import dataclass, field
from threading import Lock
from typing import Optional


@dataclass
class VoiceInputState:#20260618_kpopmodder
    mic_mode: str = "open mic"
    recording: bool = False
    input_language: str = "korean"
    ambience_adjusted: bool = False

    last_interrupt_time: float = 0.0
    last_interrupt_check_time: float = 0.0

    input_device_index: Optional[int] = None#20260625_kpopmodder: Use system default input device unless Audio Settings has a valid current input label.
    mic_lock: object = field(default_factory=Lock)
