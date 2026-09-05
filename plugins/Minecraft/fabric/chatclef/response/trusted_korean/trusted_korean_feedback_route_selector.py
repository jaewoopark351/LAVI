#20260905_kpopmodder: Isolate typed route-outcome selection from response rendering.
from __future__ import annotations


class TrustedKoreanFeedbackRouteSelector:
    EXISTING = "existing"
    DISCONNECTED = "disconnected"
    BUSY = "busy"
    TERMINAL_UNKNOWN = "terminal_unknown"
    ITEM_REJECTION = "item_rejection"
    CRAFTING_REJECTION = "crafting_rejection"
    CRAFTING_SUBMITTED = "crafting_submitted"

    def select(self, decision: object) -> str:
        if getattr(decision, "handled", False) is not True:
            return self.EXISTING
        reason = str(getattr(decision, "reason", "") or "").strip()
        direct = {
            "minecraft_bridge_disconnected": self.DISCONNECTED,
            "minecraft_command_busy": self.BUSY,
            "minecraft_submission_outcome_unknown": self.TERMINAL_UNKNOWN,
            "minecraft_item_command_translation_rejected": self.ITEM_REJECTION,
        }.get(reason)
        if direct is not None:
            return direct
        if str(getattr(decision, "route_kind", "") or "").strip() != (
            "generic_crafting_defaults"
        ):
            return self.EXISTING
        if reason == "minecraft_generic_crafting_defaults_rejected":
            return self.CRAFTING_REJECTION
        if reason == "minecraft_command_routed":
            return self.CRAFTING_SUBMITTED
        return self.EXISTING


__all__ = ("TrustedKoreanFeedbackRouteSelector",)
