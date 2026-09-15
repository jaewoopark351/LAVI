#20260915_kpopmodder: Freeze one live-issued output selection owner without retaining its ingress proof.
from dataclasses import dataclass
from types import FunctionType


@dataclass(frozen=True, slots=True)
class RoutedResponseDeferredSelection:
    owner: object
    commit: object

    def __post_init__(self):
        if self.owner is None or type(self.commit) is not FunctionType:
            raise TypeError("deferred response selection requires an owner and unbound function")

    def run(self, callback):
        try:
            return self.commit(self.owner, callback)
        except Exception:
            return None
