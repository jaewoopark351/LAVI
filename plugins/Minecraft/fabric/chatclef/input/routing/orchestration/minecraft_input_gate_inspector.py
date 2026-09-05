#20260905_kpopmodder: Isolate modern and legacy input-gate adaptation.
from plugins.Minecraft.fabric.chatclef.input.gating.contracts import (
    MinecraftChatClefInputGateDecision,
)


class MinecraftInputGateInspector:
    def __init__(self, intent_gate):
        self._intent_gate = intent_gate

    def inspect(self, text: str):
        inspect = getattr(self._intent_gate, "inspect", None)
        if callable(inspect):
            return inspect(text)
        if self._intent_gate.should_consider(text):
            return MinecraftChatClefInputGateDecision.generic()
        return MinecraftChatClefInputGateDecision.none()


__all__ = ("MinecraftInputGateInspector",)
