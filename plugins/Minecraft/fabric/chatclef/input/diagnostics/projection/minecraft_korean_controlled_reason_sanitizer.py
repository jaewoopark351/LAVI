#20260905_kpopmodder: Sanitize diagnostic reasons against one controlled vocabulary.
from __future__ import annotations

import re


class MinecraftKoreanControlledReasonSanitizer:
    _CONTROLLED_REASON = re.compile(r"[a-z0-9_]{1,160}\Z", re.ASCII)
    _REASON_PREFIXES = (
        "auto_deposit_trust_",
        "consumed_ingress_",
        "eligibility_",
        "generic_crafting_",
        "input_",
        "item_command_",
        "minecraft_",
        "stop_control_",
    )
    _REASON_VALUES = frozenset(
        {
            "bridge_disconnected",
            "duplicate_or_spent_stop_claim",
            "empty_input",
            "extension_unavailable",
            "handler_unavailable",
            "no_minecraft_trigger",
            "not_stop_control",
            "submission_failed",
            "translation_failed",
            "trusted_input_evidence_rejected",
            "unsafe_stop_phrase",
        }
    )

    def sanitize(self, value: object) -> str:
        if type(value) is not str or self._CONTROLLED_REASON.fullmatch(value) is None:
            return "unclassified_internal_reason"
        if value in self._REASON_VALUES or value.startswith(self._REASON_PREFIXES):
            return value
        return "unclassified_internal_reason"


__all__ = ("MinecraftKoreanControlledReasonSanitizer",)
