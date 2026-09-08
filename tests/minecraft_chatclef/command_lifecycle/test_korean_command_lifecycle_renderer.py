#20260907_kpopmodder: Snapshot natural Korean lifecycle grammar across profile families.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)
from plugins.Minecraft.fabric.chatclef.result.store_home import (
    StoreHomeTerminalPayload,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle.grammar import (
    KoreanParticleRenderer,
)


class KoreanCommandLifecycleRendererTests(unittest.TestCase):
    def setUp(self) -> None:
        self.renderer = CommandLifecycleResponseRenderer()

    def test_get_preserves_intent_verb_but_not_unproven_mining_route(self):
        craft = _descriptor(
            "get",
            family="item_get",
            label="다이아 곡괭이",
            target="diamond_pickaxe",
            count=1,
            verb="craft",
        )
        acquire = _descriptor(
            "get",
            family="item_get",
            label="철",
            target="iron_ingot",
            count=3,
            verb="acquire",
        )
        mining = _descriptor(
            "get",
            family="item_get",
            label="석탄",
            target="coal",
            count=2,
            verb="mining",
        )

        self.assertEqual("다이아 곡괭이 만들어 줄게", self.renderer.render_start(craft))
        self.assertEqual("다이아 곡괭이 만드는 중이야", _status(self.renderer, craft))
        self.assertEqual("다이아 곡괭이 다 만들었어", _terminal(self.renderer, craft, verified=True))
        self.assertEqual("철 3개 구해 올게", self.renderer.render_start(acquire))
        self.assertEqual("철 3개 구하는 중이야", _status(self.renderer, acquire))
        self.assertEqual("석탄 2개 캐 올게", self.renderer.render_start(mining))
        self.assertEqual("석탄 2개 구하는 중이야", _status(self.renderer, mining))
        self.assertEqual("석탄 2개 다 구했어", _terminal(self.renderer, mining, verified=True))

    def test_food_and_meat_use_hunger_units_and_never_eating_or_item_count(self):
        food = _descriptor(
            "food",
            family="food_acquisition",
            count=8,
        )
        meat = _descriptor(
            "meat",
            family="meat_acquisition",
            count=6,
        )

        values = (
            self.renderer.render_start(food),
            _status(self.renderer, food),
            _terminal(self.renderer, food),
            self.renderer.render_start(meat),
            _status(self.renderer, meat),
            _terminal(self.renderer, meat),
        )
        self.assertEqual("허기를 8만큼 채울 음식을 모아 올게", values[0])
        self.assertEqual("허기를 8만큼 채울 음식을 모으는 중이야", values[1])
        self.assertEqual("허기를 6만큼 채울 고기를 모아 올게", values[3])
        self.assertEqual("허기를 6만큼 채울 고기를 모으는 중이야", values[4])
        for value in values:
            self.assertNotIn("개", value)
            self.assertNotIn("먹", value)

    def test_store_home_means_inventory_organization_not_home_registration(self):
        descriptor = _descriptor("store_home", family="store_home")

        values = (
            self.renderer.render_start(descriptor),
            _status(self.renderer, descriptor),
            _terminal(self.renderer, descriptor),
        )
        self.assertEqual("아이템을 집에 정리할게", values[0])
        self.assertEqual("아이템을 집에 정리하는 중이야", values[1])
        for value in values:
            self.assertNotIn("위치", value)
            self.assertNotIn("등록", value)

    def test_verified_store_home_projection_renders_positive_and_zero_work(self):
        descriptor = _descriptor("store_home", family="store_home")
        positive = _store_home_projection(stored_items=909)
        zero_work = _store_home_projection(stored_items=0)

        self.assertEqual(
            "아이템 909개를 집에 정리했어",
            _terminal(
                self.renderer,
                descriptor,
                verified=True,
                projection=positive,
            ),
        )
        self.assertEqual(
            "집에 정리할 아이템이 없었어",
            _terminal(
                self.renderer,
                descriptor,
                verified=True,
                projection=zero_work,
            ),
        )
        self.assertNotIn(
            positive.reason,
            _terminal(
                self.renderer,
                descriptor,
                verified=True,
                projection=positive,
            ),
        )

    def test_store_home_success_fails_closed_without_exact_completed_projection(self):
        descriptor = _descriptor("store_home", family="store_home")
        cautious = _terminal(self.renderer, descriptor, verified=False)
        completed = _store_home_projection(stored_items=14)
        invalid_projections = (
            None,
            SimpleNamespace(
                result="COMPLETED",
                stored_items=14,
                remaining_stacks=0,
                reason="lookalike",
                goal_satisfied=True,
            ),
            StoreHomeTerminalPayload(
                result="PARTIAL_TRUSTED_CAPACITY_EXHAUSTED",
                stored_items=14,
                remaining_stacks=1,
                reason="trusted_capacity_exhausted",
                goal_satisfied=False,
            ),
            StoreHomeTerminalPayload(
                result="NO_USABLE_TRUSTED_DESTINATION",
                stored_items=0,
                remaining_stacks=1,
                reason="no_usable_destination",
                goal_satisfied=False,
            ),
            StoreHomeTerminalPayload(
                result="COMPLETED",
                stored_items=14,
                remaining_stacks=1,
                reason="inconsistent_remaining",
                goal_satisfied=True,
            ),
            StoreHomeTerminalPayload(
                result="COMPLETED",
                stored_items=14,
                remaining_stacks=0,
                reason="inconsistent_goal",
                goal_satisfied=False,
            ),
            StoreHomeTerminalPayload(
                result="COMPLETED",
                stored_items=False,
                remaining_stacks=0,
                reason="boolean_count",
                goal_satisfied=True,
            ),
            StoreHomeTerminalPayload(
                result="COMPLETED",
                stored_items=-1,
                remaining_stacks=0,
                reason="negative_count",
                goal_satisfied=True,
            ),
            StoreHomeTerminalPayload(
                result="COMPLETED",
                stored_items=0,
                remaining_stacks=False,
                reason="boolean_remaining",
                goal_satisfied=True,
            ),
        )

        self.assertEqual(
            cautious,
            _terminal(
                self.renderer,
                descriptor,
                verified=False,
                projection=completed,
            ),
        )
        for projection in invalid_projections:
            with self.subTest(projection=projection):
                self.assertEqual(
                    cautious,
                    _terminal(
                        self.renderer,
                        descriptor,
                        verified=True,
                        projection=projection,
                    ),
                )

    def test_follow_uses_player_particle_and_persistent_tasks_never_finish_as_success(self):
        follow = _descriptor(
            "follow",
            family="movement_follow",
            player="Steve",
        )

        self.assertEqual("Steve를 따라갈게", self.renderer.render_start(follow))
        self.assertEqual("Steve를 따라가는 중이야", _status(self.renderer, follow))
        for command_name, family in (
            ("follow", "movement_follow"),
            ("idle", "persistent"),
            ("hero", "persistent"),
            ("gamer", "generic"),
        ):
            text = _terminal(
                self.renderer,
                _descriptor(command_name, family=family),
                verified=True,
            )
            with self.subTest(command_name=command_name):
                self.assertNotIn("다 했어", text)
                self.assertNotIn("다 따라갔어", text)
                self.assertNotIn("게임을 깼어", text)
                self.assertIn("확인하지 못했어", text)

    def test_give_stays_cautious_even_if_generic_verified_flag_is_true(self):
        give = _descriptor(
            "give",
            family="item_give",
            label="참나무 원목",
            target="oak_log",
            count=2,
            player="Steve",
        )

        self.assertEqual("Steve에게 참나무 원목 2개 건네줄게", self.renderer.render_start(give))
        terminal = _terminal(self.renderer, give, verified=True)
        self.assertIn("확인하지 못했어", terminal)
        self.assertNotIn("전달했어", terminal)

    def test_typed_area_and_name_only_raw_profiles_do_not_invent_slots(self):
        area = _descriptor("auto_deposit_trust", family="registry")
        raw_area = _descriptor(
            "auto_deposit_trust",
            family="registry",
            detail_level="command_name_only",
        )
        raw_get = _descriptor(
            "get",
            family="item_get",
            detail_level="command_name_only",
        )
        raw_deposit_all = _descriptor(
            "deposit_all",
            family="item_deposit",
            detail_level="command_name_only",
        )

        self.assertEqual(
            "주변 16×16 범위에 있는 보관함을 자동 보관 대상으로 등록할게",
            self.renderer.render_start(area),
        )
        self.assertEqual(
            "자동 보관 대상을 등록할게",
            self.renderer.render_start(raw_area),
        )
        self.assertEqual("요청한 아이템 구해 올게", self.renderer.render_start(raw_get))
        self.assertEqual(
            "요청한 아이템 보관할게",
            self.renderer.render_start(raw_deposit_all),
        )

    def test_every_registered_name_has_natural_name_only_start_and_status(self):
        for command_name in KoreanChatClefCommandRegistry().command_names():
            descriptor = _descriptor(
                command_name,
                family="unused_by_registry_lookup",
                detail_level="command_name_only",
            )
            with self.subTest(command_name=command_name):
                start = self.renderer.render_start(descriptor)
                status = _status(self.renderer, descriptor)
                self.assertTrue(start)
                self.assertTrue(status)
                self.assertNotIn("[Minecraft]", start)
                self.assertNotIn("[Minecraft]", status)
                self.assertNotIn("명령 시작", start)

    def test_particle_rules_cover_hangul_ascii_fallback_and_numerals(self):
        particles = KoreanParticleRenderer()

        self.assertEqual("철을", particles.attach("철", "을", "를"))
        self.assertEqual("사과를", particles.attach("사과", "을", "를"))
        self.assertEqual("Steve를", particles.attach("Steve", "을", "를"))
        self.assertEqual("1을", particles.attach("1", "을", "를"))
        self.assertEqual("2를", particles.attach("2", "을", "를"))
        self.assertEqual("길로", particles.attach_directional("길"))
        self.assertEqual("바다로", particles.attach_directional("바다"))
        self.assertEqual("1로", particles.attach_directional("1"))
        self.assertEqual("7로", particles.attach_directional("7"))
        self.assertEqual("8로", particles.attach_directional("8"))
        self.assertEqual("0으로", particles.attach_directional("0"))
        self.assertEqual("3으로", particles.attach_directional("3"))
        self.assertEqual("6으로", particles.attach_directional("6"))


def _descriptor(
    command_name: str,
    *,
    family: str,
    label: str = "",
    target: str | None = None,
    count: int | None = None,
    verb: str = "",
    player: str = "",
    coordinates: tuple[int, int, int] | None = None,
    detail_level: str = "typed",
):
    return SimpleNamespace(
        command_name=command_name,
        command=command_name,
        requested_family=family,
        spoken_target_label=label,
        target_item=target,
        requested_count=count,
        acquisition_verb_class=verb,
        player_name=player,
        coordinates=coordinates,
        detail_level=detail_level,
    )


def _status(renderer, descriptor):
    return renderer.render_status(
        SimpleNamespace(state="running", descriptor=descriptor)
    )


def _terminal(renderer, descriptor, *, verified=False, projection=None):
    return renderer.render_terminal(
        SimpleNamespace(
            descriptor=descriptor,
            status="completed",
            verified=verified,
            dispatch_started=True,
            evidence_projection=projection,
        )
    )


def _store_home_projection(*, stored_items: int) -> StoreHomeTerminalPayload:
    return StoreHomeTerminalPayload(
        result="COMPLETED",
        stored_items=stored_items,
        remaining_stacks=0,
        reason="private_verified_reason",
        goal_satisfied=True,
    )


if __name__ == "__main__":
    unittest.main()
