#20260803_kpopmodder: Added strict intent validation before DSL compilation.
#20260827_kpopmodder: Enforce the STORE_HOME zero-slot contract.
#20260905_kpopmodder: Enforce the deterministic rule-only H5 zero-slot contract.
from __future__ import annotations

import re
from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_numeric_constraints import (
    JAVA_INT_MAX,
    JAVA_INT_MIN,
)


class ChatClefIntentSchemaValidator:
    _ALLOWED_FIELDS = set(ChatClefIntentDTO().to_dict()) | {"parse_rule_id"}
    _FORBIDDEN_FIELDS = {
        "command",
        "chatclef_command",
        "target",
        "minecraft_id",
        "dsl",
        "executable",
    }
    _PLAYER_RE = re.compile(r"^[A-Za-z0-9_]{3,16}$")

    def validate(self, value: Any) -> tuple[bool, str, str]:
        try:
            payload = self._payload(value)
            forbidden = sorted(set(payload) & self._FORBIDDEN_FIELDS)
            if forbidden:
                return False, "forbidden_intent_fields", ",".join(forbidden)
            extra = sorted(set(payload) - self._ALLOWED_FIELDS)
            if extra:
                return False, "unknown_intent_fields", ",".join(extra)
            intent = ChatClefIntentDTO.from_mapping(payload)
        except Exception as error:
            return False, "malformed_intent", f"{type(error).__name__}: {error}"
        return self._validate_intent(intent)

    def _payload(self, value: Any) -> Mapping[str, Any]:
        if isinstance(value, ChatClefIntentDTO):
            return value.to_dict()
        if isinstance(value, Mapping):
            return value
        raise TypeError("intent must be a mapping or ChatClefIntentDTO")

    def _validate_intent(self, intent: ChatClefIntentDTO) -> tuple[bool, str, str]:
        #20260915_kpopmodder: Interpretation metadata cannot be attached to unrelated intent families.
        if intent.parse_rule_id and intent.intent_type is not ChatClefIntentType.CHATCLEF:
            return False, "invalid_parse_rule_id", "parse rule metadata does not match this command"
        #20260915_kpopmodder: New families and native subgrammars have a focused closed-slot owner.
        from .grammar.validation.korean_command_request_validator import KoreanCommandRequestValidator
        extended = KoreanCommandRequestValidator().validate(intent)
        if extended is not None:
            return extended
        if intent.intent_type is ChatClefIntentType.UNKNOWN:
            return True, "validated_unknown_intent", "unknown intent is non-executable"
        #20260914_kpopmodder: Runtime resolution owns IDs; Python only admits strict rule slots.
        if intent.intent_type is ChatClefIntentType.FIND:
            from .navigation.find import FindRequest, KoreanFindRuleParser
            try:
                request = FindRequest.from_slots(intent.slots)
                parsed = KoreanFindRuleParser().parse(intent.original_text)
                if (intent.source != "rule" or parsed is None
                        or parsed.intent_type is not ChatClefIntentType.FIND
                        or parsed.slots != request.slots()
                        or any((intent.quantity is not None, intent.item_phrase,
                                intent.food_units is not None, intent.x is not None,
                                intent.y is not None, intent.z is not None, intent.player_name))):
                    raise ValueError("find_requires_exact_rule_request")
            except (ValueError, TypeError):
                return False, "invalid_find_request", "FIND requires one explicit target request."
            return True, "validated_intent", "intent schema is valid"
        if intent.intent_type is ChatClefIntentType.STORE_HOME:
            if intent.source != "rule":
                return (
                    False,
                    "store_home_requires_rule_source",
                    "store_home can only be authorized by deterministic rules",
                )
            if any(
                (
                    intent.quantity is not None,
                    bool(intent.item_phrase.strip()),
                    intent.food_units is not None,
                    intent.x is not None,
                    intent.y is not None,
                    intent.z is not None,
                    bool(intent.player_name.strip()),
                    bool(intent.slots),
                )
            ):
                return (
                    False,
                    "store_home_requires_zero_slots",
                    "store_home does not accept item, quantity, player, coordinate, or extra slots",
                )
            return True, "validated_intent", "intent schema is valid"
        if intent.intent_type is ChatClefIntentType.AUTO_DEPOSIT_TRUST_AREA:
            if intent.source != "rule":
                return (
                    False,
                    "auto_deposit_trust_area_requires_rule_source",
                    "auto_deposit_trust_area can only be authorized by deterministic rules",
                )
            if any(
                (
                    intent.quantity is not None,
                    bool(intent.item_phrase.strip()),
                    intent.food_units is not None,
                    intent.x is not None,
                    intent.y is not None,
                    intent.z is not None,
                    bool(intent.player_name.strip()),
                    bool(intent.slots),
                )
            ):
                return (
                    False,
                    "auto_deposit_trust_area_requires_zero_slots",
                    "auto_deposit_trust_area does not accept user-controlled slots",
                )
            return True, "validated_intent", "intent schema is valid"
        if intent.intent_type in {
            ChatClefIntentType.GET_ITEM,
            ChatClefIntentType.DEPOSIT_ITEM,
            ChatClefIntentType.GIVE_ITEM,
        }:
            if not intent.item_phrase.strip():
                return False, "missing_item_phrase", "item action requires item_phrase"
            if (
                intent.intent_type is ChatClefIntentType.DEPOSIT_ITEM
                and intent.quantity is None
            ):
                return False, "missing_deposit_quantity", "deposit_item requires explicit quantity"
            if (
                intent.quantity is None
                or intent.quantity < 1
                or intent.quantity > JAVA_INT_MAX
            ):
                return False, "invalid_quantity", "item action quantity must be in 1..2147483647"
        if intent.intent_type is ChatClefIntentType.EQUIP_ITEM:
            if not intent.item_phrase.strip():
                return False, "missing_item_phrase", "equip_item requires item_phrase"
            #20260915_kpopmodder: Explicit EQUIP quantities retain ItemList integer validation.
            if intent.quantity is not None and not 1 <= intent.quantity <= JAVA_INT_MAX:
                return False, "invalid_quantity", "equipment quantity must be in 1..2147483647"
        if intent.intent_type in {ChatClefIntentType.FOOD, ChatClefIntentType.MEAT}:
            if (
                intent.food_units is None
                or intent.food_units < 1
                or intent.food_units > JAVA_INT_MAX
            ):
                return False, "invalid_food_units", "food units must be in 1..2147483647"
        if intent.intent_type is ChatClefIntentType.GOTO:
            if intent.x is None or intent.y is None or intent.z is None:
                return False, "missing_coordinates", "goto requires x, y, and z"
            if not (
                JAVA_INT_MIN <= intent.x <= JAVA_INT_MAX
                and JAVA_INT_MIN <= intent.y <= JAVA_INT_MAX
                and JAVA_INT_MIN <= intent.z <= JAVA_INT_MAX
            ):
                return False, "invalid_coordinates", "goto coordinates must be Java int values"
        if intent.intent_type is ChatClefIntentType.FOLLOW:
            if not self._PLAYER_RE.fullmatch(intent.player_name):
                return False, "invalid_player_name", "follow requires a valid player name"
        if intent.intent_type is ChatClefIntentType.GIVE_ITEM:
            if not self._PLAYER_RE.fullmatch(intent.player_name):
                return False, "invalid_player_name", "give_item requires a valid player name"
        return True, "validated_intent", "intent schema is valid"
