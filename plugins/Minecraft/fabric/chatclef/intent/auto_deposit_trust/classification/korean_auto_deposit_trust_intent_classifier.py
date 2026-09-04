#20260905_kpopmodder: Classify one H5 candidate with raw guards before normalization.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust.candidate import (
    AutoDepositTrustCandidateDetector,
)
from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust.contracts import (
    AutoDepositTrustIntentClassification,
    AutoDepositTrustIntentDecision,
)
from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust.normalization import (
    AutoDepositTrustInputNormalizer,
)


class KoreanAutoDepositTrustIntentClassifier:
    _EXACT_FORMS = frozenset(
        {
            "auto_deposit_trust area 16x16",
            "auto_deposit_trust 반경 16x16",
            "자동보관등록 영역 16x16",
            "자동보관등록 반경 16x16",
        }
    )
    _NEGATION_TERMS = (
        "등록하지마",
        "등록하지말",
        "등록하지않",
        "등록안해",
        "등록하지마세요",
        "등록금지",
        "등록해보지마",
        "등록해주지마",
        "등록해주지는마",
        "등록해주진마",
        "등록해주지않",
        "등록하지는마",
        "등록하지마라",
        "등록하면안돼",
        "등록하면안되",
        "등록해주면안돼",
        "등록해주면안되",
        "등록해놓지마",
        "등록해놓으면안돼",
        "등록해놓으면안되",
        "등록해달라는게아니야",
        "등록해줘서는안돼",
        "등록해줘서는안되",
        "등록해선안돼",
        "등록해선안되",
        "등록해서는안돼",
        "등록해서는안되",
        "하지마",
        "하지말",
    )
    _QUESTION_TERMS = (
        "등록할까",
        "등록하면",
        "등록해도돼",
        "등록해도되",
        "등록해도",
        "등록하나요",
        "등록하니",
        "등록해야해",
        "등록해야하나",
        "등록해야할까",
        "등록해도괜찮",
        "등록하면괜찮",
        "등록해볼까",
        "등록해보는건어때",
        "등록해줘야할까",
        "가능하면",
        "되나",
        "될까",
    )
    _DEFERRED_TERMS = (
        "나중에",
        "나중으로",
        "내일",
        "모레",
        "이따",
        "잠시후",
        "조금있다가",
        "좀있다가",
        "조금후",
        "조금뒤에",
        "잠시뒤",
        "잠깐뒤",
        "잠깐있다가",
        "이따가",
        "준비되면",
        "다음에",
        "다음번에",
        "다음주",
        "후에",
    )
    _COMPOUND_SEQUENCE_TERMS = (
        "그리고",
        "그다음",
        "다음으로",
        "그후",
        "한뒤",
        "한다음",
    )
    _OTHER_COMMAND_TERMS = (
        "캐와",
        "캐줘",
        "채굴",
        "구해",
        "가져와",
        "만들어",
        "제작",
        "따라가",
        "팔로우",
        "쫓아가",
        "이동",
        "공격",
        "입어",
        "착용",
        "장착",
        "전달",
        "멈춰",
        "중지",
        "정지",
    )
    _RESTRICTIVE_OPTION_TERMS = (
        "상자만",
        "컨테이너만",
        "배럴만",
        "하나만",
        "한개만",
        "절반",
        "반만",
        "일부",
        "몇개만",
        "몇몇",
        "것만",
        "특정",
        "대신",
        "말고",
        "빼고",
        "제외",
    )
    _OPTION_MARKER_RE = re.compile(r"--|[=:/\\\[\]{}]")
    _PURPOSE_TERMS = (
        "자동보관",
        "자동입고",
        "보관대상",
        "입고대상",
        "trustedstorage",
    )
    _AREA_TERMS = ("주변", "영역", "범위", "반경", "현재위치", "캐릭터")
    _EXPLICIT_CURRENT_POSITION_AREA_TERMS = ("현재위치기준",)
    _TARGET_TERMS = ("상자", "컨테이너", "보관함", "chest", "container")
    _SIZE_LIKE_RE = re.compile(r"(?<!\d)\d+\s*(?:x|×|곱하기)\s*\d+(?!\d)", re.IGNORECASE)
    _CONTROL_RE = re.compile(r"[\r\n\x00-\x08\x0b\x0c\x0e-\x1f\x7f]")
    _NATURAL_EXECUTION_RE = re.compile(
        r"^(?:"
        r"(?:(?:캐릭터(?:위치)?|현재위치)(?:를)?기준(?:으로)?)(?:"
        r"(?:주변|반경|영역|범위)16x16"
        r"(?:(?:범위|영역|반경)(?:에있는|안에있는|내의|의)?|(?:에|안에)있는)?"
        r"|16x16(?:(?:범위|영역|반경)(?:에있는|안에있는|내의|의)?|(?:에|안에)있는)"
        r")"
        r"|캐릭터(?:주변|반경|영역|범위)16x16"
        r"(?:(?:범위|영역|반경)(?:에있는|안에있는|내의|의)?|(?:에|안에)있는)?"
        r"|(?:주변|반경|영역|범위)16x16"
        r"(?:(?:범위|영역|반경)(?:에있는|안에있는|내의|의)?|(?:에|안에)있는)?"
        r")"
        r"(?:모든|전부)?"
        r"(?:상자|컨테이너|보관함)(?:들)?(?:을|를|의)?"
        r"(?:전부|모두)?"
        r"(?:자동보관(?:대상)?|자동입고(?:대상)?|보관대상|입고대상)(?:으로)?"
        r"등록해(?:줘|줘요|주세요|주십시오)?(?:[,!.，。！]+)?$"
    )
    _CURRENT_POSITION_EXECUTION_RE = re.compile(
        r"^현재위치(?:를)?기준(?:으로)?"
        r"(?:"
        r"(?:주변|반경|영역|범위)16x16"
        r"(?:(?:범위|영역|반경)(?:에있는|안에있는|내의|의)?|(?:에|안에)있는)?"
        r"|16x16(?:(?:범위|영역|반경)(?:에있는|안에있는|내의|의)?|(?:에|안에)있는)"
        r")"
        r"(?:모든|전부)?"
        r"(?:상자|컨테이너|보관함)(?:들)?(?:을|를|의)?"
        r"(?:전부|모두)?"
        r"등록해(?:줘|줘요|주세요|주십시오)?(?:[,!.，。！]+)?$"
    )

    def __init__(
        self,
        normalizer: AutoDepositTrustInputNormalizer | None = None,
        candidate_detector: AutoDepositTrustCandidateDetector | None = None,
    ):
        self._normalizer = normalizer or AutoDepositTrustInputNormalizer()
        self._candidate_detector = candidate_detector or AutoDepositTrustCandidateDetector(
            self._normalizer
        )

    def classify(self, text: object) -> AutoDepositTrustIntentClassification:
        if not self._candidate_detector.is_candidate(text):
            return self._decision(AutoDepositTrustIntentDecision.NO_MATCH)

        raw = str(text or "")
        raw_compact = re.sub(r" +", "", raw).lower()
        if self._CONTROL_RE.search(raw):
            return self._decision(AutoDepositTrustIntentDecision.MALFORMED)
        if "@" in raw:
            return self._decision(AutoDepositTrustIntentDecision.MALFORMED)
        if any(term in raw_compact for term in self._NEGATION_TERMS):
            return self._decision(AutoDepositTrustIntentDecision.NEGATED)
        if re.search(r"[?？]", raw) or any(
            term in raw_compact for term in self._QUESTION_TERMS
        ):
            return self._decision(AutoDepositTrustIntentDecision.QUESTION)
        if any(term in raw_compact for term in self._DEFERRED_TERMS):
            return self._decision(AutoDepositTrustIntentDecision.DEFERRED)
        if any(
            term in raw_compact
            for term in self._RESTRICTIVE_OPTION_TERMS
        ) or self._OPTION_MARKER_RE.search(raw):
            return self._decision(AutoDepositTrustIntentDecision.MALFORMED)
        if self._is_compound(raw_compact):
            return self._decision(AutoDepositTrustIntentDecision.AMBIGUOUS_COMPOUND)

        normalized = self._normalizer.normalize(raw)
        compact = re.sub(r" +", "", normalized)
        if normalized in self._EXACT_FORMS:
            return self._decision(
                AutoDepositTrustIntentDecision.AUTO_DEPOSIT_TRUST_AREA
            )

        fixed_size_count = normalized.count("16x16")
        size_like_tokens = self._SIZE_LIKE_RE.findall(normalized)
        if fixed_size_count > 1:
            return self._decision(AutoDepositTrustIntentDecision.MALFORMED)
        if size_like_tokens and fixed_size_count == 0:
            return self._decision(AutoDepositTrustIntentDecision.UNSUPPORTED_SIZE)
        if fixed_size_count == 1:
            without_size = normalized.replace("16x16", " ", 1)
            if re.search(r"\d", without_size):
                return self._decision(AutoDepositTrustIntentDecision.MALFORMED)

        if normalized.startswith(
            ("auto_deposit_trust ", "@auto_deposit_trust ", "자동보관등록 ")
        ):
            if fixed_size_count == 0:
                return self._decision(AutoDepositTrustIntentDecision.UNSUPPORTED_SIZE)
            return self._decision(AutoDepositTrustIntentDecision.MALFORMED)

        if fixed_size_count == 0:
            has_purpose = any(term in compact for term in self._PURPOSE_TERMS)
            has_explicit_current_position_area = any(
                term in compact
                for term in self._EXPLICIT_CURRENT_POSITION_AREA_TERMS
            )
            has_area = any(term in compact for term in self._AREA_TERMS)
            has_target = any(term in compact for term in self._TARGET_TERMS)
            has_imperative = "등록해" in compact
            if (
                (has_purpose or has_explicit_current_position_area)
                and has_area
                and has_target
                and has_imperative
            ):
                return self._decision(
                    AutoDepositTrustIntentDecision.UNSUPPORTED_SIZE
                )
            return self._decision(AutoDepositTrustIntentDecision.AMBIGUOUS)
        if not (
            self._NATURAL_EXECUTION_RE.fullmatch(compact)
            or self._CURRENT_POSITION_EXECUTION_RE.fullmatch(compact)
        ):
            return self._decision(AutoDepositTrustIntentDecision.AMBIGUOUS)
        return self._decision(
            AutoDepositTrustIntentDecision.AUTO_DEPOSIT_TRUST_AREA
        )

    def _is_compound(self, compact: str) -> bool:
        if any(term in compact for term in self._COMPOUND_SEQUENCE_TERMS):
            return True
        return any(term in compact for term in self._OTHER_COMMAND_TERMS)

    def _decision(
        self,
        decision: AutoDepositTrustIntentDecision,
    ) -> AutoDepositTrustIntentClassification:
        return AutoDepositTrustIntentClassification(decision)
