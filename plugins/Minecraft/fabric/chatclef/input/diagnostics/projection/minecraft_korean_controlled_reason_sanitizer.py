#20260905_kpopmodder: Sanitize diagnostic reasons against one controlled vocabulary.
from __future__ import annotations

import re


class MinecraftKoreanControlledReasonSanitizer:
    #20260915_kpopmodder: Actual bounded name decisions selected by the trusted rejection owner.
    NAME_REASONS = frozenset({"ambiguous_registered_name", "ambiguous_native_command_token",
        "unknown_item_phrase", "empty_item_phrase", "unknown_registered_item", "unknown_registered_target",
        "unsupported_material", "unsupported_equipment_material", "native_command_target_unsupported",
        "runtime_catalogue_required", "target_not_equippable", "target_not_in_chatclef_catalog", "ambiguous_item_phrase"})
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
            #20260915_kpopmodder: Closed name-resolution decisions, never caller-provided prose or target IDs.
            "ambiguous_registered_name",
            "ambiguous_native_command_token",
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
        if value in self._REASON_VALUES or value in self.NAME_REASONS or value.startswith(self._REASON_PREFIXES):
            return value
        return "unclassified_internal_reason"


__all__ = ("MinecraftKoreanControlledReasonSanitizer",)
