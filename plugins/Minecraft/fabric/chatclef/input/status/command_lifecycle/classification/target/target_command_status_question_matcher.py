#20260908_kpopmodder: Match characterized normalized target-qualified STATUS questions without resolving targets.
from __future__ import annotations

import re


class TargetCommandStatusQuestionMatcher:
    _PATTERNS = (
        (
            "item_get",
            re.compile(
                r"^(.+?)\s+(?:만들고 있어|만드는 중이야|구하고 있어|구하는 중이야)$"
            ),
        ),
        (
            "item_deposit",
            re.compile(r"^(.+?)\s+(?:보관하고 있어|보관하는 중이야)$"),
        ),
        (
            "item_equip",
            re.compile(r"^(.+?)\s+(?:장착하고 있어|장착하는 중이야)$"),
        ),
        (
            "item_give",
            re.compile(
                r"^(.+?)\s+(?:건네고 있어|건네는 중이야|전달하는 중이야)$"
            ),
        ),
        (
            "movement_follow",
            re.compile(r"^(.+?)\s+(?:따라가고 있어|따라가는 중이야)$"),
        ),
    )

    def match(self, normalized_body: str) -> tuple[str, str] | None:
        if type(normalized_body) is not str:
            raise TypeError("normalized target status body must be an exact str")
        for family, pattern in self._PATTERNS:
            match = pattern.fullmatch(normalized_body)
            if match is None:
                continue
            target = match.group(1).strip()
            if target.startswith("지금 "):
                target = target[3:].strip()
            if target:
                return family, target
        return None


__all__ = ("TargetCommandStatusQuestionMatcher",)
