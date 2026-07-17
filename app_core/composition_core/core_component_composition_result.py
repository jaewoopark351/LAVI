#20260717_kpopmodder: Isolates core component composition result from the composition service.
from dataclasses import dataclass
from typing import Any, Dict, Tuple


@dataclass(frozen=True)
class CoreComponentCompositionResult:
    input: Any
    llm: Any
    translate: Any
    tts: Any
    vtuber: Any
    core_components: Tuple[Any, ...]
    startup_components: Tuple[Any, ...]

    def attribute_map(self) -> Dict[str, Any]:
        return {
            "input": self.input,
            "llm": self.llm,
            "translate": self.translate,
            "tts": self.tts,
            "vtuber": self.vtuber,
        }
