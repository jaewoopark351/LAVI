#20260901_kpopmodder: Name the exact Gradio command transport used by one-shot tests.
from __future__ import annotations

from enum import Enum


class CommandSubmissionTransport(str, Enum):
    KOREAN = "korean"
    RAW = "raw"
