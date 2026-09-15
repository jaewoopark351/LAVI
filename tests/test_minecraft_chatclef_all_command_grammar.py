#20260915_kpopmodder: Exercise canonical all-command grammar, runtime names, malformed slots and native variants.
import unittest

from plugins.Minecraft.fabric.chatclef.intent.chatclef_natural_language_service import ChatClefNaturalLanguageService
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_schema_validator import ChatClefIntentSchemaValidator
from plugins.Minecraft.fabric.chatclef.intent.korean_quantity_parser import KoreanQuantityParser


def catalogue():
    return {"schema_version": 1, "minecraft_version": "1.20.1", "session_id": "test-session",
            "catalogue_sha256": "a" * 64, "butler_user": "Alex", "entries": [
        {"kind": "entity", "id": "minecraft:zombie", "translation_key": "entity.minecraft.zombie",
         "korean_name": "좀비", "tokens": {"attack": "zombie"}, "capabilities": ["attack", "find"]},
        {"kind": "block", "id": "minecraft:diamond_ore", "translation_key": "block.minecraft.diamond_ore",
         "korean_name": "다이아몬드 원석", "tokens": {"scan": "DIAMOND_ORE"}, "capabilities": ["scan", "find"]},
        {"kind": "item", "id": "example:crystal", "translation_key": "item.example.crystal",
         "korean_name": "시험 수정", "tokens": {"give": "item.example.crystal"}, "capabilities": ["give", "find"]},
    ]}


class AllCommandGrammarTests(unittest.TestCase):
    def setUp(self):
        self.service = ChatClefNaturalLanguageService(runtime_catalog_provider=catalogue)

    def test_all_registered_command_examples(self):
        cases = {
            "좀비 세 마리 공격해 줘": "attack zombie 3",
            "여기를 자동 보관 장소로 등록해 줘": "auto_deposit_trust",
            "자동 보관 장소 목록 보여 줘": "auto_deposit_trusted_list",
            "자동 보관 장소 test-id 등록 해제해 줘": "auto_deposit_untrust test-id",
            "챗클레프 꺼 줘": "chatclef off",
            "철괴 16개 보관해 줘": "deposit iron_ingot 16",
            "인벤토리 전부 보관해 줘": "deposit_all",
            "다이아몬드 흉갑 장착해 줘": "equip diamond_chestplate",
            "철 골렘 찾아 줘": "find entity minecraft:iron_golem approach",
            "Alex 따라가 줘": "follow Alex",
            "음식 10포인트만큼 모아 줘": "food 10",
            "마인크래프트 엔딩까지 진행해 줘": "gamer",
            "밝기를 1.5로 설정해 줘": "gamma 1.5",
            "철괴 16개 구해 줘": "get iron_ingot 16",
            "Alex에게 철괴 세 개 줘": "give Alex iron_ingot 3",
            "500, 90, -928 좌표로 가 줘": "goto 500 90 -928",
            "적대 몹 계속 정리해 줘": "hero",
            "가만히 있어 줘": "idle",
            "엔드 요새 찾아가 줘": "locate_structure stronghold",
            "고기 10포인트만큼 모아 줘": "meat 10",
            "오버레이 꺼 줘": "overlay off",
            "마인크래프트 설정 다시 불러와 줘": "reload_settings",
            "챗클레프 대화 기록 초기화해 줘": "resetmemory",
            "다이아몬드 원석 블록 위치 스캔해 줘": "scan DIAMOND_ORE",
            "멈춰 줘": "stop",
            "아이템 집에 정리해 줘": "store_home",
        }
        for sentence, command in cases.items():
            with self.subTest(sentence=sentence):
                result = self.service.translate(sentence)
                self.assertTrue(result.executable, (result.reason_code, result.message))
                self.assertEqual(command, result.command)

    def test_lists_defaults_native_modes_and_runtime_item(self):
        for sentence, command in {
            "철괴 두 개와 금괴 세 개 구해 줘": "get [iron_ingot 2, gold_ingot 3]",
            "철괴 두 개와 철괴 세 개 구해 줘": "get [iron_ingot 5]",
            "철 방어구 세트 장착해 줘": "equip iron",
            "철 흉갑 하나와 철 신발 하나 장착해 줘": "equip [iron_chestplate 1, iron_boots 1]",
            "철 흉갑 두 개 장착해 줘": "equip iron_chestplate 2",
            "아이템 액자 두 개 보관해 줘": "deposit item_frame 2",
            "발광 아이템 액자 두 개 보관해 줘": "deposit glow_item_frame 2",
            "철괴 두 개와 금괴 세 개 전체 보관해 줘": "deposit_all [iron_ingot 2, gold_ingot 3]",
            "장비와 도구를 제외한 아이템 전부 보관해 줘": "deposit",
            "나 따라와 줘": "follow",
            "나에게 철괴 두 개 줘": "give iron_ingot 2",
            "Alex에게 시험 수정 두 개 줘": "give Alex item.example.crystal 2",
            "스캔해 줘": "scan",
            "밝기를 기본값으로 돌려 줘": "gamma 1.0",
            "네더로 가 줘": "goto nether",
            "X 100 Z -20 좌표로 가 줘": "goto 100 -20",
            "높이 80으로 가 줘": "goto 80",
            "네더 100 80 -20 좌표로 가 줘": "goto 100 80 -20 nether",
            "주변 16x16 자동 보관 장소 등록해 줘": "auto_deposit_trust area 16x16",
        }.items():
            with self.subTest(sentence=sentence):
                result = self.service.translate(sentence)
                self.assertTrue(result.executable, (result.reason_code, result.message))
                self.assertEqual(command, result.command)

    def test_invalid_requests_never_compile(self):
        for sentence in (
            "좀비 세 마리 공격하지 마", "오버레이 꺼 줘?", "기억 초기화가 뭐야?",
            "철괴 1.5개 구해 줘", "철괴 1개 2개 구해 줘", "철괴 0개 구해 줘",
            "철괴 2147483647개와 철괴 한 개 구해 줘", "밝기를 NaN으로 설정해 줘",
            "밝기를 1e999로 설정해 줘", "Alex에게 철괴 두 개와 금괴 한 개 줘",
            "플레이어 Alex 공격해 줘", "요새 찾아가 줘", "100 20 좌표로 가 줘",
            "오버레이 꺼 줘 그리고 철괴 구해 줘", "시험 수정 구해 줘",
            "음식 열 개 모아 줘", "고기 세 마리 모아 줘", "좀비 세 개 공격해 줘",
            "철괴 만만 개 구해줘", "철괴 일만이억 개 구해줘",
        ):
            with self.subTest(sentence=sentence):
                self.assertFalse(self.service.translate(sentence).executable)

    def test_resolved_name_does_not_bypass_capability_or_ambiguity(self):
        snapshot = catalogue()
        snapshot["entries"].append({**snapshot["entries"][0], "id": "example:zombie", "tokens": {"attack": "example.zombie"}})
        service = ChatClefNaturalLanguageService(runtime_catalog_provider=lambda: snapshot)
        self.assertEqual("ambiguous_registered_name", service.translate("좀비 공격해 줘").reason_code)
        self.assertEqual("native_command_target_unsupported", self.service.translate("시험 수정 구해 줘").reason_code)

    def test_new_intent_requires_original_request_binding(self):
        result = self.service.translate("오버레이 꺼 줘")
        payload = result.intent.to_dict()
        payload["slots"] = {"state": "on"}
        self.assertFalse(ChatClefIntentSchemaValidator().validate(payload)[0])

    def test_distinct_registered_names_cannot_share_ambiguous_native_token(self):
        snapshot = catalogue()
        snapshot["entries"].append({**snapshot["entries"][2], "id": "other:crystal", "korean_name": "다른 수정"})
        service = ChatClefNaturalLanguageService(runtime_catalog_provider=lambda: snapshot)
        for sentence in ("Alex에게 시험 수정 한 개 줘", "Alex에게 다른 수정 한 개 줘"):
            result = service.translate(sentence)
            self.assertFalse(result.executable)
            self.assertEqual("ambiguous_native_command_token", result.reason_code)

    def test_ascii_official_tnt_name_has_verified_korean_pronunciation_alias(self):
        snapshot = catalogue()
        snapshot["entries"].extend([
            {"kind": "item", "id": "minecraft:tnt", "translation_key": "block.minecraft.tnt",
             "korean_name": "TNT", "tokens": {"give": "tnt"}, "capabilities": ["give", "find"]},
            {"kind": "block", "id": "minecraft:tnt", "translation_key": "block.minecraft.tnt",
             "korean_name": "TNT", "tokens": {"scan": "TNT"}, "capabilities": ["scan", "find"]},
        ])
        service = ChatClefNaturalLanguageService(runtime_catalog_provider=lambda: snapshot)
        self.assertEqual("give Alex tnt 2", service.translate("Alex에게 티엔티 두 개 줘").command)
        self.assertEqual("scan TNT", service.translate("티엔티 블록 위치 스캔해 줘").command)

    def test_closed_korean_numerals(self):
        parser = KoreanQuantityParser()
        for text, expected in {"한 개": 1, "열두 개": 12, "스물한 개": 21,
                               "서른 개": 30, "십육 개": 16, "이천삼백사십오 개": 2345,
                               "일억이천만 개": 120000000, "５개": 5, "＋２개": 2}.items():
            self.assertEqual(expected, parser.parse(text))

    def test_runtime_name_metadata_and_coverage_are_snapshot_bound(self):
        from plugins.Minecraft.fabric.chatclef.intent.names.runtime_command_name_catalog import RuntimeCommandNameCatalog
        snapshot = catalogue()
        indexed = RuntimeCommandNameCatalog(snapshot)
        self.assertEqual({"registered": 1, "korean_names": 1, "missing_korean_names": 0, "ambiguous_targets": 0}, indexed.coverage()["item"])
        result = self.service.translate("Alex에게 시험 수정 두 개 줘")
        self.assertEqual({"item.example.crystal": "시험 수정"}, result.data["target_labels"])
        self.assertEqual({"session_id": "test-session", "catalogue_sha256": "a" * 64}, result.data["runtime_catalogue"])
        snapshot["entries"][0]["tokens"]["attack"] = "bad_mutation"
        self.assertEqual("zombie", indexed.entries[("entity", "minecraft:zombie")]["tokens"]["attack"])

    def test_native_mod_tokens_preserve_exact_supported_syntax_and_bounds(self):
        snapshot = catalogue()
        entry = snapshot["entries"][2]
        native = "example:crystal.variant-one"
        give = "item.example." + "c" * (256 - len("item.example."))
        entry["tokens"] = {"get": native, "deposit": native, "deposit_all": native, "give": give}
        entry["capabilities"] = list(entry["tokens"])
        service = ChatClefNaturalLanguageService(runtime_catalog_provider=lambda: snapshot)
        cases = {
            "시험 수정 두 개 구해줘": f"get {native} 2",
            "시험 수정 두 개와 시험 수정 하나 구해줘": f"get [{native} 3]",
            "시험 수정 두 개 보관해줘": f"deposit {native} 2",
            "시험 수정 두 개 전체 보관해줘": f"deposit_all [{native} 2]",
            "Alex에게 시험 수정 두 개 줘": f"give Alex {give} 2",
        }
        for text, command in cases.items():
            with self.subTest(text=text):
                result = service.translate(text)
                self.assertTrue(result.executable, result.to_dict())
                self.assertEqual(command, result.command)
        entry["tokens"]["give"] += "c"
        self.assertFalse(service.translate("Alex에게 시험 수정 두 개 줘").executable)

    def test_registered_item_name_can_contain_conversational_word_prefix(self):
        snapshot = catalogue()
        snapshot["entries"][2]["korean_name"] = "설명서"
        service = ChatClefNaturalLanguageService(runtime_catalog_provider=lambda: snapshot)
        self.assertEqual("give Alex item.example.crystal 1", service.translate("Alex에게 설명서 한 개 줘").command)

    def test_butler_default_requires_current_verified_binding(self):
        snapshot = catalogue()
        service = ChatClefNaturalLanguageService(runtime_catalog_provider=lambda: snapshot)
        self.assertEqual("follow", service.translate("나 따라와 줘").command)
        snapshot["butler_user"] = None
        self.assertEqual("verified_butler_user_required", service.translate("나 따라와 줘").reason_code)
        self.assertEqual("verified_butler_user_required", service.translate("나에게 철괴 줘").reason_code)

    def test_every_source_backed_item_name_is_indexed_without_sample_ceiling(self):
        import json
        import re
        from pathlib import Path
        from plugins.Minecraft.fabric.chatclef.intent.names.runtime_command_name_catalog import RuntimeCommandNameCatalog
        from plugins.Minecraft.fabric.chatclef.intent.names.command_target_resolver import CommandTargetResolver
        from plugins.Minecraft.fabric.chatclef.intent.korean_item_phrase_resolver import KoreanItemPhraseResolver
        resource = Path(__file__).resolve().parents[1] / "plugins/Minecraft/fabric/chatclef/intent/resources/chatclef_item_command_target_policy.json"
        artifact = json.loads(resource.read_text(encoding="utf-8"))
        rows = {}
        for token, record in artifact["targets"].items():
            match = re.fullmatch(r"(?:item|block)\.minecraft\.([a-z0-9_]+)", record.get("lang_key") or "")
            if match:
                identifier = "minecraft:" + match[1]
                rows.setdefault(identifier, {"kind": "item", "id": identifier, "translation_key": record["lang_key"],
                    "korean_name": record["display_name"], "tokens": {"give": match[1], "get": token}, "capabilities": ["get", "give"]})
        snapshot = {**catalogue(), "entries": tuple(rows.values())}
        indexed = RuntimeCommandNameCatalog(snapshot)
        resolver = CommandTargetResolver(KoreanItemPhraseResolver(), "give", indexed)
        self.assertGreater(len(rows), 500)
        for row in rows.values():
            with self.subTest(identifier=row["id"]):
                result = resolver.resolve(row["korean_name"])
                candidates = indexed.matching("item", row["korean_name"])
                self.assertEqual("ambiguous" if len(candidates) > 1 else "validated", result["status"])
                self.assertIn(row["id"], {candidate["id"] for candidate in candidates})
        self.assertEqual(len(rows), indexed.coverage()["item"]["registered"])


if __name__ == "__main__":
    unittest.main()
