#20260628_kpopmodder: Added focused tests for LLM speech style prompt composition.
import unittest

from llm_core import LLM
from llm_core.speech_style import (
    apply_game_reaction_speech_style,
    build_game_reaction_system_prompt,
)


class LLMSpeechStyleTests(unittest.TestCase):
    def make_llm_without_plugins(self, speech_style_mode="polite"):
        llm = LLM.__new__(LLM)
        llm.speech_style_mode = speech_style_mode
        llm.system_prompt_text = "기본 프롬프트"
        return llm

    def test_polite_mode_appends_polite_instruction(self):
        llm = self.make_llm_without_plugins("polite")

        prompt = llm.build_effective_system_prompt("기본 프롬프트")

        self.assertIn("기본 프롬프트", prompt)
        self.assertIn("[Speech Style]", prompt)
        self.assertIn("존댓말", prompt)
        self.assertIn("캐릭터 프롬프트의 기본 말투와 예시보다 우선", prompt)
        self.assertIn("반말 종결을 쓰지 말고", prompt)

    def test_casual_mode_appends_casual_instruction(self):
        llm = self.make_llm_without_plugins("casual")

        prompt = llm.build_effective_system_prompt("기본 프롬프트")

        self.assertIn("기본 프롬프트", prompt)
        self.assertIn("[Speech Style]", prompt)
        self.assertIn("반말", prompt)
        self.assertIn("캐릭터 프롬프트의 기본 말투와 예시보다 우선", prompt)

    def test_runtime_abilities_are_appended_even_without_base_prompt(self):
        llm = self.make_llm_without_plugins("casual")

        prompt = llm.build_effective_system_prompt("")

        self.assertIn("[Runtime Abilities]", prompt)
        self.assertIn("ScreenVision", prompt)
        self.assertIn("볼 수 있다/볼 수 없다", prompt)
        self.assertIn("화면을 관찰 후 말해줄 수 있습니다", prompt)
        self.assertIn("준비된 노래만 할 수 있습니다", prompt)
        self.assertIn("대화와 ScreenVision 화면 관찰", prompt)
        self.assertIn("기억해줘", prompt)
        self.assertIn("불가능하다고 답하지", prompt)
        self.assertIn("[Speech Style]", prompt)

    def test_korean_ui_label_normalizes_to_ascii_config_value(self):
        llm = self.make_llm_without_plugins()

        self.assertEqual("casual", llm.normalize_speech_style("반말"))
        self.assertEqual("polite", llm.normalize_speech_style("존댓말"))

    def test_game_reaction_prompt_uses_selected_speech_style(self):
        polite_prompt = build_game_reaction_system_prompt("base", "polite")
        casual_prompt = build_game_reaction_system_prompt("base", "casual")

        self.assertIn("base", polite_prompt)
        self.assertIn("존댓말", polite_prompt)
        self.assertIn("반말", casual_prompt)

    def test_game_reaction_text_can_be_polished_or_relaxed(self):
        self.assertEqual(
            "제가 이번 경기를 이겼어요.",
            apply_game_reaction_speech_style("내가 이번 경기를 이겼어.", "polite"),
        )
        self.assertEqual(
            "제 드라군이 포지를 박살내고 있어요.",
            apply_game_reaction_speech_style("내 드라군이 포지를 박살내고 있어.", "polite"),
        )
        self.assertEqual(
            "내가 이번 경기를 이겼어.",
            apply_game_reaction_speech_style("내가 이번 경기를 이겼어요.", "casual"),
        )


if __name__ == "__main__":
    unittest.main()
