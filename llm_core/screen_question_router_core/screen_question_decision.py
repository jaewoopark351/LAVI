#20260717_kpopmodder: Isolates screen-question route result DTO from router logic.
from dataclasses import dataclass


@dataclass
class ScreenQuestionDecision:#20260628_kpopmodder: Small route result passed per request.
    intent: str
    need_screen: bool
    reason: str
    confidence: float = 0.0
    fallback_used: bool = False
