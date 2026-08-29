#20260827_kpopmodder: Own deterministic Korean STORE_HOME candidate and execution rules.
#20260828_kpopmodder: Preserve question marks and reject compound actions before STORE_HOME execution.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.intent.korean_text_normalizer import (
    KoreanTextNormalizer,
)
from plugins.Minecraft.fabric.chatclef.intent.store_home.store_home_intent_classification import (
    StoreHomeIntentClassification,
    StoreHomeIntentDecision,
)


class KoreanStoreHomeIntentClassifier:
    _DESTINATION_TERMS = ("집에", "집으로", "집상자", "집창고")
    _TARGET_TERMS = (
        "인벤토리",
        "아이템",
        "물건",
        "짐",
        "남는것",
        "남는거",
        "남는물건",
    )
    _ACTION_TERMS = ("넣", "저장", "보관", "정리", "가져다", "갖다", "옮겨")
    _NEGATION_TERMS = (
        "하지마",
        "넣지마",
        "저장하지마",
        "보관하지마",
        "정리하지마",
        "안넣어",
        "말고",
        "취소",
        "그냥들고있어",
        "안저장해",
        "안보관해",
        "안정리해",
        "저장안해",
        "보관안해",
        "정리안해",
        "하지말",
    )
    _DEFERRED_TERMS = ("나중에", "이따", "지금말고")
    _QUESTION_TERMS = (
        "할까",
        "해도돼",
        "하면될까",
        "해야하나",
        "해야할까",
        "방법알려줘",
        "어떻게해",
        "넣어도돼",
        "저장해도돼",
        "보관해도돼",
        "정리해도돼",
    )
    _OTHER_COMMAND_TERMS = (
        "캐와",
        "캐줘",
        "구해",
        "가져와",
        "만들어",
        "제작해",
        "찾아",
        "따라가",
        "팔로우",
        "쫓아가",
        "이동",
        "공격",
        "입어",
        "착용",
        "장착",
        "전달해",
        "멈춰",
        "중지",
    )
    _COMPOUND_SEQUENCE_TERMS = (
        "그리고",
        "그다음",
        "다음으로",
        "그후",
        "한뒤",
        "한다음",
    )
    _COMPOUND_ACTION_LINKS = (
        "넣고",
        "저장하고",
        "보관하고",
        "정리하고",
        "켜고",
        "끄고",
        "열고",
        "닫고",
        "점프하고",
        "대기하고",
        "쉬고",
        "모으고",
        "모아주고",
        "주고",
        "입고",
        "착용하고",
        "장착하고",
        "이동하고",
        "따라가고",
        "팔로우하고",
        "쫓아가고",
        "캐고",
        "캐와주고",
        "구하고",
        "구해주고",
        "가져오고",
        "가져와주고",
        "만들고",
        "만들어주고",
        "제작하고",
        "찾고",
        "찾아주고",
        "멈추고",
        "중지하고",
        "정지하고",
    )
    _DIRECT_ACTION_ENDINGS = (
        "넣어",
        "넣어줘",
        "넣어주세요",
        "넣어둬",
        "넣어놔",
        "저장해",
        "저장해줘",
        "저장해주세요",
        "보관해",
        "보관해줘",
        "보관해주세요",
        "가져다놔",
        "가져다놔줘",
        "가져다둬",
        "갖다놔",
        "갖다놔줘",
        "갖다둬",
        "옮겨놔",
        "옮겨놔줘",
        "옮겨둬",
        "옮겨줘",
        "옮겨주세요",
        "정리해",
        "정리해줘",
        "정리해주세요",
    )

    def __init__(self, normalizer: KoreanTextNormalizer | None = None):
        self._normalizer = normalizer or KoreanTextNormalizer()

    def classify(self, text: object) -> StoreHomeIntentClassification:
        has_question_mark = re.search(r"[?？]", str(text or "")) is not None
        normalized = self._normalizer.normalize(text)
        compact = re.sub(r"\s+", "", normalized)
        if not compact:
            return self._decision(StoreHomeIntentDecision.NO_MATCH)

        has_destination = self._has_destination(normalized, compact)
        has_target = any(term in compact for term in self._TARGET_TERMS)
        has_action = any(term in compact for term in self._ACTION_TERMS)
        if not has_action or not (has_destination or has_target):
            return self._decision(StoreHomeIntentDecision.NO_MATCH)

        if any(term in compact for term in self._NEGATION_TERMS):
            return self._decision(StoreHomeIntentDecision.NEGATED)
        if any(term in compact for term in self._DEFERRED_TERMS):
            return self._decision(StoreHomeIntentDecision.DEFERRED)
        if has_question_mark or any(term in compact for term in self._QUESTION_TERMS):
            return self._decision(StoreHomeIntentDecision.QUESTION)
        if any(term in compact for term in self._OTHER_COMMAND_TERMS) or any(
            term in compact
            for term in self._COMPOUND_SEQUENCE_TERMS + self._COMPOUND_ACTION_LINKS
        ):
            return self._decision(StoreHomeIntentDecision.AMBIGUOUS_COMPOUND)
        if not has_destination or not has_target:
            return self._decision(StoreHomeIntentDecision.AMBIGUOUS)
        if not compact.endswith(self._DIRECT_ACTION_ENDINGS):
            return self._decision(StoreHomeIntentDecision.AMBIGUOUS)
        return self._decision(StoreHomeIntentDecision.STORE_HOME)

    def is_candidate(self, text: object) -> bool:
        return self.classify(text).candidate

    def _has_destination(self, normalized: str, compact: str) -> bool:
        if any(term in compact for term in self._DESTINATION_TERMS):
            return True
        return re.search(r"(?:^|\s)집(?:$|\s)", normalized) is not None

    def _decision(
        self,
        decision: StoreHomeIntentDecision,
    ) -> StoreHomeIntentClassification:
        return StoreHomeIntentClassification(decision)
