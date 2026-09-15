#20260915_kpopmodder: Observe the selected closed spelling rule without reinterpreting text or granting execution.
from collections.abc import Mapping


class MinecraftKoreanInterpretationRuleProjector:
    @staticmethod
    def project(translation):
        if not isinstance(translation, Mapping):
            return "none"
        intent = translation.get("intent")
        if (isinstance(intent, Mapping) and intent.get("intent_type") == "chatclef"
                and intent.get("parse_rule_id") == "chatclef_verified_spelling"):
            return "chatclef_verified_spelling"
        data = translation.get("data")
        if not isinstance(data, Mapping):
            return "none"
        resolutions = data.get("resolutions", (data.get("resolution"),))
        if not isinstance(resolutions, (list, tuple)) or len(resolutions) > 64:
            return "none"
        for resolution in resolutions:
            detail = resolution.get("data") if isinstance(resolution, Mapping) else None
            if isinstance(detail, Mapping) and detail.get("equipment_alias") == "레겡스":
                return "equipment_leggings_verified_spelling"
        return "none"


__all__ = ("MinecraftKoreanInterpretationRuleProjector",)
