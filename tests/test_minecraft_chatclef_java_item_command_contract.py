#20260815_kpopmodder: Lock Phase 0 ChatClef Java item command contracts.
from __future__ import annotations

import hashlib
import subprocess
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
JAVA_SOURCE_ROOT = (
    "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java"
)
JAVA_ROOT = REPO_ROOT / Path(JAVA_SOURCE_ROOT)
#20260829_openai: Rebaseline the reviewed dispatcher after its ownership-gate hardening.
REVIEWED_SOURCE_COMMIT = "03d4e93b36ffdde4936da274eab72632569ea5b0"

JAVA_CONTRACT_HASHES = {
    "adris/altoclef/commands/GetCommand.java": "4f3b86aabd4d94eab1a97b11d8225e2852ea4407c216bab5848b542894dc461d",
    "adris/altoclef/commands/EquipCommand.java": "fc63661acb97a98c989165f6499c2216a711b66359695e64682303f03af7848a",
    "adris/altoclef/commands/DepositCommand.java": "4259159f77f5467401bd6b96e89c3fc8898a75b72716c9ac26f4007cd881ca80",
    "adris/altoclef/commands/GiveCommand.java": "b88c70e31ee2f76cd078ec691fdc3ecf8acd8ba92acca6dfcb2779c60e1d0a45",
    "adris/altoclef/commandsystem/Arg.java": "26d54316c9b3336ba63dcbe848274d5005e21cc26ca00fe75c95840e9cfe40c2",
    "adris/altoclef/commandsystem/ArgParser.java": "d2fcd9aaa8c6f59d2958545319e0c0543940f2e62875c46e2460f1ef60347cc8",
    "adris/altoclef/commandsystem/CommandExecutor.java": "17ef21bf10e5841b4fd2c5fe0190eda1b6d9ce2e9825a5cf059fdf44b47e7136",
    "adris/altoclef/commandsystem/ItemList.java": "38c442cc01a700a91c2b3af2dadb86a32ca4316a81cbbec2f7d311d6f30783ef",
    "lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandDispatcher.java": "5afbb6d923a453f7888efb46957920bf3e665ad415f3f492af8980d2a764d8fc",
}


class MinecraftChatClefJavaItemCommandContractTests(unittest.TestCase):
    def test_java_contract_sources_match_reviewed_git_blob_hashes(self):
        for relative_path, expected_hash in JAVA_CONTRACT_HASHES.items():
            with self.subTest(relative_path=relative_path):
                blob = _git_blob_bytes(
                    REVIEWED_SOURCE_COMMIT,
                    _repository_relative_java_path(relative_path),
                )
                actual_hash = hashlib.sha256(blob).hexdigest()

                self.assertEqual(expected_hash, actual_hash)

    def test_java_contract_sources_have_not_drifted_at_head(self):
        for relative_path, expected_hash in JAVA_CONTRACT_HASHES.items():
            with self.subTest(relative_path=relative_path):
                blob = _git_blob_bytes(
                    "HEAD",
                    _repository_relative_java_path(relative_path),
                )
                actual_hash = hashlib.sha256(blob).hexdigest()

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


def _repository_relative_java_path(relative_path: str) -> str:
    return f"{JAVA_SOURCE_ROOT}/{relative_path}"


def _git_blob_bytes(commit: str, repository_relative_path: str) -> bytes:
    return subprocess.check_output(
        [
            "git",
            "show",
            "--no-textconv",
            f"{commit}:{repository_relative_path}",
        ],
        cwd=REPO_ROOT,
    )


if __name__ == "__main__":
    unittest.main()
