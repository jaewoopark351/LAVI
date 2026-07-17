#20260717_kpopmodder: Isolates ScreenVision observation filter decision DTO.
from dataclasses import dataclass


@dataclass(frozen=True)
class ObservationDecision:
    observation: str
    accepted: bool
    reason: str = ""
    #20260623_kpopmodder: Keep the exact filter explanation for later tuning.
    detail: str = ""
