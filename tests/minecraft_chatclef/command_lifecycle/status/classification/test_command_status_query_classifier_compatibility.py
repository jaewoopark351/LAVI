#20260908_kpopmodder: Verify the compatibility facade adds candidates without tightening legacy family and target inputs.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle import (
    CommandStatusClassificationFailure,
    CommandStatusQuery,
    CommandStatusQueryClassifier,
    GenericCommandStatusQuestionMatcher,
)


class CommandStatusQueryClassifierCompatibilityTests(unittest.TestCase):
    def setUp(self) -> None:
        self.classifier = CommandStatusQueryClassifier()

    def test_prefixless_generic_and_family_are_lexical_candidates(self):
        self.assertEqual(
            CommandStatusQuery("any", False),
            self.classifier.classify("지금 뭐 해?"),
        )
        self.assertEqual(
            CommandStatusQuery("item_get", False),
            self.classifier.classify("뭐 만드는 중이야?"),
        )

    def test_every_generic_body_now_and_addressee_combination_maps_to_any(self):
        for body in GenericCommandStatusQuestionMatcher._BODIES:
            for now in ("", "지금 "):
                for addressee in ("", "마크 AI ", "마인크래프트 ", "마크 "):
                    text = f"{addressee}{now}{body}?"
                    with self.subTest(body=body, now=now, addressee=addressee):
                        query = self.classifier.classify(text)
                        self.assertEqual(
                            CommandStatusQuery("any", bool(addressee)),
                            query,
                        )

    def test_expanded_addressee_is_nfkc_lowercase_and_longest_match(self):
        self.assertEqual(
            CommandStatusQuery("any", True),
            self.classifier.classify("  마크 ＡＩ   지금 무슨 작업 중이야?!  "),
        )
        for text in ("마크AI 지금 뭐 해?", "마크야 지금 뭐 해?", "마크 마크 지금 뭐 해?"):
            with self.subTest(text=text):
                self.assertIsNone(self.classifier.classify(text))

    def test_generic_raw_gate_rejects_erased_or_mixed_syntax(self):
        for text in (
            "뭐, 해?",
            '"뭐 해"',
            "뭐; 철 캐줘",
            "뭐\n해",
            "뭐 해 그리고 철 10개 구해줘",
        ):
            with self.subTest(text=text):
                self.assertIsNone(self.classifier.classify(text))

    def test_generic_and_expanded_addressee_reject_nfkc_soft_punctuation(self):
        for punctuation in ("\ufe50", "\ufe52", "\ufe56"):
            for prefix in ("", "마크 AI "):
                text = f"{prefix}뭐 해{punctuation}"
                with self.subTest(
                    punctuation=f"U+{ord(punctuation):04X}",
                    prefix=prefix,
                ):
                    self.assertIsNone(self.classifier.classify(text))

    def test_legacy_family_and_target_normalization_remains_characterized(self):
        self.assertEqual(
            CommandStatusQuery("item_get", True),
            self.classifier.classify("마크 뭐, 만드는 중이야?"),
        )
        self.assertEqual(
            CommandStatusQuery("item_get", True, "다이아 곡괭이"),
            self.classifier.classify("마크 다이아 곡괭이, 만드는 중이야?"),
        )
        self.assertIsNone(self.classifier.classify("마크 AI 뭐, 만드는 중이야?"))

    def test_legacy_family_and_target_keep_their_existing_nfkc_scope(self):
        self.assertEqual(
            CommandStatusQuery("item_get", True),
            self.classifier.classify("마크 뭐﹐ 만드는 중이야?"),
        )
        self.assertEqual(
            CommandStatusQuery("item_get", True, "다이아 곡괭이"),
            self.classifier.classify(
                "마크 다이아 곡괭이﹐ 만드는 중이야?"
            ),
        )
        self.assertIsNone(
            self.classifier.classify("마크 AI 뭐﹐ 만드는 중이야?")
        )

    def test_stage_failures_are_bounded_and_distinct_from_no_match(self):
        fixtures = (
            (
                "input_validation",
                {"input_validator": SimpleNamespace(accepts=_raise_value_error)},
                "뭐 해?",
            ),
            (
                "addressee_parsing",
                {"addressee_parser": SimpleNamespace(parse=_raise_value_error)},
                "뭐 해?",
            ),
            (
                "generic_matching",
                {"generic_matcher": SimpleNamespace(matches=_raise_value_error)},
                "뭐 해?",
            ),
            (
                "family_matching",
                {
                    "generic_matcher": SimpleNamespace(matches=lambda _value: False),
                    "family_matcher": SimpleNamespace(match=_raise_value_error),
                },
                "뭐 만드는 중이야?",
            ),
            (
                "target_matching",
                {
                    "generic_matcher": SimpleNamespace(matches=lambda _value: False),
                    "family_matcher": SimpleNamespace(match=lambda _value: None),
                    "target_matcher": SimpleNamespace(match=_raise_value_error),
                },
                "철 구하는 중이야?",
            ),
        )
        for stage, collaborators, text in fixtures:
            with self.subTest(stage=stage):
                with self.assertRaises(CommandStatusClassificationFailure) as caught:
                    CommandStatusQueryClassifier(**collaborators).classify(text)
                self.assertEqual(stage, caught.exception.stage)
                self.assertEqual("ValueError", caught.exception.exception_class)
                self.assertNotIn(text, str(caught.exception))
        self.assertIsNone(self.classifier.classify("철 10개 구해줘"))

    def test_failure_exception_class_uses_exact_ascii_identifier_boundary(self):
        accepted = "E" + ("x" * 95)

        self.assertEqual(
            accepted,
            CommandStatusClassificationFailure(
                stage="generic_matching",
                exception_class=accepted,
            ).exception_class,
        )
        for rejected in ("E" + ("x" * 96), "잘못된예외", "contains.dot"):
            with self.subTest(rejected=rejected):
                self.assertEqual(
                    "invalid",
                    CommandStatusClassificationFailure(
                        stage="generic_matching",
                        exception_class=rejected,
                    ).exception_class,
                )

    def test_facade_preserves_the_first_typed_classification_failure(self):
        first = CommandStatusClassificationFailure(
            stage="generic_matching",
            exception_class="FirstMatcherFailure",
        )

        def raise_first(_value):
            raise first

        classifier = CommandStatusQueryClassifier(
            generic_matcher=SimpleNamespace(matches=raise_first)
        )
        with self.assertRaises(CommandStatusClassificationFailure) as caught:
            classifier.classify("뭐 해?")
        self.assertIs(first, caught.exception)


def _raise_value_error(_value):
    raise ValueError("raw text must never be retained")


if __name__ == "__main__":
    unittest.main()
