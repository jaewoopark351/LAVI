#20260815_kpopmodder: Lock Phase 0 ChatClef Java item command contracts.
from __future__ import annotations

import hashlib
import unittest
from pathlib import Path

from plugins.Minecraft.fabric.chatclef.intent.chatclef_command_compiler import (
    ChatClefCommandCompiler,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)


REPO_ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = REPO_ROOT / "plugins" / "Minecraft" / "runtime" / "chatclef_fabric_1.20.1" / "src" / "main" / "java"

JAVA_CONTRACT_HASHES = {
    "adris/altoclef/commands/GetCommand.java": "4b49e3d4569c3b8843c7a24aab2a8dd5976b32b5ec1288629991eed13cddef69",
    "adris/altoclef/commands/EquipCommand.java": "613ba1faec1a9625dd7042675fc660fdc41f478fef2c1718265bc5683855a00a",
    "adris/altoclef/commands/DepositCommand.java": "d4d598c6d1f2f3465a18f0a150b8294f621f3248c35dbcd97896ac75209f09b5",
    "adris/altoclef/commands/GiveCommand.java": "c02c59de782c1f20fab7b004c3dc173c08e3f8ffa5e1e4052744b811ab67cf85",
    "adris/altoclef/commandsystem/Arg.java": "f319d8751f9b210536ffbee9d353642f2e1a08a50e141ee41a1473bc10f4a382",
    "adris/altoclef/commandsystem/ArgParser.java": "657ed1aaa019f043b32740eeb69d180f1c38c7ac6f6611befa7a9c5b9f919efb",
    "adris/altoclef/commandsystem/CommandExecutor.java": "41bbbcd63260e7b8106c72bcf6b59b5d9c0dbdfacda22cd9bd663cee4394ac1e",
    "adris/altoclef/commandsystem/ItemList.java": "cc54ee78aa933ddc29f161131b6c80165ebcd7b1330b6038ca37c1ad6d193c93",
    "lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandDispatcher.java": "458edb3b1639e4a7db282f61ae743d780575a33a18bedcfc1b78bf12e3178f7c",
}


class MinecraftChatClefJavaItemCommandContractTests(unittest.TestCase):
    def test_java_contract_sources_match_documented_baseline_hashes(self):
        for relative_path, expected_hash in JAVA_CONTRACT_HASHES.items():
            with self.subTest(relative_path=relative_path):
                path = JAVA_ROOT / Path(relative_path)
                actual_hash = hashlib.sha256(path.read_bytes()).hexdigest()

                self.assertEqual(expected_hash, actual_hash)

    def test_get_single_item_grammar_contract(self):
        contract = {
            "command": "get",
            "single_item": "get <item> [count]",
            "omitted_count": 1,
            "explicit_count": "preserved",
        }

        self.assertEqual("get", contract["command"])
        self.assertEqual("get <item> [count]", contract["single_item"])
        self.assertEqual(1, contract["omitted_count"])
        self.assertEqual("preserved", contract["explicit_count"])

    def test_get_item_list_grammar_contract(self):
        contract = {
            "command": "get",
            "multi_item": "get [<item> [count], ...]",
            "duplicate_targets": "merged by ItemList",
        }

        self.assertEqual("get [<item> [count], ...]", contract["multi_item"])
        self.assertEqual("merged by ItemList", contract["duplicate_targets"])

    def test_deposit_java_default_count_contract_is_one(self):
        contract = {
            "command": "deposit",
            "single_item": "deposit <item> [count]",
            "omitted_count": 1,
            "korean_no_count_policy": "separate UX decision",
        }

        self.assertEqual("deposit <item> [count]", contract["single_item"])
        self.assertEqual(1, contract["omitted_count"])
        self.assertEqual("separate UX decision", contract["korean_no_count_policy"])

    def test_bare_deposit_contract_is_broad_non_gear_storage(self):
        contract = {
            "command": "deposit",
            "bare_form": "deposit",
            "stores": "non-armor non-ToolItem inventory items",
            "not_equivalent_to": "Korean all-inventory wording",
        }

        self.assertEqual("deposit", contract["bare_form"])
        self.assertEqual("non-armor non-ToolItem inventory items", contract["stores"])
        self.assertEqual("Korean all-inventory wording", contract["not_equivalent_to"])

    def test_give_butler_current_user_mode_contract(self):
        contract = {
            "command": "give",
            "butler_current_user_mode": "give <item> [count]",
            "omitted_count": 1,
            "item_shape": "single String item",
        }

        self.assertEqual(
            "give <item> [count]",
            contract["butler_current_user_mode"],
        )
        self.assertEqual(1, contract["omitted_count"])
        self.assertEqual("single String item", contract["item_shape"])

    def test_give_explicit_recipient_requires_count_contract(self):
        contract = {
            "command": "give",
            "explicit_recipient": "give <username> <item> <count>",
            "count_required": True,
            "invalid_shorthand": "give Steve diamond",
            "item_shape": "single String item",
        }

        self.assertEqual(
            "give <username> <item> <count>",
            contract["explicit_recipient"],
        )
        self.assertTrue(contract["count_required"])
        self.assertEqual("give Steve diamond", contract["invalid_shorthand"])
        self.assertEqual("single String item", contract["item_shape"])

    def test_bridge_dispatcher_owns_prefix_insertion(self):
        compiler = ChatClefCommandCompiler()

        command = compiler.compile(
            ChatClefIntentDTO(
                intent_type=ChatClefIntentType.GET_ITEM,
                item_phrase="다이아몬드",
                quantity=1,
            ),
            target="diamond",
        )

        self.assertEqual("get diamond 1", command)
        self.assertFalse(command.startswith("@"))


if __name__ == "__main__":
    unittest.main()
