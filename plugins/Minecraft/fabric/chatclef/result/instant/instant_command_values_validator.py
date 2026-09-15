#20260915_kpopmodder: Validate command-specific readbacks independently of transport ownership.
from __future__ import annotations

from collections.abc import Mapping
import math
import re
from ...intent.auto_deposit_trust.classification.korean_auto_deposit_trust_intent_classifier import KoreanAutoDepositTrustIntentClassifier
from ...intent.auto_deposit_trust.contracts import AutoDepositTrustIntentDecision


class InstantCommandValuesValidator:
    @staticmethod
    def valid(name, command, outcome, reason, values):
        if not isinstance(values, Mapping):
            return False
        parts = command.split()
        success = outcome == "completed"
        if outcome == "unknown":
            return (reason == "RESULT_UNAVAILABLE" and not values) or (
                name == "reload_settings" and reason == "RELOAD_RETURNED"
                and set(values) == {"callback_returned", "configuration_files_verified"}
                and values["callback_returned"] is True
                and values["configuration_files_verified"] is False)
        if name in {"give", "follow"}:
            allowed = {"BUTLER_USER_UNAVAILABLE"}
            if name == "give":
                allowed |= {"PLAYER_NOT_LOADED", "ITEM_UNAVAILABLE"}
            return not success and reason in allowed and not values
        if name in {"chatclef", "overlay"}:
            return (reason == "SETTING_APPLIED" and set(values) == {"enabled"}
                    and type(values["enabled"]) is bool and len(parts) == 2
                    and parts[1] in {"on", "off"}
                    and success == (values["enabled"] == (parts[1] == "on")))
        if name == "gamma":
            if set(values) != {"requested", "value"}:
                return False
            if any(type(v) not in (int, float) or not math.isfinite(v) for v in values.values()):
                return False
            try:
                expected = float(parts[1]) if len(parts) == 2 else 1.0
            except ValueError:
                return False
            return (len(parts) <= 2 and values["requested"] == expected
                    and success == (values["requested"] == values["value"])
                    and reason == ("SETTING_APPLIED" if success else "VALUE_NOT_APPLIED"))
        if name == "resetmemory":
            count = values.get("remaining_messages")
            return (reason == "MEMORY_CLEARED" and set(values) == {"remaining_messages", "base_prompt_retained"}
                    and type(values["base_prompt_retained"]) is bool
                    and type(count) is int and 0 <= count <= 2_147_483_647
                    and success == (count == 0))
        if name == "scan":
            if not success and reason == "INVALID_BLOCK":
                return not values
            expected = parts[1] if len(parts) == 2 else "DIRT"
            if (values.get("block") != expected or len(parts) > 2
                    or not re.fullmatch(r"[A-Za-z0-9_]{1,256}", expected)):
                return False
            if not success and reason == "NOT_FOUND":
                return set(values) == {"block"}
            return (success and reason == "BLOCK_FOUND" and set(values) == {"block", "x", "y", "z"}
                    and all(type(values[k]) is int and -2_147_483_648 <= values[k] <= 2_147_483_647
                            for k in ("x", "y", "z")))
        if name == "auto_deposit_trusted_list":
            return success and reason == "LISTED" and InstantCommandValuesValidator._destinations(values)
        if name in {"auto_deposit_trust", "auto_deposit_untrust", "자동보관등록"}:
            return InstantCommandValuesValidator._trust(name, parts, success, reason, values)
        return False

    @staticmethod
    def _trust(name, parts, success, reason, values):
        if (name in {"auto_deposit_trust", "자동보관등록"} and len(parts) == 3 and parts[-1] == "16x16"
                and KoreanAutoDepositTrustIntentClassifier().classify(" ".join(parts)).decision
                is AutoDepositTrustIntentDecision.AUTO_DEPOSIT_TRUST_AREA):
            keys = {"registered", "reenabled", "already_registered", "coverage_complete"}
            return (name in {"auto_deposit_trust", "자동보관등록"} and set(values) == keys
                    and type(values["coverage_complete"]) is bool
                    and all(type(values[k]) is int and 0 <= values[k] <= 2_147_483_647
                            for k in keys - {"coverage_complete"})
                    and reason in ({"UPDATED", "NO_CHANGE"} if success else {
                        "REGISTRY_READ_FAILED", "INVALID_REQUEST", "PREEXISTING_DOUBLE_CHEST_DUPLICATE",
                        "EXTERNAL_MODIFICATION_CONFLICT", "PERSISTENCE_FAILED", "SCAN_FAILED",
                        "INVALID_ANCHOR", "SCAN_COVERAGE_INCOMPLETE", "AMBIGUOUS_DOUBLE_CHEST"}))
        if not success and reason == "TARGET_UNAVAILABLE":
            return not values
        allowed = ({"REMOVED"} if name == "auto_deposit_untrust" else
                   {"REGISTERED", "UPDATED", "ALREADY_REGISTERED"}) if success else {
                       "NOT_FOUND", "AMBIGUOUS_ID", "PERSISTENCE_FAILED"}
        destination = values.get("destination_id")
        return (reason in allowed and set(values) == {"destination_id"}
                and type(destination) is str
                and re.fullmatch(r"[A-Za-z0-9_-]{0,128}", destination) is not None)

    @staticmethod
    def _destinations(values):
        if set(values) != {"total", "listed", "truncated", "destinations"}:
            return False
        total, listed, entries = values["total"], values["listed"], values["destinations"]
        if (type(total) is not int or type(listed) is not int or not 0 <= listed <= min(total, 64)
                or total > 2_147_483_647 or type(values["truncated"]) is not bool
                or values["truncated"] != (listed < total)
                or type(entries) not in (list, tuple) or len(entries) != listed):
            return False
        for entry in entries:
            if not isinstance(entry, Mapping) or set(entry) != {"destination_id", "dimension", "x", "y", "z", "status"}:
                return False
            if (type(entry["destination_id"]) is not str
                    or not re.fullmatch(r"[A-Za-z0-9_-]{1,128}", entry["destination_id"])
                    or entry["dimension"] not in {"overworld", "nether", "end"}
                    or entry["status"] not in {"KNOWN_AVAILABLE", "KNOWN_FULL", "MISSING",
                                                "KNOWN_UNREACHABLE", "UNKNOWN_OR_STALE"}
                    or any(type(entry[k]) is not int or not -2_147_483_648 <= entry[k] <= 2_147_483_647
                           for k in ("x", "y", "z"))):
                return False
        return len({entry["destination_id"] for entry in entries}) == listed
