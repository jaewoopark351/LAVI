#20260915_kpopmodder: Validate closed typed slots and exact deterministic request provenance.
from __future__ import annotations

import math
import re

from ...chatclef_intent_type import ChatClefIntentType as I
from ...chatclef_numeric_constraints import ChatClefNumericConstraints as N


class KoreanCommandRequestValidator:
    EXTENDED = frozenset({I.ATTACK, I.AUTO_DEPOSIT_TRUST, I.AUTO_DEPOSIT_TRUSTED_LIST,
        I.AUTO_DEPOSIT_UNTRUST, I.CHATCLEF, I.DEPOSIT_ALL, I.GAMER, I.GAMMA,
        I.HERO, I.LOCATE_STRUCTURE, I.OVERLAY, I.RELOAD_SETTINGS, I.RESETMEMORY, I.SCAN})

    def applies(self, intent):
        return intent.intent_type in self.EXTENDED or bool(set(intent.slots) & {"items", "bulk", "armor_set", "coordinates", "butler_user"})

    def validate(self, intent):
        if not self.applies(intent):
            return None
        try:
            self._validate_slots(intent)
            if intent.source != "rule" or not intent.original_text:
                raise ValueError("command_requires_deterministic_request")
            from ...korean_chatclef_rule_parser import KoreanChatClefRuleParser
            replay = KoreanChatClefRuleParser().parse(intent.original_text)
            fields = ("intent_type", "item_phrase", "quantity", "food_units", "x", "y", "z", "player_name", "slots", "parse_rule_id")
            if any(getattr(intent, field) != getattr(replay, field) for field in fields):
                raise ValueError("command_request_binding_mismatch")
        except (ValueError, TypeError, KeyError) as error:
            return False, str(error), "명령 인자 또는 원래 요청이 일치하지 않아 실행하지 않았어."
        return True, "validated_intent", "intent schema is valid"

    def _validate_slots(self, intent):
        kind, slots = intent.intent_type, intent.slots
        if "items" in slots:
            if kind not in {I.GET_ITEM, I.DEPOSIT_ITEM, I.DEPOSIT_ALL, I.EQUIP_ITEM} or set(slots) != {"items"}:
                raise ValueError("invalid_item_list_slots")
            items = slots["items"]
            if type(items) is not list or not 1 <= len(items) <= 64:
                raise ValueError("invalid_item_list")
            for item in items:
                if type(item) is not dict or set(item) != {"phrase", "quantity"} or type(item["phrase"]) is not str or not item["phrase"].strip():
                    raise ValueError("invalid_item_list_entry")
                N.positive_java_int(item["quantity"], "quantity")
        elif "bulk" in slots:
            if kind not in {I.DEPOSIT_ITEM, I.DEPOSIT_ALL} or slots != {"bulk": True} or slots["bulk"] is not True:
                raise ValueError("invalid_bulk_scope")
        elif "armor_set" in slots:
            if kind is not I.EQUIP_ITEM or set(slots) != {"armor_set"} or slots["armor_set"] not in {"leather", "iron", "gold", "diamond", "netherite"}:
                raise ValueError("invalid_armor_set")
        elif "coordinates" in slots:
            if kind is not I.GOTO or set(slots) != {"coordinates", "dimension"}:
                raise ValueError("invalid_goto_variant")
            from ...navigation.goto.goto_parse_result import GotoParseResult
            from ...navigation.goto.goto_parse_decision import GotoParseDecision
            if type(slots["coordinates"]) is not list:
                raise ValueError("invalid_goto_variant")
            GotoParseResult(GotoParseDecision.VALID_VARIANT, coordinates=tuple(slots["coordinates"]), dimension=slots["dimension"])
        elif "butler_user" in slots:
            if kind not in {I.FOLLOW, I.GIVE_ITEM} or slots != {"butler_user": True} or slots["butler_user"] is not True or intent.player_name:
                raise ValueError("invalid_butler_request")
            if kind is I.GIVE_ITEM:
                N.positive_java_int(intent.quantity, "quantity")
                if not intent.item_phrase:
                    raise ValueError("missing_item_phrase")
        elif kind in {I.CHATCLEF, I.OVERLAY}:
            if set(slots) != {"state"} or slots["state"] not in {"on", "off"}:
                raise ValueError("invalid_setting_state")
        elif kind is I.GAMMA:
            if set(slots) != {"value"} or type(slots["value"]) not in {int, float} or not math.isfinite(slots["value"]):
                raise ValueError("invalid_gamma_value")
        elif kind is I.AUTO_DEPOSIT_UNTRUST:
            if set(slots) != {"destination_id"} or not re.fullmatch(r"[A-Za-z0-9_.:/-]{0,160}", slots["destination_id"]):
                raise ValueError("invalid_destination_id")
        elif kind is I.LOCATE_STRUCTURE:
            if set(slots) != {"structure"} or slots["structure"] not in {"stronghold", "desert_temple"}:
                raise ValueError("unsupported_structure")
        elif slots:
            raise ValueError("unexpected_command_slots")
        if kind is I.ATTACK:
            N.positive_java_int(intent.quantity, "quantity")
            if not intent.item_phrase:
                raise ValueError("missing_attack_target")
