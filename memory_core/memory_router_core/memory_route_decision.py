#20260717_kpopmodder: Isolates memory routing result DTO from router logic.
from dataclasses import dataclass, field


@dataclass
class MemoryRouteDecision:
    intent: str
    need_memory: bool
    reason: str
    queries: list[str] = field(default_factory=list)
    memory_scope: list[str] = field(default_factory=list)
    max_items: int = 0
    fallback_used: bool = False
