#20260915_kpopmodder: Accept only complete explicit Korean confirmation/cancellation utterances.
import re


class KoreanConfirmationInputClassifier:
    _CONFIRM = frozenset(
        (
            "확인",
            "확인해",
            "확인해줘",
            "실행해",
            "실행해줘",
            "응실행해",
            "응실행해줘",
            "네실행해줘",
            "그래실행해줘",
            "진행해줘",
        )
    )
    _CANCEL = frozenset(
        ("취소", "취소해", "취소해줘", "확인취소", "실행취소", "아니취소해줘", "안할래")
    )

    def classify(self, text):
        if (
            type(text) is not str
            or len(text) > 64
            or any(c in text for c in "\r\n\t?？\"'@;")
        ):
            return None
        normalized = re.sub(" +", "", text.strip())
        if normalized in self._CONFIRM:
            return "confirm"
        if normalized in self._CANCEL:
            return "cancel"
        return None
