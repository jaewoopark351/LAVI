#20260907_kpopmodder: Lock all 26 registered raw grammars and bounded slot projections.
from __future__ import annotations

import unittest
from pathlib import Path
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackDescriptor,
    CommandFeedbackDescriptorFactory,
    CommandFeedbackLifecycleKindProfileRegistry,
    CommandFeedbackLifecycleKindProfile,
    CommandFeedbackPublicationCoordinator,
    CommandFeedbackRawFormDecoder,
    CommandFeedbackRawFormProfileRegistry,
    CommandTerminalFact,
)


VALID_FORMS = {
    "attack": (("attack zombie", "target_default_count"), ("attack zombie 2", "target_count"), ("attack zombie 0", "target_count_unprojected")),
    "auto_deposit_trust": (("auto_deposit_trust", "crosshair_target"), ("auto_deposit_trust area 16x16", "area_16x16"), ("auto_deposit_trust 반경 16x16", "radius_16x16")),
    "auto_deposit_trusted_list": (("auto_deposit_trusted_list", "no_arguments"),),
    "auto_deposit_untrust": (("auto_deposit_untrust", "crosshair_target"), ("auto_deposit_untrust td_aaaaaaaaaaaaaaaaaaaaaaaa", "explicit_destination")),
    "chatclef": (("chatclef ON", "state_on"),),
    "deposit": (("deposit", "inventory_default"), ("deposit stone 2", "single_item"), ("deposit [stone 2, dirt]", "item_list"), ("deposit stone 0", "single_item_unprojected")),
    "deposit_all": (("deposit_all", "inventory_default"), ("deposit_all dirt", "single_item"), ("deposit_all dirt -1", "single_item_unprojected")),
    "equip": (("equip iron", "equipment_material_set"), ("equip [iron_helmet, iron_chestplate]", "equipment_item_list"), ("equip iron_helmet 0", "equipment_single_item_unprojected")),
    "follow": (("follow", "butler_player"), ("follow A", "explicit_player"), ("follow Player-With-Dash", "explicit_player_unprojected")),
    "food": (("food 10", "food_units"), ("food 0", "food_units_unprojected")),
    "gamer": (("gamer", "no_arguments"),),
    "gamma": (("gamma", "default_value"), ("gamma 1.25", "explicit_value"), ("gamma 0x1.0p2", "explicit_value")),
    "get": (("get diamond_pickaxe", "single_item"), ("get [oak_log 2, birch_log 3]", "item_list"), ("get diamond_pickaxe -1", "single_item_unprojected")),
    "give": (("give diamond", "butler_item_default_count"), ("give diamond 2", "butler_item_count"), ("give A diamond 2", "explicit_player_item_count"), ("give Player-With-Dash diamond 2", "explicit_player_item_count_unprojected"), ("give diamond 0", "butler_item_count_unprojected")),
    "goto": (("goto 1 64 3", "xyz"), ("goto nether", "dimension"), ("goto (1 2 nether)", "parenthesized_xz_dimension")),
    "hero": (("hero", "no_arguments"),),
    "idle": (("idle", "no_arguments"),),
    "locate_structure": (("locate_structure stronghold", "structure"),),
    "meat": (("meat 8", "meat_units"), ("meat -1", "meat_units_unprojected")),
    "overlay": (("overlay off", "state_off"),),
    "reload_settings": (("reload_settings", "no_arguments"),),
    "resetmemory": (("resetmemory", "no_arguments"),),
    "scan": (("scan", "default_target"), ("scan STONE", "explicit_target"), ("scan minecraft:stone", "explicit_target_unprojected")),
    "stop": (("stop", "no_arguments"),),
    "store_home": (("store_home", "no_arguments"),),
    "자동보관등록": (("자동보관등록 영역 16x16", "area_16x16"), ("자동보관등록 반경 16x16", "radius_16x16")),
}

INVALID_FORMS = {
    "attack": "attack zombie many",
    "auto_deposit_trust": "auto_deposit_trust area 8x8",
    "auto_deposit_trusted_list": "auto_deposit_trusted_list extra",
    "auto_deposit_untrust": "auto_deposit_untrust not_a_destination_id",
    "chatclef": "chatclef maybe",
    "deposit": "deposit [stone two]",
    "deposit_all": "deposit_all stone many",
    "equip": "equip",
    "follow": "follow Steve Alex",
    "food": "food many",
    "gamer": "gamer target",
    "gamma": "gamma NaN",
    "get": "get",
    "give": "give Steve diamond",
    "goto": "goto 1 2 3 4",
    "hero": "hero extra",
    "idle": "idle extra",
    "locate_structure": "locate_structure village",
    "meat": "meat many",
    "overlay": "overlay maybe",
    "reload_settings": "reload_settings extra",
    "resetmemory": "resetmemory extra",
    "scan": "scan stone dirt",
    "stop": "stop now",
    "store_home": "store_home extra",
    "자동보관등록": "자동보관등록",
}


class RawCommandFeedbackFormMatrixTests(unittest.TestCase):
    def setUp(self) -> None:
        self.decoder = CommandFeedbackRawFormDecoder()
        self.factory = CommandFeedbackDescriptorFactory()

    def test_exact_26_profile_and_valid_invalid_form_matrix(self):
        expected = KoreanChatClefCommandRegistry().command_names()

        self.assertEqual(expected, tuple(VALID_FORMS))
        self.assertEqual(expected, tuple(INVALID_FORMS))
        self.assertEqual(
            expected,
            CommandFeedbackRawFormProfileRegistry().command_names(),
        )
        for command_name in expected:
            with self.subTest(command_name=command_name, kind="invalid"):
                self.assertIsNone(self.decoder.decode(INVALID_FORMS[command_name]))
            for command, form_kind in VALID_FORMS[command_name]:
                for prefix in ("", "@"):
                    prefixed = f"{prefix}{command}"
                    with self.subTest(
                        command_name=command_name,
                        form_kind=form_kind,
                        prefix=prefix,
                    ):
                        raw_form = self.decoder.decode(prefixed)
                        self.assertIsNotNone(raw_form)
                        self.assertEqual(command_name, raw_form.command_name)
                        self.assertEqual(form_kind, raw_form.form_kind)
                        descriptor = _decode(self.factory, prefixed)
                        self.assertIsNotNone(descriptor)
                        self.assertEqual(form_kind, descriptor.form_kind)
                        self.assertEqual(
                            CommandFeedbackLifecycleKindProfileRegistry()
                            .profile(command_name)
                            .response_lifecycle_kind,
                            descriptor.response_lifecycle_kind,
                        )

    def test_single_and_bracketed_item_slots_are_bounded_and_ordered(self):
        single = _decode(self.factory, "@get diamond_pickaxe 2")
        multiple = _decode(self.factory, "@get [oak_log 2, birch_log 3]")

        self.assertEqual(CommandFeedbackDescriptor.RAW_TYPED, single.detail_level)
        self.assertEqual("verified", single.rollout_state)
        self.assertEqual(("diamond_pickaxe", 2), (single.target_item, single.requested_count))
        self.assertEqual(
            (("oak_log", 2), ("birch_log", 3)),
            tuple(
                (target.canonical_target, target.requested_count)
                for target in multiple.targets
            ),
        )
        self.assertIsNone(multiple.target_item)
        self.assertIsNone(multiple.requested_count)
        self.assertEqual("cautious", multiple.rollout_state)

    def test_recoverable_player_setting_scan_and_goto_slots_are_preserved(self):
        give = _decode(self.factory, "give A diamond 2")
        gamma = _decode(self.factory, "gamma 1.25")
        scan = _decode(self.factory, "scan STONE")
        goto = _decode(self.factory, "goto 1 64 3 nether")

        self.assertEqual(("A", "diamond", 2), (give.player_name, give.target_item, give.requested_count))
        self.assertEqual("1.25", gamma.setting_value)
        self.assertEqual("stone", scan.operation_target)
        self.assertEqual(((1, 64, 3), "nether"), (goto.coordinate_values, goto.dimension))

    def test_renderer_uses_safe_slots_and_never_echoes_unknown_item_id(self):
        renderer = CommandLifecycleResponseRenderer()
        known = _decode(self.factory, "get diamond_pickaxe 2")
        unknown = _decode(self.factory, "get future_mod_item 2")
        gamma = _decode(self.factory, "gamma 1.25")
        gamma_zero = _decode(self.factory, "gamma 1.0")
        goto = _decode(self.factory, "goto 1 64 3 nether")
        stronghold = _decode(self.factory, "locate_structure stronghold")
        temple = _decode(self.factory, "locate_structure desert_temple")

        self.assertEqual("다이아몬드 곡괭이 2개 구해 올게", renderer.render_start(known))
        unknown_text = renderer.render_start(unknown)
        self.assertNotIn("future_mod_item", unknown_text)
        self.assertEqual("요청한 아이템 2개 구해 올게", unknown_text)
        self.assertEqual("밝기를 1.25로 바꿀게", renderer.render_start(gamma))
        self.assertEqual("밝기를 1.0으로 바꿀게", renderer.render_start(gamma_zero))
        self.assertEqual("네더의 좌표 1, 64, 3으로 갈게", renderer.render_start(goto))
        self.assertEqual("요새를 찾아볼게", renderer.render_start(stronghold))
        self.assertEqual("사막 사원을 찾아볼게", renderer.render_start(temple))

    def test_java_accepted_but_unsafe_slots_fall_back_to_command_only_feedback(self):
        renderer = CommandLifecycleResponseRenderer()
        commands = (
            "attack zombie 0",
            "deposit stone 0",
            "deposit_all dirt -1",
            "equip iron_helmet 0",
            "follow Player-With-Dash",
            "food 0",
            "get diamond_pickaxe -1",
            "give Player-With-Dash diamond 2",
            "give diamond 0",
            "meat -1",
            "scan minecraft:stone",
        )

        for command in commands:
            with self.subTest(command=command):
                descriptor = _decode(self.factory, command)
                self.assertIsNotNone(descriptor)
                self.assertEqual(
                    CommandFeedbackDescriptor.COMMAND_NAME_ONLY,
                    descriptor.detail_level,
                )
                self.assertEqual("cautious", descriptor.rollout_state)
                self.assertIsNone(descriptor.target_item)
                self.assertIsNone(descriptor.requested_count)
                rendered = renderer.render_start(descriptor)
                self.assertTrue(rendered)
                for unit in command.split(" ")[1:]:
                    if unit not in {"stone", "dirt", "diamond"}:
                        self.assertNotIn(unit, rendered)

    def test_java_string_and_integer_argument_sources_back_the_parity_rows(self):
        java_root = (
            Path(__file__).resolve().parents[3]
            / "plugins"
            / "Minecraft"
            / "runtime"
            / "chatclef_fabric_1.20.1"
            / "src"
            / "main"
            / "java"
            / "adris"
            / "altoclef"
        )
        contracts = {
            "commands/FollowCommand.java": ("new Arg(String.class",),
            "commands/GiveCommand.java": (
                "new Arg(String.class, \"username\"",
                "new Arg(String.class, \"item\")",
                "new Arg(Integer.class, \"count\"",
            ),
            "commands/AttackPlayerOrMobCommand.java": (
                "new Arg<>(String.class, \"name\")",
                "new Arg<>(Integer.class, \"count\"",
            ),
            "commands/FoodCommand.java": (
                "new Arg<>(Integer.class, \"count\")",
            ),
            "commands/MeatCommand.java": (
                "new Arg<>(Integer.class, \"count\")",
            ),
            "commands/random/ScanCommand.java": (
                "new Arg<>(String.class, \"block\"",
            ),
            "commandsystem/ItemList.java": ("Integer.parseInt",),
        }

        for relative_path, fragments in contracts.items():
            with self.subTest(relative_path=relative_path):
                source = (java_root / relative_path).read_text(encoding="utf-8")
                for fragment in fragments:
                    self.assertIn(fragment, source)

    def test_markerless_raw_stop_gets_only_common_cautious_feedback(self):
        self.assertIsNotNone(self.decoder.decode("stop"))
        for command in ("stop", "@stop"):
            descriptor = _decode(self.factory, command)
            self.assertIsNotNone(descriptor)
            self.assertEqual("cautious", descriptor.rollout_state)
            self.assertEqual(
                "specialized_control",
                descriptor.response_lifecycle_kind,
            )
            self.assertEqual(
                "멈출게",
                CommandLifecycleResponseRenderer().render_start(descriptor),
            )
            terminal = CommandLifecycleResponseRenderer().render_terminal(
                type(
                    "Fact",
                    (),
                    {
                        "descriptor": descriptor,
                        "status": "completed",
                        "verified": False,
                        "dispatch_started": True,
                    },
                )()
            )
            self.assertEqual(
                "중지 명령은 끝났는데, 실제로 멈췄는지는 확인하지 못했어",
                terminal,
            )
            self.assertNotEqual("멈췄어", terminal)

    def test_exact_26_commands_render_and_follow_their_publication_lifecycle(self):
        renderer = CommandLifecycleResponseRenderer()
        lifecycle_profiles = CommandFeedbackLifecycleKindProfileRegistry()

        for command_name, forms in VALID_FORMS.items():
            with self.subTest(command_name=command_name):
                descriptor = _decode(self.factory, forms[0][0])
                profile = lifecycle_profiles.profile(command_name)
                status = (
                    "accepted_without_result_callback"
                    if command_name == "gamma"
                    else "failed"
                    if profile.response_lifecycle_kind
                    == CommandFeedbackLifecycleKindProfile.PERSISTENT_TASK
                    else "completed"
                )
                fact = CommandTerminalFact(
                    descriptor=descriptor,
                    status=status,
                    verified=False,
                    dispatch_started=status != "accepted_without_result_callback",
                    result_reason="matrix_terminal",
                    event_id=descriptor.event_id,
                    owner_token=object(),
                )
                responses = (
                    renderer.render_start(descriptor),
                    renderer.render_status(
                        SimpleNamespace(state="running", descriptor=descriptor)
                    ),
                    renderer.render_terminal(fact),
                )
                self.assertTrue(all(responses))
                self.assertTrue(all(len(text) <= 256 for text in responses))

                publications = CommandFeedbackPublicationCoordinator()
                lifecycle_token = object()
                start = publications.begin(
                    lifecycle_token,
                    profile.response_lifecycle_kind,
                )
                self.assertEqual(
                    (None, False),
                    publications.stage_terminal(lifecycle_token, fact),
                )
                selected = publications.select_coalesced_terminal(
                    lifecycle_token,
                    start,
                )
                if (
                    profile.response_lifecycle_kind
                    == CommandFeedbackLifecycleKindProfile.ASYNCHRONOUS_IMMEDIATE
                ):
                    self.assertIs(fact, selected)
                    resolution = publications.acknowledge(start, True)
                    self.assertIsNone(resolution.terminal_response)
                else:
                    self.assertIsNone(selected)
                    resolution = publications.acknowledge(start, True)
                    self.assertIs(fact, resolution.terminal_response)
                self.assertTrue(resolution.accepted)
                self.assertTrue(resolution.retire_lifecycle)


def _decode(factory: CommandFeedbackDescriptorFactory, command: str):
    return factory.decode_registered_command_name_only(
        command,
        command_source="lavi_gui",
        event_id="d" * 32,
        provider_id="minecraft_fabric_chatclef_ui",
        event_kind="minecraft_raw_gui_submit",
    )


if __name__ == "__main__":
    unittest.main()
