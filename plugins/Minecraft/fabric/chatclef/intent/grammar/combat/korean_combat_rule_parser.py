#20260915_kpopmodder: Parse explicit combat, food-score and persistent-task requests.
from __future__ import annotations

import re

from ...chatclef_intent_dto import ChatClefIntentDTO
from ...chatclef_intent_type import ChatClefIntentType as I
from ...korean_quantity_parser import KoreanQuantityParser
from ..validation.korean_command_request_guard import KoreanCommandRequestGuard as Guard


class KoreanCombatRuleParser:
    _END = r"(?:\s*(?:줘|주세요))?[.!。！]*"

    def __init__(self, quantity_parser=None):
        self._quantity = quantity_parser or KoreanQuantityParser()

    def parse(self, text: object) -> ChatClefIntentDTO | None:
        raw = str(text or "")
        value = raw.strip()
        if re.fullmatch(r"(?:마인크래프트\s*)?(?:엔딩까지|끝까지)\s*(?:진행해|깨)" + self._END, value):
            return self._intent(I.GAMER, raw)
        if re.fullmatch(r"(?:적대\s*몹|적들)(?:을)?\s*(?:계속\s*)?(?:정리해|처치해)" + self._END, value):
            return self._intent(I.HERO, raw)
        if re.fullmatch(r"(?:가만히\s*있어|대기해|대기|쉬어)" + self._END, value):
            return self._intent(I.IDLE, raw)
        if re.fullmatch(r"(?:멈춰|중지해|정지해|중지|정지|스톱|그만)" + self._END, value):
            return self._intent(I.STOP, raw)
        follow = re.fullmatch(r"(?P<player>[A-Za-z0-9_]{3,16}|나(?:를)?)\s*(?:따라가|따라와|팔로우해|쫓아가)" + self._END, value)
        if follow:
            player = follow["player"]
            return ChatClefIntentDTO(intent_type=I.FOLLOW, player_name="" if player in {"나", "나를"} else player,
                                     original_text=raw, source="rule", slots={"butler_user": True} if player in {"나", "나를"} else {})
        attack = re.fullmatch(r"(?P<body>.+?)\s*(?:공격해|처치해|죽여)" + self._END, value)
        if attack:
            body = attack["body"]
            try:
                if re.search(r"(?:개|포인트|만큼)\s*$", body):
                    raise ValueError("invalid_attack_quantity_unit")
                quantity = self._quantity.parse(body)
                phrase = re.sub(r"[을를]$", "", self._quantity.strip_quantity(body)).strip()
            except ValueError as error:
                return Guard.reject(raw, str(error))
            if not phrase or phrase.startswith("플레이어 "):
                return Guard.reject(raw, "player_attack_not_allowed")
            return ChatClefIntentDTO(intent_type=I.ATTACK, quantity=quantity, item_phrase=phrase, original_text=raw, source="rule")
        food = re.fullmatch(r"(?P<type>음식|고기)\s*(?P<count>.+?)\s*(?:모아|구해|얻어)" + self._END, value)
        if food:
            try:
                if re.search(r"(?:개|마리)", food["count"]):
                    raise ValueError("invalid_food_quantity_unit")
                count = self._quantity.parse_optional(food["count"])
                residue = self._quantity.strip_quantity(food["count"])
                if count is None or residue:
                    raise ValueError("invalid_food_units")
            except ValueError as error:
                return Guard.reject(raw, str(error))
            return ChatClefIntentDTO(intent_type=I.FOOD if food["type"] == "음식" else I.MEAT,
                                     food_units=count, original_text=raw, source="rule")
        return None

    @staticmethod
    def _intent(kind, raw):
        return ChatClefIntentDTO(intent_type=kind, original_text=raw, source="rule")
