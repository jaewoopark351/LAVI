#20260905_kpopmodder: Classify only the bounded diagnostic feature identity.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults import (
    GenericCraftingDefaultsCandidateDetector,
)
from plugins.Minecraft.fabric.chatclef.input.stop import KoreanStopInputClassifier


class MinecraftKoreanFeatureIdentityClassifier:
    def __init__(self, *, stop_classifier=None, crafting_detector=None):
        self._stop_classifier = stop_classifier or KoreanStopInputClassifier()
        self._crafting_detector = (
            crafting_detector or GenericCraftingDefaultsCandidateDetector()
        )

    def classify(
        self,
        event: object,
        *,
        proof_issued: bool,
        route_kind: str,
        handled: bool,
    ) -> tuple[str, str, str]:
        if not proof_issued:
            return "none", "none", "none"
        text = getattr(event, "text", None)
        if route_kind == "stop_control":
            stop_decision = self._stop_classifier.classify(text)
            return (
                "C",
                "stop_control_v1",
                stop_decision.phrase_rule_id or "none",
            )
        if route_kind == "generic_crafting_defaults":
            candidate = self._crafting_detector.inspect(text)
            rule = candidate.rule
            return (
                "B",
                "generic_crafting_defaults_v1",
                getattr(rule, "rule_id", None) or "none",
            )
        if handled and route_kind in {"minecraft_command", "minecraft_chatclef"}:
            return "A", "minecraft_command_feedback_v1", "none"
        return "none", "none", "none"


__all__ = ("MinecraftKoreanFeatureIdentityClassifier",)
