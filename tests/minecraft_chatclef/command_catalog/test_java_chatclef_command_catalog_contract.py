#20260815_kpopmodder: Lock registered Java ChatClef commands and Korean support status.
from __future__ import annotations

import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[3]
JAVA_ROOT = (
    REPO_ROOT
    / "plugins"
    / "Minecraft"
    / "runtime"
    / "chatclef_fabric_1.20.1"
    / "src"
    / "main"
    / "java"
)

ALTOCLEF_COMMANDS_SOURCE = JAVA_ROOT / "adris" / "altoclef" / "AltoClefCommands.java"

EXPECTED_REGISTERED_ALTOCLEF_COMMANDS = {
    "GetCommand": "get",
    "EquipCommand": "equip",
    "DepositCommand": "deposit",
    "GotoCommand": "goto",
    "IdleCommand": "idle",
    "HeroCommand": "hero",
    "LocateStructureCommand": "locate_structure",
    "StopCommand": "stop",
    "SetGammaCommand": "gamma",
    "FoodCommand": "food",
    "MeatCommand": "meat",
    "ReloadSettingsCommand": "reload_settings",
    "ResetMemoryCommand": "resetmemory",
    "GamerCommand": "gamer",
    "FollowCommand": "follow",
    "GiveCommand": "give",
    "ScanCommand": "scan",
    "AttackPlayerOrMobCommand": "attack",
    "SetAIBridgeEnabledCommand": "chatclef",
}

COMMAND_CLASS_PATHS = {
    "AttackPlayerOrMobCommand": "adris/altoclef/commands/AttackPlayerOrMobCommand.java",
    "DepositCommand": "adris/altoclef/commands/DepositCommand.java",
    "EquipCommand": "adris/altoclef/commands/EquipCommand.java",
    "FollowCommand": "adris/altoclef/commands/FollowCommand.java",
    "FoodCommand": "adris/altoclef/commands/FoodCommand.java",
    "GamerCommand": "adris/altoclef/commands/GamerCommand.java",
    "GetCommand": "adris/altoclef/commands/GetCommand.java",
    "GiveCommand": "adris/altoclef/commands/GiveCommand.java",
    "GotoCommand": "adris/altoclef/commands/GotoCommand.java",
    "HeroCommand": "adris/altoclef/commands/HeroCommand.java",
    "IdleCommand": "adris/altoclef/commands/IdleCommand.java",
    "LocateStructureCommand": "adris/altoclef/commands/LocateStructureCommand.java",
    "MeatCommand": "adris/altoclef/commands/MeatCommand.java",
    "ReloadSettingsCommand": "adris/altoclef/commands/ReloadSettingsCommand.java",
    "ResetMemoryCommand": "adris/altoclef/commands/ResetMemoryCommand.java",
    "ScanCommand": "adris/altoclef/commands/random/ScanCommand.java",
    "SetAIBridgeEnabledCommand": "adris/altoclef/commands/SetAIBridgeEnabledCommand.java",
    "SetGammaCommand": "adris/altoclef/commands/SetGammaCommand.java",
    "StopCommand": "adris/altoclef/commands/StopCommand.java",
}

ADDITIONAL_LAVI_JAVA_COMMANDS = {
    "OverlayCommand": ("overlay", "lavi/minecraft/overlay/command/OverlayCommand.java"),
}

KOREAN_SUPPORT_BY_COMMAND = {
    "get": "supported_korean",
    "food": "supported_korean",
    "meat": "supported_korean",
    "goto": "supported_korean",
    "follow": "supported_korean",
    "idle": "supported_korean",
    "stop": "supported_korean",
    "equip": "planned_item_action_phase",
    "deposit": "planned_item_action_phase",
    "give": "planned_item_action_phase",
    "attack": "java_only_task_command",
    "gamer": "java_only_task_command",
    "hero": "java_only_task_command",
    "locate_structure": "java_only_task_command",
    "scan": "java_only_task_command",
    "chatclef": "java_only_user_or_dev_command",
    "gamma": "java_only_user_or_dev_command",
    "overlay": "java_only_user_or_dev_command",
    "reload_settings": "java_only_user_or_dev_command",
    "resetmemory": "java_only_user_or_dev_command",
}


class JavaChatClefCommandCatalogContractTests(unittest.TestCase):
    def test_altoclef_registered_command_catalog_matches_fixture(self):
        actual = _registered_altoclef_command_classes()

        self.assertEqual(set(EXPECTED_REGISTERED_ALTOCLEF_COMMANDS), set(actual))
        self.assertNotIn("StashCommand", actual)

    def test_registered_command_classes_advertise_expected_command_names(self):
        for class_name, expected_name in EXPECTED_REGISTERED_ALTOCLEF_COMMANDS.items():
            with self.subTest(class_name=class_name):
                actual_name = _command_name_from_constructor(
                    JAVA_ROOT / Path(COMMAND_CLASS_PATHS[class_name])
                )

                self.assertEqual(expected_name, actual_name)

    def test_lavi_overlay_command_is_separate_java_only_command(self):
        for class_name, (expected_name, relative_path) in ADDITIONAL_LAVI_JAVA_COMMANDS.items():
            with self.subTest(class_name=class_name):
                actual_name = _command_name_from_constructor(
                    JAVA_ROOT / Path(relative_path)
                )

                self.assertEqual(expected_name, actual_name)
                self.assertEqual(
                    "java_only_user_or_dev_command",
                    KOREAN_SUPPORT_BY_COMMAND[actual_name],
                )

    def test_every_known_java_command_has_one_korean_support_status(self):
        java_command_names = set(EXPECTED_REGISTERED_ALTOCLEF_COMMANDS.values())
        java_command_names.update(
            command_name for command_name, _path in ADDITIONAL_LAVI_JAVA_COMMANDS.values()
        )

        self.assertEqual(java_command_names, set(KOREAN_SUPPORT_BY_COMMAND))
        for command_name, status in KOREAN_SUPPORT_BY_COMMAND.items():
            with self.subTest(command_name=command_name):
                self.assertIn(
                    status,
                    {
                        "supported_korean",
                        "planned_item_action_phase",
                        "java_only_task_command",
                        "java_only_user_or_dev_command",
                    },
                )

    def test_current_korean_support_is_intentionally_not_full_command_coverage(self):
        supported = {
            command
            for command, status in KOREAN_SUPPORT_BY_COMMAND.items()
            if status == "supported_korean"
        }
        unsupported_or_planned = set(KOREAN_SUPPORT_BY_COMMAND) - supported

        self.assertEqual(
            {"get", "food", "meat", "goto", "follow", "idle", "stop"},
            supported,
        )
        self.assertIn("equip", unsupported_or_planned)
        self.assertIn("deposit", unsupported_or_planned)
        self.assertIn("give", unsupported_or_planned)
        self.assertGreater(len(unsupported_or_planned), 0)


def _registered_altoclef_command_classes() -> list[str]:
    text = ALTOCLEF_COMMANDS_SOURCE.read_text(encoding="utf-8")
    command_classes: list[str] = []
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith("//"):
            continue
        match = re.search(r"new\s+([A-Za-z0-9_]+Command)\s*\(", stripped)
        if match is not None:
            command_classes.append(match.group(1))
    return command_classes


def _command_name_from_constructor(path: Path) -> str:
    text = path.read_text(encoding="utf-8")
    match = re.search(r"super\(\s*\"([a-z0-9_]+)\"", text)
    if match is None:
        raise AssertionError(f"Command constructor name not found: {path}")
    return match.group(1)


if __name__ == "__main__":
    unittest.main()

