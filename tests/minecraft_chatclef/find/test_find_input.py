#20260914_kpopmodder: Verify deterministic FIND parity, scope and refusal before generic extraction.
import unittest
from dataclasses import replace
from unittest.mock import Mock

from plugins.Minecraft.fabric.chatclef.intent import ChatClefNaturalLanguageService
from plugins.Minecraft.fabric.chatclef.intent.find import FindInputParser
from plugins.Minecraft.fabric.chatclef.input.gating import MinecraftChatClefInputIntentGate
from plugins.Minecraft.fabric.chatclef.command_registry import KoreanChatClefCommandRegistry
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import CommandFeedbackDescriptorFactory
from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import ChatClefTranslationResultDTO

from .fixtures import descriptor, snapshot, translation
from plugins.Minecraft.fabric.chatclef.transport.find_catalog import FindCatalogSnapshot
from plugins.Minecraft.fabric.chatclef.transport.find_catalog.find_catalog_snapshot import catalog_digest


class FindInputTests(unittest.TestCase):
    def test_default_report_and_explicit_approach_compile_identically_for_chat_and_final_voice(self):
        for text, expected in (("마을 주민 찾아줘", "find entity minecraft:villager report"),
                ("마을 주민 찾아서 가까이 가줘", "find entity minecraft:villager approach"),
                ("상자 블록 찾아줘", "find block minecraft:chest report"),
                ("떨어진 다이아몬드 찾아줘", "find item minecraft:diamond report"),
                ("하늘 짐승 찾아줘", "find entity example:sky_beast report"),
                ("플레이어 Steve 찾아줘", "find player Steve report")):
            for source in ("lavi_chat_ui", "voice_input_final"):
                with self.subTest(text=text, source=source):
                    self.assertEqual(expected, translation(text)["command"])
                    actual = descriptor(source, text)
                    self.assertEqual(expected, actual.command)
                    self.assertEqual("finite_task", actual.response_lifecycle_kind)
                    self.assertIsNotNone(actual.find_binding)

    def test_every_catalog_kind_id_roundtrips_without_a_handwritten_support_list(self):
        service = ChatClefNaturalLanguageService(find_catalog_provider=snapshot)
        for record in snapshot().records:
            result = service.translate(f"find {record.target_kind} {record.canonical_target_id}")
            if record.eligibility == "non_mob":
                self.assertFalse(result.executable)
            else:
                self.assertTrue(result.executable)
                self.assertEqual(record.canonical_target_id, result.resolved_target)

    def test_ambiguity_item_approach_and_other_query_scopes_never_fall_back_to_get_or_llm(self):
        extractor = Mock(side_effect=AssertionError("FIND must not reach generic extraction"))
        service = ChatClefNaturalLanguageService(extractor=extractor, find_catalog_provider=snapshot)
        for text, reason in (("상자 찾아줘", "find_target_ambiguous"),
                ("떨어진 다이아몬드 찾아서 가까이 가줘", "item_find_approach_unsupported"),
                ("인벤토리 다이아몬드 찾아줘", "unsupported_find_query"),
                ("상자 안 다이아몬드 찾아줘", "unsupported_find_query"),
                ("다이아몬드 얻는 장소 찾아줘", "unsupported_find_query"),
                ("알 수 없는 몹 찾아줘", "find_target_unresolved"),
                ("좀비 찾아줘 그리고 공격해", "invalid_find_grammar")):
            with self.subTest(text=text):
                result = service.translate(text)
                self.assertFalse(result.executable)
                self.assertIsNone(result.command)
                self.assertEqual(reason, result.reason_code)
        extractor.extract.assert_not_called()

    def test_missing_catalog_does_not_block_literal_player_or_enable_registry_fallback(self):
        service = ChatClefNaturalLanguageService()
        self.assertFalse(service.translate("좀비 찾아줘").executable)
        self.assertEqual("find player Steve report", service.translate("플레이어 Steve 찾아줘").command)

    def test_private_use_unassigned_and_non_bmp_mod_labels_resolve_through_the_shared_vocabulary(self):
        for label in ("\ue000 하늘 짐승", "\u0378 하늘 짐승", "\U0001f600 하늘 짐승"):
            original = snapshot()
            current = tuple(replace(record, korean_name=label) if record.canonical_target_id == "example:sky_beast" else record
                for record in original.records)
            catalog = FindCatalogSnapshot(original.session_id, original.connection_generation,
                original.resource_generation, catalog_digest(current), current)
            service = ChatClefNaturalLanguageService(find_catalog_provider=lambda: catalog)
            result = service.translate(label + " 찾아줘")
            self.assertTrue(result.executable)
            self.assertEqual("find entity example:sky_beast report", result.command)
        for marker in ("\x00", "\u200d", "\ud800", "\u2028", "\u2029"):
            self.assertEqual("invalid_find_target", FindInputParser().parse("하늘" + marker + "짐승 찾아줘").reason)

    def test_interim_and_untrusted_descriptor_sources_are_refused(self):
        from types import SimpleNamespace
        for source, final in (("voice_input_final", False), ("untrusted", True), ("lavi_gui_korean", True)):
            actual = CommandFeedbackDescriptorFactory().from_trusted_translation(event=SimpleNamespace(
                source=source, final=final, event_id="a" * 32, provider_id=source, event_kind="final_transcript"),
                translation=translation())
            self.assertIsNone(actual)

    def test_compilation_rejects_changed_kind_id_or_mode(self):
        value = translation()
        for changed in ("find block minecraft:villager report", "find entity minecraft:zombie report", "find entity minecraft:villager approach"):
            with self.assertRaises(ValueError):
                ChatClefTranslationResultDTO.from_mapping({**value, "command": changed})

    def test_gate_claims_find_but_discussions_remain_unclaimed(self):
        gate = MinecraftChatClefInputIntentGate()
        for text in ("좀비 찾아줘", "마을 주민 위치만 알려줘", "상자 블록 찾아서 가까이 가줘"):
            self.assertTrue(gate.should_consider(text))
        self.assertFalse(FindInputParser().is_candidate("좀비를 찾는 방법이 궁금해"))

    def test_registry_has_one_finite_r2_find_with_existing_source_policy(self):
        registry = KoreanChatClefCommandRegistry()
        self.assertEqual(1, registry.command_names().count("find"))
        spec = registry.spec("find")
        self.assertEqual("R2", spec.safety_tier)
        self.assertEqual("none", spec.confirmation_mode)
        self.assertEqual(("lavi_chat_ui", "voice_input_final", "direct_typed"), spec.allowed_input_sources)
