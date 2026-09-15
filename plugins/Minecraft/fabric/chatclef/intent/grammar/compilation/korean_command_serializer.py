#20260915_kpopmodder: Serialize typed registered-command forms without resolving names or executing commands.
from __future__ import annotations

import math
import re

from ...chatclef_intent_type import ChatClefIntentType as I
from ...chatclef_numeric_constraints import ChatClefNumericConstraints as N


class KoreanCommandSerializer:
    _ITEM_COMMANDS = {I.GET_ITEM: "get", I.EQUIP_ITEM: "equip", I.DEPOSIT_ITEM: "deposit", I.DEPOSIT_ALL: "deposit_all"}
    _TOKEN = re.compile(r"[a-zA-Z0-9_.:/-]{1,256}\Z", re.ASCII)

    def compile(self, intent, target=None):
        kind, slots = intent.intent_type, intent.slots
        if "items" in slots:
            targets = str(target or "").split(",")
            items = slots["items"]
            if len(targets) != len(items) or len(items) > 64:
                raise ValueError("item_list_target_mismatch")
            totals = {}
            for token, item in zip(targets, items):
                self.token(token, item=True)
                count = N.positive_java_int(item["quantity"], "quantity")
                totals[token] = N.positive_java_int(totals.get(token, 0) + count, "quantity_sum")
            return self._ITEM_COMMANDS[kind] + " [" + ", ".join(f"{t} {q}" for t, q in totals.items()) + "]"
        if slots.get("bulk") is True:
            if kind not in {I.DEPOSIT_ITEM, I.DEPOSIT_ALL}:
                raise ValueError("invalid_bulk_command")
            return self._ITEM_COMMANDS[kind]
        if "armor_set" in slots:
            if kind is not I.EQUIP_ITEM or slots["armor_set"] not in {"leather", "iron", "gold", "diamond", "netherite"}:
                raise ValueError("invalid_armor_set")
            return "equip " + slots["armor_set"]
        if "coordinates" in slots:
            from ...navigation.goto.goto_parse_result import GotoParseResult
            from ...navigation.goto.goto_parse_decision import GotoParseDecision
            result = GotoParseResult(GotoParseDecision.VALID_VARIANT, coordinates=tuple(slots["coordinates"]), dimension=slots["dimension"])
            return "goto " + result.canonical_arguments
        if kind is I.GIVE_ITEM and slots.get("butler_user") is True:
            return f"give {self.token(target)} {N.positive_java_int(intent.quantity, 'quantity')}"
        if kind is I.FOLLOW and slots.get("butler_user") is True:
            return "follow"
        if kind is I.ATTACK:
            return f"attack {self.token(target)} {N.positive_java_int(intent.quantity, 'quantity')}"
        if kind is I.SCAN:
            return "scan" if not intent.item_phrase else "scan " + self.token(target)
        if kind in {I.CHATCLEF, I.OVERLAY}:
            if slots.get("state") not in {"on", "off"}:
                raise ValueError("invalid_setting_state")
            return kind.value + " " + slots["state"]
        if kind is I.GAMMA:
            value = slots.get("value")
            if type(value) not in {int, float} or not math.isfinite(value):
                raise ValueError("invalid_gamma_value")
            return "gamma " + str(float(value))
        if kind is I.LOCATE_STRUCTURE:
            if slots.get("structure") not in {"stronghold", "desert_temple"}:
                raise ValueError("unsupported_structure")
            return "locate_structure " + slots["structure"]
        if kind is I.AUTO_DEPOSIT_UNTRUST:
            identifier = slots.get("destination_id", "")
            return "auto_deposit_untrust" + (" " + self.token(identifier) if identifier else "")
        if kind in {I.AUTO_DEPOSIT_TRUST, I.AUTO_DEPOSIT_TRUSTED_LIST, I.GAMER, I.HERO, I.RELOAD_SETTINGS, I.RESETMEMORY}:
            if slots:
                raise ValueError("unexpected_command_slots")
            return kind.value
        return None

    @classmethod
    def token(cls, value, *, item=False):
        if type(value) is not str or not cls._TOKEN.fullmatch(value):
            raise ValueError("invalid_resolved_command_token")
        # ItemList uses exact TaskCatalogue keys; registry-proven mod aliases may contain :./-.
        return value
