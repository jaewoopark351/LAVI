#20260907_kpopmodder: Classify closed Korean status-question shapes without resolving targets.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.intent.korean_text_normalizer import (
    KoreanTextNormalizer,
)

from .command_status_query import CommandStatusQuery


class CommandStatusQueryClassifier:
    _ADDRESSEE = re.compile(r"^(?:마크|마인크래프트)\s+")
    _ANY = frozenset({"지금 뭐 하고 있어", "뭐 하고 있어", "지금 무슨 작업 하고 있어"})
    _FAMILY_ONLY = {
        "item_get": frozenset(
            {
                "지금 뭐 만들고 있어",
                "뭐 만들고 있어",
                "지금 뭐 만드는 중이야",
                "뭐 만드는 중이야",
                "지금 뭐 구하고 있어",
                "뭐 구하는 중이야",
            }
        ),
        "movement_goto": frozenset(
            {"지금 어디로 가고 있어", "어디로 가는 중이야"}
        ),
        "store_home": frozenset(
            {"아이템 집에 정리하는 중이야", "집에 정리하고 있어"}
        ),
    }
    _TARGET_PATTERNS = (
        ("item_get", re.compile(r"^(.+?)\s+(?:만들고 있어|만드는 중이야|구하고 있어|구하는 중이야)$")),
        ("item_deposit", re.compile(r"^(.+?)\s+(?:보관하고 있어|보관하는 중이야)$")),
        ("item_equip", re.compile(r"^(.+?)\s+(?:장착하고 있어|장착하는 중이야)$")),
        ("item_give", re.compile(r"^(.+?)\s+(?:건네고 있어|건네는 중이야|전달하는 중이야)$")),
        ("movement_follow", re.compile(r"^(.+?)\s+(?:따라가고 있어|따라가는 중이야)$")),
    )

    def __init__(self, normalizer=None) -> None:
        self._normalizer = normalizer or KoreanTextNormalizer()

    def classify(self, text: object) -> CommandStatusQuery | None:
        normalized = self._normalizer.normalize(text)
        addressed = self._ADDRESSEE.match(normalized) is not None
        body = self._ADDRESSEE.sub("", normalized, count=1) if addressed else normalized
        if addressed and body in self._ANY:
            return CommandStatusQuery(requested_family="any", addressed=addressed)
        for family, forms in self._FAMILY_ONLY.items():
            if addressed and body in forms:
                return CommandStatusQuery(
                    requested_family=family,
                    addressed=addressed,
                )
        if not addressed and (
            body in self._ANY
            or any(body in forms for forms in self._FAMILY_ONLY.values())
        ):
            return None
        for family, pattern in self._TARGET_PATTERNS:
            match = pattern.fullmatch(body)
            if match is not None:
                target = match.group(1).strip()
                if target.startswith("지금 "):
                    target = target[3:].strip()
                if target:
                    return CommandStatusQuery(
                        requested_family=family,
                        addressed=addressed,
                        target_text=target,
                    )
        return None


__all__ = ("CommandStatusQueryClassifier",)
