#20260915_kpopmodder: Preserve native item lists, explicit bulk modes, armor sets and recipient defaults.
from __future__ import annotations

import re

from ...chatclef_command_safety import ChatClefCommandSafetyValidator
from ...chatclef_intent_dto import ChatClefIntentDTO
from ...chatclef_intent_type import ChatClefIntentType as I
from ...korean_acquisition_verb_matcher import KoreanAcquisitionVerbMatcher
from ...korean_quantity_parser import KoreanQuantityParser
from ..validation.korean_command_request_guard import KoreanCommandRequestGuard as Guard


class KoreanItemListRuleParser:
    _END = r"(?:\s*(?:줘|주세요))?[.!。！]*"
    #20260915_openai: Only this native head gets typed interpretation, never arbitrary @ passthrough.
    _NATIVE_DEPOSIT_HEAD = re.compile(r"@?deposit_all\b", re.IGNORECASE)
    _SPOKEN_DEPOSIT_HEAD = re.compile(r"(?:deposit\s+all|디포짓\s*올)(?=$|\s|[.!。！?？])", re.IGNORECASE)
    _DEPOSIT_BULK = re.compile(
        r"(?:@?deposit_all|deposit\s+all|디포짓\s*올)"
        r"(?:\s*(?:실행해|해)(?:\s*(?:줘|주세요))?)?[.!。！]*", re.IGNORECASE
    )
    _ITEM_ACTION = re.compile(r"(?P<body>.*?)\s*(?P<verb>전체\s*보관해|보관해|저장해|넣어|장착해|착용해|입어)" + _END)
    _SETS = {"가죽": "leather", "철": "iron", "금": "gold", "다이아몬드": "diamond", "다이아": "diamond", "네더라이트": "netherite"}

    def __init__(self, quantity_parser=None, acquisition_verbs=None):
        self._quantity = quantity_parser or KoreanQuantityParser()
        self._acquisition = acquisition_verbs or KoreanAcquisitionVerbMatcher()

    def parse(self, text: object) -> ChatClefIntentDTO | None:
        native = self.parse_native_deposit_all(text)
        if native is not None:
            return native
        raw = str(text or "").strip()
        give = re.fullmatch(r"(?:(?P<player>[A-Za-z0-9_]{3,16}|나)(?:에게|한테)\s*)?(?P<body>.+?)\s*(?:전달해|넘겨)" + self._END, raw)
        if give is None:
            give = re.fullmatch(r"(?P<player>[A-Za-z0-9_]{3,16}|나)(?:에게|한테)\s*(?P<body>.+?)\s*(?:줘|주세요)[.!。！]*", raw)
        if give:
            player = give["player"] or "나"
            return self._items(I.GIVE_ITEM, raw, give["body"], player="" if player == "나" else player,
                               extra={"butler_user": True} if player == "나" else {})
        action = self._ITEM_ACTION.fullmatch(raw)
        if action:
            body = action["body"].strip()
            if action["verb"] in {"장착해", "착용해", "입어"}:
                armor = re.fullmatch(r"(?P<material>가죽|철|금|다이아몬드|다이아|네더라이트)\s*(?:방어구|갑옷)\s*(?:세트|전체)(?:를|을)?", body)
                if armor:
                    return ChatClefIntentDTO(intent_type=I.EQUIP_ITEM, original_text=raw,
                                             source="rule", slots={"armor_set": self._SETS[armor["material"]]})
                return self._items(I.EQUIP_ITEM, raw, body)
            if re.fullmatch(r"(?:인벤토리(?:를|의)?|소지품(?:을)?|아이템(?:을)?)\s*(?:전부|모두|전체)(?:를|다)?", body):
                return ChatClefIntentDTO(intent_type=I.DEPOSIT_ALL, original_text=raw, source="rule", slots={"bulk": True})
            if re.fullmatch(r"(?:장비와\s*도구를\s*제외한|장비\s*제외)\s*(?:아이템(?:을)?\s*)?(?:전부|모두)", body):
                return ChatClefIntentDTO(intent_type=I.DEPOSIT_ITEM, original_text=raw, source="rule", slots={"bulk": True})
            kind = I.DEPOSIT_ALL if action["verb"].startswith("전체") or body.startswith("전체 보관 ") else I.DEPOSIT_ITEM
            body = re.sub(r"^전체\s*보관\s+", "", body)
            body = re.sub(r"(?:상자|창고|보관함)\s*(?:안에|에|로)\s*", "", body).strip()
            return self._items(kind, raw, body)
        if self._acquisition.matches(raw):
            body = self._acquisition.strip(raw)
            # Single-item acquisition keeps its established parser/resolution profile.
            if self._has_separator(body):
                return self._items(I.GET_ITEM, raw, body)
        return None

    def parse_native_deposit_all(self, text: object) -> ChatClefIntentDTO | None:
        """Parse a bounded native request into existing slots, preserving raw safety checks."""
        raw = str(text or "").strip()
        native = self._NATIVE_DEPOSIT_HEAD.match(raw)
        spoken = self._SPOKEN_DEPOSIT_HEAD.match(raw)
        if native is None and spoken is None:
            return None
        # Reject controls before trimming (including a trailing newline), and never repair negation.
        reason = Guard.reason(text)
        prefixless = raw[1:] if raw.startswith("@") else raw
        if reason or ChatClefCommandSafetyValidator.has_dangerous_text(prefixless):
            return Guard.reject(text, reason or "dangerous_command_slot")
        if self._DEPOSIT_BULK.fullmatch(raw):
            return ChatClefIntentDTO(intent_type=I.DEPOSIT_ALL, original_text=raw,
                                     source="rule", slots={"bulk": True})
        if native is None:
            return Guard.reject(text, "invalid_deposit_all_syntax")
        body = raw[native.end():].strip()
        if body.startswith("[") and body.endswith("]"):
            body = body[1:-1].strip()
        if not body or "[" in body or "]" in body:
            return Guard.reject(text, "invalid_deposit_all_item_list")
        # Native item/count pairs use unitless integers; other Korean forms keep their existing parser.
        body = re.sub(r"(?<=\s)([+-]?[0-9]+)\s*(?=,|$)", r"\1개", body)
        parsed = self._items(I.DEPOSIT_ALL, raw, body)
        if parsed.intent_type is I.DEPOSIT_ALL and any(
            re.search(r"(?:^|\s)[+-]?[0-9]+(?:\.[0-9]+)?(?:\s|$)", item["phrase"])
            for item in parsed.slots.get("items", ())
        ):
            return Guard.reject(text, "invalid_item_quantity")
        return parsed

    def _has_separator(self, body):
        # A legacy comma between one item and its quantity is punctuation, not a list.
        comma_parts = body.split(",")
        if len(comma_parts) == 2:
            try:
                if comma_parts[0].strip() and not self._quantity.strip_quantity(comma_parts[1]):
                    return False
            except ValueError:
                pass
        return bool(re.search(r",|\s+(?:및|하고)\s+|(?:개|마리|하나|둘|셋|넷)(?:와|과)\s*|\s+(?:와|과)\s+", body))

    def _items(self, kind, raw, body, player="", extra=None):
        body = re.sub(r"\s*(?:을|를)$", "", body.strip())
        parts = re.split(r"\s*,\s*|\s+(?:및|하고|와|과)\s+|(?<=[개리])(?:와|과)\s*|(?<=하나)(?:와|과)\s*|(?<=[둘셋넷])(?:와|과)\s*", body)
        try:
            if not 1 <= len(parts) <= 64 or (kind is I.GIVE_ITEM and len(parts) != 1):
                raise ValueError("unsupported_item_list")
            items = []
            for part in parts:
                if re.search(r"(?:마리|포인트|만큼)\s*(?:$|와|과)", part):
                    raise ValueError("invalid_item_quantity_unit")
                quantity = self._quantity.parse_optional(part)
                if quantity is None and kind is I.DEPOSIT_ITEM:
                    raise ValueError("missing_deposit_quantity")
                quantity = 1 if quantity is None else quantity
                phrase = re.sub(r"[을를]$", "", self._quantity.strip_quantity(part)).strip()
                if not phrase or re.search(r"[0-9]+(?:\.[0-9]+)?\s*(?:개|마리)\s*$", phrase):
                    raise ValueError("invalid_item_quantity")
                items.append({"phrase": phrase, "quantity": quantity})
            slots = dict(extra or {})
            if len(items) == 1 and kind is not I.DEPOSIT_ALL:
                return ChatClefIntentDTO(intent_type=kind, item_phrase=items[0]["phrase"], quantity=items[0]["quantity"],
                                         player_name=player, original_text=raw, source="rule", slots=slots)
            slots["items"] = items
            return ChatClefIntentDTO(intent_type=kind, original_text=raw, source="rule", slots=slots)
        except ValueError as error:
            return Guard.reject(raw, str(error))
