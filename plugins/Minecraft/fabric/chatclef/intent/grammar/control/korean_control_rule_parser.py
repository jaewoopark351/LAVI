#20260915_kpopmodder: Interpret closed settings and registration requests without execution authority.
from __future__ import annotations

import math
import re
import unicodedata

from ...chatclef_intent_dto import ChatClefIntentDTO
from ...chatclef_intent_type import ChatClefIntentType as I
from ..validation.korean_command_request_guard import KoreanCommandRequestGuard as Guard


class KoreanControlRuleParser:
    _END = r"(?:\s*(?:줘|주세요))?[.!。！]*"

    def parse(self, text: object) -> ChatClefIntentDTO | None:
        raw = str(text or "")
        value = unicodedata.normalize("NFKC", raw).strip()
        state = re.fullmatch(
            r"(?P<owner>챗\s*클[레래]프|채\s*클레프|chatclef|마인크래프트\s*자동화|오버레이)"
            r"(?:를|을)?\s*(?P<state>켜|꺼|켜기|끄기|활성화해|비활성화해|on|off)" + self._END,
            value, re.IGNORECASE,
        )
        if state:
            owner = I.OVERLAY if state["owner"] == "오버레이" else I.CHATCLEF
            setting = "on" if state["state"].lower() in {"켜", "켜기", "활성화해", "on"} else "off"
            rule = "chatclef_verified_spelling" if re.fullmatch(r"챗\s*클래프", state["owner"]) else ""
            return ChatClefIntentDTO(intent_type=owner, original_text=raw, source="rule",
                                     slots={"state": setting}, parse_rule_id=rule)
        if re.fullmatch(r"(?:마인크래프트|챗클레프|chatclef)\s*설정(?:을)?\s*(?:다시\s*불러와|새로\s*불러와|재로딩해)" + self._END, value, re.I):
            return self._intent(I.RELOAD_SETTINGS, raw)
        if re.fullmatch(r"(?:챗클레프|chatclef)\s*(?:대화\s*기록|기억)(?:을)?\s*초기화해" + self._END, value, re.I):
            return self._intent(I.RESETMEMORY, raw)
        gamma = re.fullmatch(r"(?:밝기|감마)(?:를|는)?\s*(?P<value>.+?)(?:으로|로)\s*(?:설정해|바꿔|변경해)" + self._END, value)
        if gamma:
            token = gamma["value"].strip()
            if not re.fullmatch(r"[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?", token):
                return Guard.reject(raw, "invalid_gamma_value")
            number = float(token)
            if not math.isfinite(number):
                return Guard.reject(raw, "invalid_gamma_value")
            return self._intent(I.GAMMA, raw, value=number)
        if re.fullmatch(r"(?:밝기|감마)(?:를)?\s*(?:기본값으로\s*(?:설정해|돌려)|초기화해)" + self._END, value):
            return self._intent(I.GAMMA, raw, value=1.0)
        if re.fullmatch(r"자동\s*보관\s*(?:장소|상자|목적지)\s*목록(?:을)?\s*(?:보여|알려)" + self._END, value):
            return self._intent(I.AUTO_DEPOSIT_TRUSTED_LIST, raw)
        untrust = re.fullmatch(r"(?:(?:이|여기)\s*)?자동\s*보관\s*(?:장소|상자|목적지)\s*(?P<id>.*?)\s*(?:등록\s*해제해|등록\s*취소해|신뢰\s*해제해)" + self._END, value)
        if untrust:
            identifier = untrust["id"].strip()
            if identifier and not re.fullmatch(r"[A-Za-z0-9_.:/-]{1,160}", identifier):
                return Guard.reject(raw, "invalid_destination_id")
            return self._intent(I.AUTO_DEPOSIT_UNTRUST, raw, destination_id=identifier)
        if re.fullmatch(r"(?:여기(?:를)?|이\s*상자(?:를)?)\s*자동\s*보관\s*(?:장소|상자|목적지)(?:로)?\s*등록해" + self._END, value):
            return self._intent(I.AUTO_DEPOSIT_TRUST, raw)
        return None

    @staticmethod
    def _intent(kind, raw, **slots):
        return ChatClefIntentDTO(intent_type=kind, original_text=raw, source="rule", slots=slots)
