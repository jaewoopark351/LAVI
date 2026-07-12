#20260621_kpopmodder: ScreenVision 관찰을 일반 대화 history와 분리하고 사용자 정정을 보정한다.
import re
import threading
import time
from dataclasses import dataclass


@dataclass
class NormalizedInput:#20260621_kpopmodder
    text: str
    display_text: str
    remember_history: bool = True
    kind: str = "user"
    source: str = ""
    observation: str = ""


class LLMInteractionContext:#20260621_kpopmodder
    """LLM에 들어오는 입력의 성격을 구분한다.

    - 일반 사용자 대화: history에 저장한다.
    - ScreenVision 관찰 이벤트: LLM/TTS에는 보낼 수 있지만 history에는 저장하지 않는다.
    - 사용자 정정/불평: 최근 ScreenVision 관찰을 참고해 회피 답변을 줄인다.
    """

    SCREEN_KIND = "screen_observation"

    CORRECTION_PATTERNS = (
        "아니",
        "틀렸",
        "틀린",
        "잘못",
        "그게 아니라",
        "아닌데",
        "뭔 소리",
        "왜 몰라",
        "모르겠습니다",
        "도망",
        "정정",
        "불평",
        "아님",
        "아니야",
        "그거 말고",
    )

    SCREEN_QUESTION_PATTERNS = (#20260622_kpopmodder
        "화면",
        "스크린",
        "보이는",
        "보였",
        "봤어",
        "봤니",
        "뭐 보여",
        "뭐 보였",
        "뭐 있었",
        "방금 뭐",
        "아까 뭐",
        "지금 뭐",
        "창",
        "오류",
        "에러",
        "자막",
        "버튼",
        "메뉴",
        "게임 화면",
        "screen",
        "window",
        "error",
    )

    LEGACY_SCREEN_PREFIXES = (
        "[자동 화면 변화 감지]",
        "[자동 최신 화면 관찰]",
        "[단축키 최신 화면 관찰]",
        "[수동 화면 관찰]",
        "[ScreenVision]",
    )

    def __init__(self, max_observations=3):
        self.max_observations = max(1, int(max_observations))
        self._lock = threading.Lock()
        self._screen_observations = []

    def normalize_input(self, message):
        if isinstance(message, dict):
            kind = str(message.get("kind") or message.get("type") or "user")
            text = str(message.get("text") or "").strip()
            display_text = str(message.get("display_text") or text).strip()
            remember_history = bool(message.get("remember_history", kind != self.SCREEN_KIND))
            source = str(message.get("source") or "").strip()
            observation = str(message.get("observation") or "").strip()

            normalized = NormalizedInput(
                text=text,
                display_text=display_text or text,
                remember_history=remember_history,
                kind=kind,
                source=source,
                observation=observation,
            )

            if kind == self.SCREEN_KIND and observation:
                self.add_screen_observation(observation=observation, source=source)

            return normalized

        text = str(message or "").strip()
        return NormalizedInput(
            text=text,
            display_text=text,
            remember_history=True,
            kind="user",
        )

    # def add_screen_observation(self, observation, source=""):#20260622_kpopmodder
    #     observation = str(observation or "").strip()
    #     if not observation:
    #         return
    #     with self._lock:
    #         self._screen_observations.append(
    #             {
    #                 "time": time.time(),
    #                 "source": str(source or "").strip(),
    #                 "observation": observation,
    #             }
    #         )
    #         self._screen_observations = self._screen_observations[-self.max_observations:]

    def add_screen_observation(self, observation, source=""):#20260622_kpopmodder
        observation = str(observation or "").strip()
        if not observation:
            return

        with self._lock:
            if self._screen_observations:
                latest = self._screen_observations[-1]
                if latest.get("observation") == observation:
                    latest["time"] = time.time()
                    latest["source"] = str(source or "").strip()
                    return

            self._screen_observations.append(
                {
                    "time": time.time(),
                    "source": str(source or "").strip(),
                    "observation": observation,
                }
            )
            self._screen_observations = (
                self._screen_observations[-self.max_observations:]
            )

    def latest_screen_observation(self):
        with self._lock:
            if not self._screen_observations:
                return ""
            return self._screen_observations[-1].get("observation", "")

    def is_correction_or_complaint(self, text):
        compact = re.sub(r"\s+", "", str(text or "").strip().lower())
        if not compact:
            return False
        return any(pattern.replace(" ", "") in compact for pattern in self.CORRECTION_PATTERNS)

    def is_screen_question(self, text):#20260622_kpopmodder
        compact = re.sub(r"\s+", "", str(text or "").strip().lower())
        if not compact:
            return False

        return any(
            pattern.replace(" ", "").lower() in compact
            for pattern in self.SCREEN_QUESTION_PATTERNS
        )

    def build_screen_question_input(self, user_text, latest_observation):#20260622_kpopmodder
        return (
            "[사용자 화면 질문]\n"
            f"{user_text}\n\n"
            "[최근 화면 관찰 기록]\n"
            f"{latest_observation}\n\n"
            "응답 규칙:\n"
            "- 사용자가 화면에 대해 물어봤으므로 최근 화면 관찰 기록만 참고해 답한다.\n"
            "- 화면 관찰 기록에 없는 내용은 추측하지 않는다.\n"
            "- 자동으로 방금 본 것처럼 먼저 말하지 말고, 사용자가 물어봤기 때문에 답하는 방식으로 말한다.\n"
            "- '현재 화면에는', 'PC 화면에는', '화면에는'으로 문장을 시작하지 않는다.\n"
            "- 가능하면 '~이 보였습니다', '~가 보였습니다', '~라고 표시되어 있었습니다' 같은 과거형으로 답한다.\n"
            "- 너무 길게 늘어놓지 말고 핵심부터 말한다."
        )

    # def build_model_input(self, normalized_input):#20260622_kpopmodder
    #     """LLM 플러그인에 실제로 전달할 message를 만든다.

    #     사용자의 정정/불평은 최근 화면 관찰과 함께 전달하지만,
    #     history에는 사용자가 실제로 말한 문장만 저장한다.
    #     """
    #     if normalized_input.kind == self.SCREEN_KIND:
    #         return normalized_input.text

    #     text = normalized_input.text
    #     latest_observation = self.latest_screen_observation()
    #     if latest_observation and self.is_correction_or_complaint(text):
    #         return (
    #             "[사용자 정정/불평]\n"
    #             f"{text}\n\n"
    #             "[최근 화면 관찰]\n"
    #             f"{latest_observation}\n\n"
    #             "응답 규칙:\n"
    #             "- 사용자의 말은 방금 화면 설명에 대한 정정/불평일 수 있다.\n"
    #             "- '모르겠습니다'로 회피하지 말고, 틀렸을 가능성을 인정하고 짧게 다시 답한다.\n"
    #             "- 화면에 확실히 보이는 정보와 사용자의 정정만 기준으로 말한다.\n"
    #             "- 한두 문장으로 자연스럽게 답한다."
    #         )
    #     return text

    def build_model_input(
        self,
        normalized_input,
        screen_question_decision=None,
    ):#20260622_kpopmodder
        """LLM 플러그인에 실제로 전달할 message를 만든다.

        사용자의 화면 질문/정정/불평은 최근 화면 관찰과 함께 전달하지만,
        history에는 사용자가 실제로 말한 문장만 저장한다.
        """
        if normalized_input.kind == self.SCREEN_KIND:
            return normalized_input.text

        text = normalized_input.text
        latest_observation = self.latest_screen_observation()

        if latest_observation and screen_question_decision is not None:#20260628_kpopmodder: Prefer request-scoped ScreenQuestionRouter decisions over broad keyword triggers.
            if getattr(screen_question_decision, "need_screen", False):
                return self.build_screen_question_input(
                    user_text=text,
                    latest_observation=latest_observation,
                )

        if (
            latest_observation
            and screen_question_decision is None
            and self.is_screen_question(text)
        ):#20260622_kpopmodder
            return self.build_screen_question_input(
                user_text=text,
                latest_observation=latest_observation,
            )

        if latest_observation and self.is_correction_or_complaint(text):
            return (
                "[사용자 정정/불평]\n"
                f"{text}\n\n"
                "[최근 화면 관찰]\n"
                f"{latest_observation}\n\n"
                "응답 규칙:\n"
                "- 사용자의 말은 방금 화면 설명에 대한 정정/불평일 수 있다.\n"
                "- '모르겠습니다'로 회피하지 말고, 틀렸을 가능성을 인정하고 짧게 다시 답한다.\n"
                "- 화면에 확실히 보이는 정보와 사용자의 정정만 기준으로 말한다.\n"
                "- 한두 문장으로 자연스럽게 답한다."
            )

        return text

    def filter_history_for_model(self, history):
        """과거에 이미 history에 들어간 ScreenVision 이벤트도 모델 입력에서 제거한다."""
        result = []
        for entry in history or []:
            try:
                user, ai = entry
            except Exception:
                continue

            if isinstance(user, dict):
                kind = str(user.get("kind") or user.get("type") or "")
                if kind == self.SCREEN_KIND:
                    continue
                user = user.get("display_text") or user.get("text") or ""

            user_text = str(user or "").strip()
            if self._looks_like_legacy_screen_event(user_text):
                continue

            result.append([user_text, str(ai or "")])
        return result

    def _looks_like_legacy_screen_event(self, text):
        text = str(text or "").lstrip()
        return any(text.startswith(prefix) for prefix in self.LEGACY_SCREEN_PREFIXES)
