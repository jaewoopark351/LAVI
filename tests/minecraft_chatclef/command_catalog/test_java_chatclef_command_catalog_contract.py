#20260815_kpopmodder: Lock registered Java ChatClef commands and Korean support status.
#20260905_kpopmodder: Include the activated H5 registrar in the exact source-backed 26-command catalog.
from __future__ import annotations

from collections import Counter
import json
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
AUTO_DEPOSIT_RUNTIME_SOURCE = (
    JAVA_ROOT
    / "lavi"
    / "minecraft"
    / "task"
    / "container"
    / "deposit"
    / "auto"
    / "AutoDepositRuntime.java"
)
AUTO_DEPOSIT_TRUSTED_REGISTRAR_SOURCE = (
    JAVA_ROOT
    / "lavi"
    / "minecraft"
    / "task"
    / "container"
    / "deposit"
    / "auto"
    / "trusted"
    / "command"
    / "AutoDepositTrustedCommandRegistrar.java"
)
ARTIFACT_ROOT = Path(__file__).resolve().parent
REGISTERED_COMMANDS_ARTIFACT = (
    ARTIFACT_ROOT / "chatclef_registered_commands.snapshot.json"
)
SUPPORT_MATRIX_ARTIFACT = ARTIFACT_ROOT / "chatclef_command_support_matrix.json"


class JavaChatClefCommandCatalogContractTests(unittest.TestCase):
    def test_altoclef_registered_command_catalog_matches_fixture(self):
        actual = _registered_altoclef_command_classes()
        expected = _snapshot_chatclef_java_commands()

        self.assertEqual(set(expected), set(actual))
        self.assertNotIn("StashCommand", actual)

    def test_registered_command_classes_advertise_expected_command_names(self):
        for command in _registered_command_snapshot():
            if command["owner"] != "chatclef_java":
                continue
            class_name = command["class_name"]
            with self.subTest(class_name=class_name):
                actual_name = _command_name_from_constructor(
                    JAVA_ROOT / Path(command["path"])
                )

                self.assertEqual(command["command"], actual_name)

    def test_activated_h5_registrar_matches_four_source_backed_snapshot_rows(self):
        activation = AUTO_DEPOSIT_RUNTIME_SOURCE.read_text(encoding="utf-8")
        self.assertRegex(
            activation,
            r"trustedCommandRegistrar\.register\(mod\)\s*;",
        )

        actual = _registered_h5_command_classes()
        expected = _snapshot_lavi_auto_deposit_trusted_commands()

        self.assertEqual(expected, actual)
        self.assertEqual(4, len(actual))

    def test_active_catalog_has_exact_26_names_without_duplicates(self):
        snapshot = _registered_command_snapshot()
        names = [row["command"] for row in snapshot]
        expected_names = set(_support_by_command())

        self.assertEqual(26, len(names))
        self.assertEqual(26, len(set(names)))
        self.assertEqual([], [name for name, count in Counter(names).items() if count > 1])
        self.assertEqual(expected_names, set(names))

        source_names = {
            _command_name_from_constructor(JAVA_ROOT / Path(row["path"]))
            for row in snapshot
        }
        self.assertEqual(set(names), source_names)

    def test_lavi_overlay_command_is_separate_java_only_command(self):
        for command in _registered_command_snapshot():
            if command["owner"] != "lavi_overlay":
                continue
            class_name = command["class_name"]
            with self.subTest(class_name=class_name):
                actual_name = _command_name_from_constructor(
                    JAVA_ROOT / Path(command["path"])
                )

                self.assertEqual(command["command"], actual_name)
                self.assertEqual(
                    "java_only_user_or_dev_command",
                    _support_by_command()[actual_name],
                )

    def test_every_known_java_command_has_one_korean_support_status(self):
        java_command_names = {
            command["command"] for command in _registered_command_snapshot()
        }
        support_by_command = _support_by_command()

        self.assertEqual(java_command_names, set(support_by_command))
        for command_name, status in support_by_command.items():
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
            for command, status in _support_by_command().items()
            if status == "supported_korean"
        }
        unsupported_or_planned = set(_support_by_command()) - supported

        self.assertEqual(
            {
                "deposit",
                "equip",
                "follow",
                "food",
                "get",
                "give",
                "goto",
                "idle",
                "meat",
                "stop",
                "store_home",
                "auto_deposit_trust",
            },
            supported,
        )
        self.assertGreater(len(unsupported_or_planned), 0)

    def test_support_matrix_uses_pre_shared_case_schema(self):
        matrix = _support_matrix()

        self.assertEqual(1, matrix["schema_version"])
        self.assertEqual("PRE_SHARED_CASE", matrix["evidence_maturity"])
        for row in matrix["commands"]:
            with self.subTest(command=row["command"]):
                self.assertNotIn("golden_case_ids", row)
                self.assertNotIn("golden_tests", row)


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
    literal = re.search(r'super\(\s*"((?:\\.|[^"\\])*)"', text)
    if literal is not None:
        return _decode_java_string(literal.group(1))
    if re.search(r"super\(\s*COMMAND_NAME\b", text) is not None:
        constant = re.search(
            r'public\s+static\s+final\s+String\s+COMMAND_NAME\s*=\s*"((?:\\.|[^"\\])*)"',
            text,
        )
        if constant is not None:
            return _decode_java_string(constant.group(1))
    raise AssertionError(f"Command constructor name not found: {path}")


def _decode_java_string(value: str) -> str:
    return json.loads(f'"{value}"')


def _registered_h5_command_classes() -> dict[str, str]:
    text = AUTO_DEPOSIT_TRUSTED_REGISTRAR_SOURCE.read_text(encoding="utf-8")
    class_names = set(
        re.findall(
            r"new\s+(AutoDeposit(?:Trust|Untrust|TrustedList|KoreanBulkTrust)Command)\s*\(",
            text,
        )
    )
    snapshot = {
        row["class_name"]: row
        for row in _registered_command_snapshot()
        if row["owner"] == "lavi_auto_deposit_trusted"
    }
    return {
        class_name: _command_name_from_constructor(
            JAVA_ROOT / Path(snapshot[class_name]["path"])
        )
        for class_name in class_names
    }


def _registered_command_snapshot() -> list[dict[str, str]]:
    snapshot = _load_json(REGISTERED_COMMANDS_ARTIFACT)
    if snapshot.get("schema_version") != 1:
        raise AssertionError("registered command snapshot schema_version must be 1")
    commands = snapshot.get("commands")
    if not isinstance(commands, list):
        raise AssertionError("registered command snapshot commands must be a list")
    return [dict(command) for command in commands]


def _snapshot_chatclef_java_commands() -> dict[str, str]:
    return {
        command["class_name"]: command["command"]
        for command in _registered_command_snapshot()
        if command["owner"] == "chatclef_java"
    }


def _snapshot_lavi_auto_deposit_trusted_commands() -> dict[str, str]:
    return {
        command["class_name"]: command["command"]
        for command in _registered_command_snapshot()
        if command["owner"] == "lavi_auto_deposit_trusted"
    }


def _support_matrix() -> dict[str, object]:
    matrix = _load_json(SUPPORT_MATRIX_ARTIFACT)
    if matrix.get("schema_version") != 1:
        raise AssertionError("support matrix schema_version must be 1")
    if matrix.get("evidence_maturity") != "PRE_SHARED_CASE":
        raise AssertionError("support matrix must remain PRE_SHARED_CASE")
    return matrix


def _support_by_command() -> dict[str, str]:
    commands = _support_matrix().get("commands")
    if not isinstance(commands, list):
        raise AssertionError("support matrix commands must be a list")
    return {
        str(command["command"]): str(command["support_status"])
        for command in commands
    }


def _load_json(path: Path) -> dict[str, object]:
    return json.loads(path.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
