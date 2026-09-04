#20260905_kpopmodder: Route H5 candidates through one typed gate before generic Minecraft intent detection.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust.candidate import (
    AutoDepositTrustCandidateDetector,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_acquisition_verb_matcher import (
    KoreanAcquisitionVerbMatcher,
)
from plugins.Minecraft.fabric.chatclef.intent.store_home import (
    KoreanStoreHomeIntentClassifier,
)

from .contracts import MinecraftChatClefInputGateDecision


class MinecraftChatClefInputIntentGate:
    NON_ACQUISITION_TRIGGER_TERMS = (
        "이동",
        "좌표",
        "따라가",
        "따라와",
        "쫓아가",
        "멈춰",
        "중지",
        "정지",
        "스톱",
        "그만",
        "가만히",
        "대기",
        "상자에넣",
        "상자에저장",
        "상자에보관",
        "창고에넣",
        "보관함에넣",
        "입어",
        "착용",
        "장착",
        "에게",
        "한테",
        "idle",
    )

    def __init__(
        self,
        acquisition_verbs: KoreanAcquisitionVerbMatcher | None = None,
        store_home: KoreanStoreHomeIntentClassifier | None = None,
        auto_deposit_trust: AutoDepositTrustCandidateDetector | None = None,
    ):
        self._acquisition_verbs = acquisition_verbs or KoreanAcquisitionVerbMatcher()
        self._store_home = store_home or KoreanStoreHomeIntentClassifier()
        self._auto_deposit_trust = (
            auto_deposit_trust or AutoDepositTrustCandidateDetector()
        )

    def inspect(self, text: object) -> MinecraftChatClefInputGateDecision:
        if self._auto_deposit_trust.is_candidate(text):
            return MinecraftChatClefInputGateDecision.h5_auto_deposit_trust()
        normalized = self._normalize(text)
        if not normalized:
            return MinecraftChatClefInputGateDecision.none()
        if self._store_home.is_candidate(text):
            return MinecraftChatClefInputGateDecision.generic()
        if self._acquisition_verbs.matches(text):
            return MinecraftChatClefInputGateDecision.generic()
        if any(
            self._normalize(term) in normalized
            for term in self.NON_ACQUISITION_TRIGGER_TERMS
        ):
            return MinecraftChatClefInputGateDecision.generic()
        return MinecraftChatClefInputGateDecision.none()

    def should_consider(self, text: object) -> bool:
        return self.inspect(text).consider

    def _normalize(self, text: object) -> str:
        return re.sub(r"\s+", "", str(text or "").strip().lower())
