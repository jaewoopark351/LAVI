#20260717_kpopmodder: Isolates ScreenVision observation flow result DTO.
from dataclasses import dataclass


@dataclass
class ScreenObservationFlowResult:
    observation: str
    decision: object
    accepted: bool
