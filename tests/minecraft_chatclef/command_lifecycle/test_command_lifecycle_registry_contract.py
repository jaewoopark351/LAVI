#20260907_kpopmodder: Lock exact phrase and evidence coverage to the command registry.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleStartResponse,
    CommandLifecycleTerminalResponse,
    CommandPhraseProfileRegistry,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackLifecycleKindProfileRegistry,
    CommandTerminalEvidenceProfile,
    CommandTerminalEvidenceProfileRegistry,
)


EXPECTED_COMMANDS = (
    "attack",
    "auto_deposit_trust",
    "auto_deposit_trusted_list",
    "auto_deposit_untrust",
    "chatclef",
    "deposit",
    "deposit_all",
    "equip",
    "follow",
    "food",
    "gamer",
    "gamma",
    "get",
    "give",
    "goto",
    "hero",
    "idle",
    "locate_structure",
    "meat",
    "overlay",
    "reload_settings",
    "resetmemory",
    "scan",
    "stop",
    "store_home",
    "자동보관등록",
)

EXPECTED_RESPONSE_LIFECYCLE_KINDS = {
    "attack": "finite_task",
    "auto_deposit_trust": "asynchronous_immediate",
    "auto_deposit_trusted_list": "asynchronous_immediate",
    "auto_deposit_untrust": "asynchronous_immediate",
    "chatclef": "asynchronous_immediate",
    "deposit": "finite_task",
    "deposit_all": "finite_task",
    "equip": "finite_task",
    "follow": "persistent_task",
    "food": "finite_task",
    "gamer": "finite_task",
    "gamma": "asynchronous_immediate",
    "get": "finite_task",
    "give": "finite_task",
    "goto": "finite_task",
    "hero": "persistent_task",
    "idle": "persistent_task",
    "locate_structure": "finite_task",
    "meat": "finite_task",
    "overlay": "asynchronous_immediate",
    "reload_settings": "asynchronous_immediate",
    "resetmemory": "asynchronous_immediate",
    "scan": "asynchronous_immediate",
    "stop": "specialized_control",
    "store_home": "finite_task",
    "자동보관등록": "asynchronous_immediate",
}


class CommandLifecycleRegistryContractTests(unittest.TestCase):
    def test_phrase_and_evidence_profiles_cover_exact_registered_snapshot(self):
        commands = KoreanChatClefCommandRegistry()
        phrases = CommandPhraseProfileRegistry(commands)
        evidence = CommandTerminalEvidenceProfileRegistry(commands)
        lifecycle_kinds = CommandFeedbackLifecycleKindProfileRegistry(commands)

        self.assertEqual(EXPECTED_COMMANDS, commands.command_names())
        self.assertEqual(EXPECTED_COMMANDS, phrases.command_names())
        self.assertEqual(EXPECTED_COMMANDS, evidence.command_names())
        self.assertEqual(EXPECTED_COMMANDS, lifecycle_kinds.command_names())
        self.assertEqual(EXPECTED_COMMANDS, tuple(EXPECTED_RESPONSE_LIFECYCLE_KINDS))
        for command_name in EXPECTED_COMMANDS:
            with self.subTest(command_name=command_name):
                spec = commands.spec(command_name)
                self.assertEqual(
                    spec.lifecycle_kind,
                    phrases.profile(command_name).lifecycle_kind,
                )
                self.assertEqual(
                    spec.lifecycle_kind,
                    evidence.profile(command_name).lifecycle_kind,
                )
                expected_kind = EXPECTED_RESPONSE_LIFECYCLE_KINDS[command_name]
                self.assertEqual(
                    expected_kind,
                    lifecycle_kinds.profile(command_name).response_lifecycle_kind,
                )
                self.assertEqual(
                    expected_kind,
                    phrases.profile(command_name).response_lifecycle_kind,
                )
                self.assertEqual(
                    expected_kind,
                    evidence.profile(command_name).response_lifecycle_kind,
                )
                expected_trigger = (
                    "accepted_submission_caution"
                    if command_name == "gamma"
                    else "specialized_control_result"
                    if command_name == "stop"
                    else "result_callback"
                )
                self.assertEqual(
                    expected_trigger,
                    lifecycle_kinds.profile(command_name).terminal_trigger,
                )

    def test_only_get_and_trusted_store_home_profiles_are_verified(self):
        evidence = CommandTerminalEvidenceProfileRegistry()

        for command_name in EXPECTED_COMMANDS:
            with self.subTest(command_name=command_name):
                expected = (
                    CommandTerminalEvidenceProfile.VERIFIED
                    if command_name in {"get", "store_home"}
                    else CommandTerminalEvidenceProfile.CAUTIOUS
                )
                self.assertEqual(expected, evidence.profile(command_name).rollout_state)
        self.assertEqual(
            "get_acquisition",
            evidence.profile("get").success_evaluator_id,
        )
        self.assertEqual(
            "store_home_completion",
            evidence.profile("store_home").success_evaluator_id,
        )

    def test_corrected_acquisition_and_home_labels_are_not_ambiguous(self):
        profiles = CommandPhraseProfileRegistry()

        self.assertEqual("음식 모으기", profiles.profile("food").command_label)
        self.assertEqual("고기 모으기", profiles.profile("meat").command_label)
        self.assertEqual(
            "아이템 집 정리",
            profiles.profile("store_home").command_label,
        )
        self.assertEqual("food_acquisition", profiles.profile("food").family)
        self.assertEqual("meat_acquisition", profiles.profile("meat").family)

    def test_general_envelopes_use_command_lifecycle_identity(self):
        start = CommandLifecycleStartResponse(
            text="철 곡괭이 만들어 줄게",
            event_id="a" * 32,
            command_name="get",
        )
        terminal = CommandLifecycleTerminalResponse(
            text="철 곡괭이 다 만들었어",
            event_id="a" * 32,
            command_name="get",
        )

        self.assertEqual(("command_lifecycle", "command_start"), (start.route_kind, start.response_kind))
        self.assertEqual(
            ("command_lifecycle", "command_terminal"),
            (terminal.route_kind, terminal.response_kind),
        )


if __name__ == "__main__":
    unittest.main()
