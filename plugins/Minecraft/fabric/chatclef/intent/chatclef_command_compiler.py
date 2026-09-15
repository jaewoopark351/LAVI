#20260803_kpopmodder: Added strict ChatClef DSL compilation from validated intents.
#20260827_kpopmodder: Compile zero-slot STORE_HOME to one prefixless command.
#20260905_kpopmodder: Compile H5 to one fixed prefixless Java grammar form.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.intent.chatclef_command_safety import (
    ChatClefCommandSafetyValidator,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_numeric_constraints import (
    ChatClefNumericConstraints,
)


class ChatClefCommandCompiler:
    _AUTO_DEPOSIT_TRUST_AREA_COMMAND = "auto_deposit_trust area 16x16"
    #20260915_kpopmodder: Match the bounded native metadata token contract after capability validation.
    _TARGET_RE = re.compile(r"[A-Za-z0-9_.:/-]{1,256}\Z", re.ASCII)
    _PLAYER_RE = re.compile(r"^[A-Za-z0-9_]{3,16}$")
    _DANGEROUS_RE = re.compile(r"[;#\r\n\"'@]|[\x00-\x1f\x7f]")

    #20260915_kpopmodder: Keep FIND vocabulary instance-owned and lazily loaded; other commands are unchanged.
    def __init__(self, find_resolver=None):
        self._find_resolver = find_resolver

    @property
    def find_resolver(self):
        if self._find_resolver is None:
            from .navigation.find.find_target_resolver import FindTargetResolver
            self._find_resolver = FindTargetResolver()
        return self._find_resolver

    def resolve_find(self, intent: ChatClefIntentDTO):
        from .navigation.find import FindRequest
        from .chatclef_intent_schema_validator import ChatClefIntentSchemaValidator
        valid, reason, _ = ChatClefIntentSchemaValidator().validate(intent)
        if not valid or intent.intent_type is not ChatClefIntentType.FIND:
            raise ValueError(reason or "not_find_intent")
        return self.find_resolver.resolve(FindRequest.from_slots(intent.slots))

    def compile(self, intent: ChatClefIntentDTO, target: str | None = None) -> str:
        #20260914_kpopmodder: Allow only the parsed leading @find, never a generic raw-command bypass.
        if intent.intent_type is ChatClefIntentType.FIND:
            return self.resolve_find(intent).request.compile()
        #20260915_openai: Exempt only a successfully parsed deposit_all head; provenance below still
        # requires the original request to replay to these exact slots, and output is re-serialized.
        from .grammar.item.korean_item_list_rule_parser import KoreanItemListRuleParser
        native_deposit = (
            KoreanItemListRuleParser().parse_native_deposit_all(intent.original_text)
            if intent.intent_type is ChatClefIntentType.DEPOSIT_ALL else None
        )
        if native_deposit is None or native_deposit.intent_type is not ChatClefIntentType.DEPOSIT_ALL:
            self.reject_dangerous_text(intent.original_text)
        #20260915_kpopmodder: Delegate new domain serializers while preserving established scalar forms.
        from .grammar.compilation.korean_command_serializer import KoreanCommandSerializer
        from .grammar.validation.korean_command_request_validator import KoreanCommandRequestValidator
        validation = KoreanCommandRequestValidator().validate(intent)
        if validation is not None and not validation[0]:
            raise ValueError(validation[1])
        extended = KoreanCommandSerializer().compile(intent, target)
        if extended is not None:
            return extended
        if intent.intent_type is ChatClefIntentType.GET_ITEM:
            target_text = self._target(target)
            quantity = self._positive_int(intent.quantity, "quantity")
            return f"get {target_text} {quantity}"
        if intent.intent_type is ChatClefIntentType.EQUIP_ITEM:
            quantity = 1 if intent.quantity is None else self._positive_int(intent.quantity, "quantity")
            return f"equip {self._target(target)}" + (f" {quantity}" if quantity != 1 else "")
        if intent.intent_type is ChatClefIntentType.DEPOSIT_ITEM:
            target_text = self._target(target)
            quantity = self._positive_int(intent.quantity, "quantity")
            return f"deposit {target_text} {quantity}"
        if intent.intent_type is ChatClefIntentType.AUTO_DEPOSIT_TRUST_AREA:
            return self._AUTO_DEPOSIT_TRUST_AREA_COMMAND
        if intent.intent_type is ChatClefIntentType.STORE_HOME:
            return "store_home"
        if intent.intent_type is ChatClefIntentType.GIVE_ITEM:
            player_name = intent.player_name
            self.reject_dangerous_text(player_name)
            if not self._PLAYER_RE.fullmatch(player_name):
                raise ValueError("invalid_player_name")
            #20260915_kpopmodder: GIVE inventory lookup can use a full mod translation key.
            target_text = KoreanCommandSerializer.token(target)
            quantity = self._positive_int(intent.quantity, "quantity")
            return f"give {player_name} {target_text} {quantity}"
        if intent.intent_type is ChatClefIntentType.FOOD:
            return f"food {self._positive_int(intent.food_units, 'food_units')}"
        if intent.intent_type is ChatClefIntentType.MEAT:
            return f"meat {self._positive_int(intent.food_units, 'food_units')}"
        if intent.intent_type is ChatClefIntentType.GOTO:
            x = self._java_int(intent.x, "x")
            y = self._java_int(intent.y, "y")
            z = self._java_int(intent.z, "z")
            return f"goto {x} {y} {z}"
        if intent.intent_type is ChatClefIntentType.FOLLOW:
            player_name = intent.player_name
            self.reject_dangerous_text(player_name)
            if not self._PLAYER_RE.fullmatch(player_name):
                raise ValueError("invalid_player_name")
            return f"follow {player_name}"
        if intent.intent_type is ChatClefIntentType.IDLE:
            return "idle"
        if intent.intent_type is ChatClefIntentType.STOP:
            return "stop"
        raise ValueError("unsupported_intent_type")

    def has_dangerous_text(self, text: object) -> bool:
        return ChatClefCommandSafetyValidator.has_dangerous_text(text)

    def reject_dangerous_text(self, text: object) -> None:
        if self.has_dangerous_text(text):
            raise ValueError("dangerous_command_slot")

    def _target(self, target: str | None) -> str:
        target_text = str(target or "")
        self.reject_dangerous_text(target_text)
        if not self._TARGET_RE.fullmatch(target_text):
            raise ValueError("invalid_target")
        return target_text

    def _positive_int(self, value: int | None, field_name: str) -> int:
        return ChatClefNumericConstraints.positive_java_int(value, field_name)

    def _required_int(self, value: int | None, field_name: str) -> int:
        if value is None:
            raise ValueError(f"missing_{field_name}")
        return ChatClefNumericConstraints.required_exact_int(value, field_name)

    def _java_int(self, value: int | None, field_name: str) -> int:
        if value is None:
            raise ValueError(f"missing_{field_name}")
        return ChatClefNumericConstraints.java_int(value, field_name)
