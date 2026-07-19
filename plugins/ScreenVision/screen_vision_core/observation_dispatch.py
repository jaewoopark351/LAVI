#20260705_kpopmodder: Added this helper to keep ScreenVision observation storage/dispatch outside the facade.
import time

from core.logger import log_print


class ScreenObservationDispatchHelper:
    #20260705_kpopmodder: This helper only builds/saves observation payloads; capture/model/UI behavior stays in ScreenVision.
    SCREEN_UI_NOISE_TERMS = (#20260720_kpopmodder: Mark self-observation UI noise before it reaches recall indexes.
        "obs",
        "obs studio",
        "codex",
        "vscode",
        "visual studio code",
        "gradio",
        "chatbot",
        "chatgpt_openai",
        "hybrid_openai_llm",
        "agents.md",
        "config.ini",
        "terminal",
        "powershell",
        "vtuber_source_code",
        "starcraft116_config.json",
    )

    def __init__(self, memory_store=None):
        self.memory_store = memory_store

    def update_memory_store(self, memory_store):
        self.memory_store = memory_store

    def save_observation(
        self,
        observation,
        question,
        source,
        event_type,
        error_log_message,
        silent=False,
    ):
        if self.memory_store is None:
            return

        try:
            memory_metadata = self._observation_memory_metadata(
                observation,
                source,
            )
            if not memory_metadata["is_ui_noise"]:
                self.memory_store.add_screen_observation(
                    observation=observation,
                    source=source,
                    confidence=memory_metadata["confidence"],
                )

            if hasattr(self.memory_store, "add_raw_event"):
                metadata = {
                    "question": question,
                    "remember_history": False,
                }
                metadata.update(memory_metadata["metadata"])
                if silent:
                    metadata["silent"] = True
                self.memory_store.add_raw_event(
                    event_type=event_type,
                    value=observation,
                    source=source,
                    metadata=metadata,
                )
        except Exception as e:
            log_print(f"{error_log_message}: {e}")

    def current_time(self):
        return time.time()

    def _observation_memory_metadata(self, observation, source):#20260720_kpopmodder
        text = f"{source} {observation}".lower()
        compact = self._compact_key(text)
        matched_terms = []
        for term in self.SCREEN_UI_NOISE_TERMS:
            term_key = self._compact_key(term)
            if term.lower() in text or (term_key and term_key in compact):
                matched_terms.append(term)

        is_ui_noise = bool(matched_terms)
        confidence = 0.35 if is_ui_noise else 0.95
        return {
            "is_ui_noise": is_ui_noise,
            "confidence": confidence,
            "metadata": {
                "screen_memory_quality": (
                    "ui_noise" if is_ui_noise else "normal"
                ),
                "screen_memory_confidence": confidence,
                "screen_memory_noise_terms": matched_terms[:5],
            },
        }

    def _compact_key(self, text):#20260720_kpopmodder
        return "".join(
            character
            for character in str(text or "").lower()
            if character.isalnum()
        )

    def build_llm_input(self, observation, question, source):
        return (
            f"[{source}]\n"
            f"{observation}\n\n"
            "[사용자 질문 또는 관찰 목적]\n"
            f"{question}\n\n"
            "위 화면 관찰 결과에만 근거해 대답하세요. "#20260622_kpopmodder
            "사용자가 화면에 대해 물어본 경우에만 답변한다고 가정하세요. "
            "문장을 '현재 화면에는', 'PC 화면에는', '화면에는'으로 시작하지 마세요. "
            "관찰 결과에 해당 표현이 있어도 최종 답변에서는 제거하세요. "
            "대상 이름으로 바로 시작해서 '~이 보였습니다.' 또는 '~가 보였습니다.' 같은 과거형으로 말하세요. "
            "보이지 않는 내용은 추측하지 마세요."
        )

    def build_output_payload(self, observation, question, source):
        #20260706_kpopmodder: Keep SCREEN_OBSERVATION event contract stable while adding tolerant aliases.
        memory_metadata = self._observation_memory_metadata(
            observation,
            source,
        )["metadata"]
        return {
            "kind": "screen_observation",
            "source": source,
            "observation": observation,
            "text": self.build_llm_input(observation, question, source),
            "display_text": f"[{source}] {observation}",
            "remember_history": False,
            "event_type": "screen_observation",
            "event_name": "SCREEN_OBSERVATION",
            "metadata": {
                "question": question,
                "remember_history": False,
                **memory_metadata,
            },
            "payload": {
                "kind": "screen_observation",
                "source": source,
                "observation": observation,
                "text": self.build_llm_input(observation, question, source),
                "question": question,
                "display_text": f"[{source}] {observation}",
                "remember_history": False,
                "event_type": "screen_observation",
                "event_name": "SCREEN_OBSERVATION",
            },
        }
