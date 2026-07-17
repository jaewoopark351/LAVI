#20260717_kpopmodder: Isolates normalized LLM input DTO from interaction context logic.
from dataclasses import dataclass


@dataclass
class NormalizedInput:#20260621_kpopmodder
    text: str
    display_text: str
    remember_history: bool = True
    kind: str = "user"
    source: str = ""
    observation: str = ""
