#20260908_kpopmodder: Match characterized normalized family-specific STATUS questions without lifecycle state.
from __future__ import annotations


class FamilyCommandStatusQuestionMatcher:
    _FAMILY_FORMS = (
        (
            "item_get",
            frozenset(
                {
                    "지금 뭐 만들고 있어",
                    "뭐 만들고 있어",
                    "지금 뭐 만드는 중이야",
                    "뭐 만드는 중이야",
                    "지금 뭐 구하고 있어",
                    "뭐 구하는 중이야",
                }
            ),
        ),
        (
            "movement_goto",
            frozenset({"지금 어디로 가고 있어", "어디로 가는 중이야"}),
        ),
        (
            "store_home",
            frozenset({"아이템 집에 정리하는 중이야", "집에 정리하고 있어"}),
        ),
    )

    def match(self, normalized_body: str) -> str | None:
        if type(normalized_body) is not str:
            raise TypeError("normalized family status body must be an exact str")
        for family, forms in self._FAMILY_FORMS:
            if normalized_body in forms:
                return family
        return None


__all__ = ("FamilyCommandStatusQuestionMatcher",)
