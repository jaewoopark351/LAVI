#20260905_kpopmodder: Lock resolver-family rejection ownership without acquisition-verb overreach.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.input.ownership.item_command import (
    ItemCommandOwnership,
    ItemCommandOwnershipClassifier,
    ItemCommandTranslationRejectionEvidenceParser,
)
from plugins.Minecraft.fabric.chatclef.intent import ChatClefNaturalLanguageService


class ItemCommandTranslationRejectionOwnershipTests(unittest.TestCase):
    def setUp(self):
        self.parser = ItemCommandTranslationRejectionEvidenceParser()
        self.classifier = ItemCommandOwnershipClassifier()

    def test_recognized_equipment_family_is_owned_invalid_only_in_live_scope(self):
        text = "루비 검 가져와줘"
        translation = _rejection(
            text=text,
            item_phrase="루비 검",
            status="unknown",
            reason_code="unknown_item_phrase",
        )

        live = self.classifier.classify_translation_rejection(
            self.parser.parse(
                command_text=text,
                translation=translation,
                trusted_scope_live=True,
            )
        )
        direct = self.classifier.classify_translation_rejection(
            self.parser.parse(
                command_text=text,
                translation=translation,
                trusted_scope_live=False,
            )
        )

        self.assertEqual(ItemCommandOwnership.OWNED_INVALID, live.ownership)
        self.assertEqual(ItemCommandOwnership.UNRELATED, direct.ownership)

    def test_acquisition_verb_without_resolver_family_is_unrelated(self):
        text = "루비 가져와줘"
        decision = self.classifier.classify_translation_rejection(
            self.parser.parse(
                command_text=text,
                translation=_rejection(
                    text=text,
                    item_phrase="루비",
                    status="unknown",
                    reason_code="unknown_item_phrase",
                ),
                trusted_scope_live=True,
            )
        )

        self.assertEqual(ItemCommandOwnership.UNRELATED, decision.ownership)

    def test_explicit_leading_minecraft_marker_owns_unknown_item_rejection(self):
        for text in (
            "마크 루비 가져와줘",
            "마인크래프트 루비 만들어줘!",
            "마크 AI 루비 가져와줘",
        ):
            with self.subTest(text=text):
                normalized = (
                    "마인크래프트 루비 만들어줘"
                    if text.endswith("!")
                    else text
                )
                decision = self.classifier.classify_translation_rejection(
                    self.parser.parse(
                        command_text=text,
                        translation=_rejection(
                            text=normalized,
                            item_phrase=(
                                "마인크래프트 루비"
                                if text.startswith("마인크래프트")
                                else "마크 ai 루비"
                                if "AI" in text
                                else "마크 루비"
                            ),
                            status="unknown",
                            reason_code="unknown_item_phrase",
                        ),
                        trusted_scope_live=True,
                    )
                )

                self.assertEqual(
                    ItemCommandOwnership.OWNED_INVALID,
                    decision.ownership,
                )

    def test_marker_must_be_an_exact_leading_token(self):
        text = "마크업 루비 만들어줘"
        decision = self.classifier.classify_translation_rejection(
            self.parser.parse(
                command_text=text,
                translation=_rejection(
                    text=text,
                    item_phrase="마크업 루비",
                    status="unknown",
                    reason_code="unknown_item_phrase",
                ),
                trusted_scope_live=True,
            )
        )

        self.assertEqual(ItemCommandOwnership.UNRELATED, decision.ownership)

    def test_actual_rule_translation_preserves_explicit_marker_ownership(self):
        service = ChatClefNaturalLanguageService()
        text = "마인크래프트 루비 만들어줘!"

        translation = service.translate(text).to_dict()
        decision = self.classifier.classify_translation_rejection(
            self.parser.parse(
                command_text=text,
                translation=translation,
                trusted_scope_live=True,
            )
        )

        self.assertEqual("unknown", translation["status"])
        self.assertEqual(ItemCommandOwnership.OWNED_INVALID, decision.ownership)

    def test_unbound_or_inconsistent_translation_evidence_is_unrelated(self):
        text = "구리 검 하나 만들어"
        translation = _rejection(
            text="다른 입력",
            item_phrase="구리 검",
            status="unsupported",
            reason_code="unsupported_material",
        )

        decision = self.classifier.classify_translation_rejection(
            self.parser.parse(
                command_text=text,
                translation=translation,
                trusted_scope_live=True,
            )
        )

        self.assertEqual(ItemCommandOwnership.UNRELATED, decision.ownership)


def _rejection(
    *,
    text: str,
    item_phrase: str,
    status: str,
    reason_code: str,
) -> dict[str, object]:
    return {
        "status": status,
        "executable": False,
        "command": None,
        "intent": {
            "intent_type": "get_item",
            "quantity": 1,
            "item_phrase": item_phrase,
            "original_text": text,
            "source": "rule",
            "language": "ko",
        },
        "resolved_target": None,
        "reason_code": reason_code,
        "message": "rejected",
        "data": {
            "resolution": {
                "status": status,
                "target": None,
                "reason_code": reason_code,
                "data": {},
            }
        },
    }


if __name__ == "__main__":
    unittest.main()
