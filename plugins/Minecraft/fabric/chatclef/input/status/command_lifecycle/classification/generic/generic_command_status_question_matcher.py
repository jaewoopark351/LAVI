#20260908_kpopmodder: Match the closed normalized generic STATUS-question body grammar.
from __future__ import annotations


class GenericCommandStatusQuestionMatcher:
    _BODIES = frozenset(
        {
            "뭐 해",
            "뭐해",
            "뭐 해요",
            "뭐해요",
            "뭐 하고 있어",
            "뭐하고 있어",
            "뭐 하고 있어요",
            "뭐하고 있어요",
            "뭐 하는 중이야",
            "뭐 하는 중이에요",
            "무슨 작업 하고 있어",
            "무슨 작업을 하고 있어",
            "무슨 작업 하고 있어요",
            "무슨 작업을 하고 있어요",
            "어떤 작업 하고 있어",
            "어떤 작업을 하고 있어",
            "어떤 작업 하고 있어요",
            "어떤 작업을 하고 있어요",
            "무슨 작업 하는 중이야",
            "무슨 작업을 하는 중이야",
            "무슨 작업 하는 중이에요",
            "무슨 작업을 하는 중이에요",
            "어떤 작업 하는 중이야",
            "어떤 작업을 하는 중이야",
            "어떤 작업 하는 중이에요",
            "어떤 작업을 하는 중이에요",
            "무슨 작업 중이야",
            "무슨 작업 중이에요",
            "어떤 작업 중이야",
            "어떤 작업 중이에요",
            "진행 상황 알려줘",
            "진행 상황 알려주세요",
        }
    )

    def matches(self, normalized_body: str) -> bool:
        if type(normalized_body) is not str:
            raise TypeError("normalized generic status body must be an exact str")
        body = (
            normalized_body[3:]
            if normalized_body.startswith("지금 ")
            else normalized_body
        )
        return body in self._BODIES


__all__ = ("GenericCommandStatusQuestionMatcher",)
