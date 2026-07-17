#20260717_kpopmodder: Isolates Hybrid OpenAI route decision DTO.
from dataclasses import dataclass


@dataclass
class RouteDecision:
    route: str
    reason: str = ""
    forced: bool = False
    fallback_used: bool = False
