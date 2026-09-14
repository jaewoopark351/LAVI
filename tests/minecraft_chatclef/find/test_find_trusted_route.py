#20260914_kpopmodder: Exercise actual trusted Chat/final-ASR dispatch and existing busy/STOP admission.
import unittest
from unittest.mock import patch

from plugins.Minecraft.fabric.chatclef.extension import MinecraftFabricChatClefExtension
from plugins.Minecraft.fabric.chatclef.intent import ChatClefNaturalLanguageService
from plugins.Minecraft.fabric.chatclef.input.routing.find import FindTranslationBindingStage

from ..lavi_input.test_trusted_korean_chat_voice_integration import (
    _RecordingMinecraftAdapter, _ActiveStatusLifecycleAdapter, _llm_harness, _dispatch_goto_input,
)
from .fixtures import snapshot, translation
from plugins.Minecraft.fabric.chatclef.transport.find_catalog import FindCatalogRecord, FindCatalogSnapshot
from plugins.Minecraft.fabric.chatclef.transport.find_catalog.find_catalog_snapshot import catalog_digest


class FindTrustedRouteTests(unittest.TestCase):
    def test_actual_chat_and_final_voice_submit_once_without_llm_and_preserve_catalog_metadata(self):
        for voice in (False, True):
            for text, command in (("마을 주민 찾아줘", "find entity minecraft:villager report"),
                    ("마을 주민 찾아서 가까이 가줘", "find entity minecraft:villager approach"),
                    ("플레이어 Steve 찾아줘", "find player Steve report")):
                with self.subTest(voice=voice, text=text):
                    adapter = _RecordingMinecraftAdapter()
                    adapter.get_find_catalog_snapshot = snapshot
                    service = ChatClefNaturalLanguageService(find_catalog_provider=snapshot)
                    extension = MinecraftFabricChatClefExtension(adapter=adapter, natural_language_service=service)
                    llm, pipeline, outputs = _llm_harness(extension)
                    _dispatch_goto_input(llm, text, voice=voice)
                    self.assertEqual(0, pipeline.provider_calls)
                    self.assertEqual(1, len(adapter.requests))
                    request = adapter.requests[0]
                    self.assertEqual(command, request.command)
                    self.assertEqual("voice_input_final" if voice else "lavi_chat_ui", request.source)
                    self.assertTrue(request.metadata["input_event"]["final"])
                    if "player" not in command:
                        self.assertEqual(snapshot().catalog_digest, request.metadata["natural_language"]["translation"]["data"]["find_catalog_digest"])

    def test_ambiguity_and_item_approach_publish_bounded_clarification_without_any_command_or_llm(self):
        for voice in (False, True):
            for text in ("상자 찾아줘", "떨어진 다이아몬드 찾아서 가까이 가줘", "상자 안 다이아몬드 찾아줘"):
                adapter = _RecordingMinecraftAdapter()
                adapter.get_find_catalog_snapshot = snapshot
                extension = MinecraftFabricChatClefExtension(adapter=adapter,
                    natural_language_service=ChatClefNaturalLanguageService(find_catalog_provider=snapshot))
                llm, pipeline, outputs = _llm_harness(extension)
                yielded, _ = _dispatch_goto_input(llm, text, voice=voice)
                self.assertEqual([], adapter.requests)
                self.assertEqual(0, pipeline.provider_calls)
                self.assertEqual(1, len(yielded))
                self.assertEqual(1, len(outputs))

    def test_existing_busy_owner_is_preserved_and_find_submits_zero_commands(self):
        for voice in (False, True):
            adapter = _ActiveStatusLifecycleAdapter()
            adapter.get_find_catalog_snapshot = snapshot
            extension = MinecraftFabricChatClefExtension(adapter=adapter,
                natural_language_service=ChatClefNaturalLanguageService(find_catalog_provider=snapshot))
            llm, pipeline, _ = _llm_harness(extension)
            original = adapter.current_active_identity()
            yielded, _ = _dispatch_goto_input(llm, "마을 주민 찾아줘", voice=voice)
            self.assertEqual([], adapter.command_requests)
            self.assertEqual(original, adapter.current_active_identity())
            self.assertEqual(0, pipeline.provider_calls)

    def test_empty_air_item_is_refused_before_admission_without_llm_get_or_task_submission(self):
        original = snapshot()
        records = (*original.records, FindCatalogRecord("item", "minecraft:air", "block.minecraft.air", "공기", "Air"))
        catalog = FindCatalogSnapshot(original.session_id, original.connection_generation,
            original.resource_generation, catalog_digest(records), records)
        for voice in (False, True):
            for text in ("아이템 공기 찾아줘", "아이템 minecraft:air 찾아줘"):
                adapter = _RecordingMinecraftAdapter()
                adapter.get_find_catalog_snapshot = lambda: catalog
                service = ChatClefNaturalLanguageService(find_catalog_provider=lambda: catalog)
                self.assertEqual("invalid_find_target", service.translate(text).reason_code)
                extension = MinecraftFabricChatClefExtension(adapter=adapter, natural_language_service=service)
                llm, pipeline, outputs = _llm_harness(extension)
                yielded, _ = _dispatch_goto_input(llm, text, voice=voice)
                self.assertEqual([], adapter.requests)
                self.assertEqual(0, pipeline.provider_calls)
                self.assertEqual(1, len(yielded))
                self.assertEqual(1, len(outputs))
        from plugins.Minecraft.fabric.chatclef.result.find import FindCommandBinding
        self.assertEqual("invalid_find_target", service.translate("find item minecraft:air report").reason_code)
        self.assertIsNone(FindCommandBinding.from_command("find item minecraft:air report"))

    def test_catalog_replacement_during_translation_blocks_before_submission(self):
        adapter = _RecordingMinecraftAdapter()
        adapter.get_find_catalog_snapshot = lambda: None
        extension = MinecraftFabricChatClefExtension(adapter=adapter,
            natural_language_service=ChatClefNaturalLanguageService(find_catalog_provider=snapshot))
        llm, pipeline, _ = _llm_harness(extension)
        yielded, _ = _dispatch_goto_input(llm, "마을 주민 찾아줘", voice=False)
        self.assertEqual([], adapter.requests)
        self.assertEqual(0, pipeline.provider_calls)
        self.assertEqual(1, len(yielded))

    def test_expired_proof_and_changed_translation_cannot_submit_find_or_get(self):
        from types import SimpleNamespace
        event = SimpleNamespace(text="마을 주민 찾아줘", source="lavi_chat_ui", event_id="a" * 32)
        stage = FindTranslationBindingStage(extension=SimpleNamespace(get_find_catalog_snapshot=snapshot),
            live_proof_validator=lambda *_: False, log_callback=lambda _: None)
        self.assertEqual("find_live_proof_expired", stage.inspect(event=event, translation=translation(), proof=object()).reason)
        changed = {"executable": True, "intent": {"intent_type": "get_item"}}
        self.assertEqual("find_original_request_mismatch", stage.inspect(event=event, translation=changed, proof=object()).reason)
