import unittest

from plugins.ScreenVision.screen_vision_core.observation_policy import (
    ObservationPolicy,
)


class ObservationPolicyTests(unittest.TestCase):
    def setUp(self):
        self.policy = ObservationPolicy(duplicate_similarity=0.96)

    def test_normalize_collapses_whitespace(self):
        self.assertEqual(
            "코드 편집기가 보입니다",
            self.policy.normalize("  코드   편집기가\n보입니다  "),
        )

    def test_no_important_change_phrase_is_detected(self):
        self.assertTrue(
            self.policy.is_no_important_change(
                "현재 화면에는 중요한 변화가 없습니다."
            )
        )

    def test_empty_short_and_repeated_noise_are_broken(self):
        self.assertTrue(self.policy.is_broken(""))
        self.assertTrue(self.policy.is_broken("없음"))
        self.assertTrue(self.policy.is_broken("!!!!!!!!"))
        self.assertTrue(self.policy.is_broken("가가가가가가가가가가가가"))

    def test_normal_observation_is_not_broken(self):
        self.assertFalse(
            self.policy.is_broken("VS Code에서 Python 코드가 보입니다.")
        )

    def test_duplicate_observation_ignores_whitespace(self):
        self.assertTrue(
            self.policy.is_duplicate(
                "VS Code에서 코드가 보입니다.",
                " VS   Code에서 코드가\n보입니다. ",
            )
        )

    def test_different_observation_is_not_duplicate(self):
        self.assertFalse(
            self.policy.is_duplicate(
                "VS Code에서 코드가 보입니다.",
                "웹 브라우저에서 동영상이 재생되고 있습니다.",
            )
        )


    def test_describes_broken_noise_reason(self):
        self.assertEqual(
            "empty_after_normalize",
            self.policy.describe_broken(""),
        )
        self.assertEqual(
            "no_letters_or_digits",
            self.policy.describe_broken("!!!!!!!!"),
        )

    def test_uncertain_observation_is_broken_noise(self):
        observation = "\ud655\uc2e4\ud558\uc9c0 \uc54a\uc74c."

        self.assertTrue(self.policy.is_broken(observation))
        self.assertIn(
            "uncertain_observation=",
            self.policy.describe_broken(observation),
        )

    def test_repeated_laughter_in_observation_text_is_allowed(self):
        observation = (
            "\uac8c\uc784\uc758 \ud55c \uc7a5\uba74\uc774 "
            "\ubcf4\uc785\ub2c8\ub2e4. "
            "\ud654\uba74 \uc624\ub978\ucabd \uc0c1\ub2e8\uc5d0\ub294 "
            "\ud55c\uad6d\uc5b4\ub85c '\u314b\u314b\u314b\u314b'\uc640 "
            "'\uace0\uc790\ub77c\ub2c8 \u314b\u314b'\ub77c\ub294 "
            "\ud14d\uc2a4\ud2b8\uac00 \ubcf4\uc785\ub2c8\ub2e4."
        )

        self.assertFalse(self.policy.is_broken(observation))
        self.assertEqual(
            "",
            self.policy.describe_broken(observation),
        )

    def test_many_periods_are_allowed_for_long_structured_observations(self):
        observation = " ".join(
            f"Section {index}. YouTube URL https://example.com/watch?v={index}. "
            f"Time 02:{index:02d}. Normal visible text appears."
            for index in range(16)
        )

        self.assertGreaterEqual(observation.count("."), 12)
        self.assertFalse(self.policy.is_broken(observation))
        self.assertEqual("", self.policy.describe_broken(observation))

    def test_long_observation_can_be_summarized_without_becoming_broken(self):
        observation = " ".join(
            f"Visible item {index}. This part describes a normal screen element "
            f"with enough context for later recall."
            for index in range(30)
        )

        summary = self.policy.summarize_if_long(observation)

        self.assertFalse(self.policy.is_broken(observation))
        self.assertLess(len(summary), len(self.policy.normalize(observation)))
        self.assertLessEqual(
            len(summary),
            self.policy.MAX_ACCEPTED_OBSERVATION_CHARS,
        )
        self.assertIn(
            "summarized=",
            self.policy.describe_summary(observation, summary),
        )

    def test_long_bullet_observation_deduplicates_repeated_lines(self):
        repeated_line = (
            "- main.py imports app_core/app_composer.py and calls "
            "AppComposer().run()."
        )
        observation = "\n".join(
            [
                repeated_line,
                "- if __name__ == '__main__' keeps python main.py working.",
                repeated_line,
                repeated_line,
                "- ScreenVision should keep the useful summary concise.",
            ]
            * 8
        )

        summary = self.policy.summarize_if_long(observation)

        self.assertEqual(1, summary.count("AppComposer().run()"))
        self.assertLess(len(summary), len(self.policy.normalize(observation)))

    def test_describes_duplicate_similarity(self):
        self.assertEqual(
            "normalized_exact_match",
            self.policy.describe_duplicate(
                "VS Code has Python code.",
                " VS   Code has Python code. ",
            ),
        )
        detail = self.policy.describe_duplicate(
            "VS Code has Python code.",
            "A browser video is playing.",
        )

        self.assertIn("similarity=", detail)
        self.assertIn("threshold=0.960", detail)

    def test_exit_code_in_list_or_code_block_is_broken(self):
        self.assertTrue(
            self.policy.is_broken(
                "1. code list\n- 'r_exit_code': 1",
            )
        )
        self.assertEqual(
            "exit_code_ui_snippet",
            self.policy.describe_broken(
                "1. code list\n- 'r_exit_code': 1",
            ),
        )

        self.assertTrue(
            self.policy.is_broken(
                "```\nlog_line: r_exit_code: 1\n```",
            )
        )
        self.assertEqual(
            "exit_code_ui_snippet",
            self.policy.describe_broken(
                "```\nlog_line: r_exit_code: 1\n```",
            ),
        )

    def test_exit_code_sentence_is_kept(self):
        self.assertFalse(
            self.policy.is_broken(
                "게임이 비정상적으로 종료되어 exit code가 1로 표시되었어요.",
            )
        )


if __name__ == "__main__":
    unittest.main()
