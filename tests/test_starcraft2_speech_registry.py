#20260711_kpopmodder: Verifies every configured SC2 speech term reaches the
# structured telemetry or log-parser TTS path without a generic fallback.
import unittest

from plugins.StarCraft2.sc2_event_parser import SC2EventParser
from plugins.StarCraft2.sc2_speech_terms import (
    SC2_STRATEGY_SPEAK_NAMES,
    SC2_UNIT_SPEAK_NAMES,
    SC2_UPGRADE_SPEAK_NAMES,
)
from plugins.StarCraft2.sc2_telemetry_registry import (
    SC2_BUILDING_CATEGORY,
    SC2_BUILDING_UNIT_TYPE_IDS,
    SC2_UNIT_CATEGORY,
    SC2_UNIT_TYPE_ID_BY_TOKEN,
    SC2_UNIT_TYPE_IDS,
    SC2_UPGRADE_ID_BY_TOKEN,
    canonical_unit_token,
    canonical_upgrade_token,
    unit_category,
    unit_speak_name,
    upgrade_speak_name,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_reaction_policy import (
    StarCraft2ReactionPolicy,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_reaction_runtime import (
    build_starcraft2_reaction_text,
)


class StarCraft2SpeechRegistryTests(unittest.TestCase):
    def test_registry_covers_all_126_configured_speech_terms(self):
        self.assertEqual(99, len(SC2_UNIT_SPEAK_NAMES))
        self.assertEqual(18, len(SC2_UPGRADE_SPEAK_NAMES))
        self.assertEqual(9, len(SC2_STRATEGY_SPEAK_NAMES))
        self.assertEqual(
            126,
            len(SC2_UNIT_SPEAK_NAMES)
            + len(SC2_UPGRADE_SPEAK_NAMES)
            + len(SC2_STRATEGY_SPEAK_NAMES),
        )
        self.assertEqual(54, len(SC2_UNIT_TYPE_IDS))
        self.assertEqual(45, len(SC2_BUILDING_UNIT_TYPE_IDS))

    def test_all_unit_and_building_ids_render_configured_korean_names(self):
        for token, spoken_name in SC2_UNIT_SPEAK_NAMES.items():
            with self.subTest(token=token):
                unit_type_id = SC2_UNIT_TYPE_ID_BY_TOKEN[token]
                category = unit_category(unit_type_id)
                event_type = (
                    "building_started"
                    if category == SC2_BUILDING_CATEGORY
                    else "unit_produced"
                )
                event = {
                    "event_type": event_type,
                    "details": {
                        "unit_type_id": str(unit_type_id),
                        "unit_token": token,
                        "count": 1,
                        "unit_changes": {str(unit_type_id): 1},
                    },
                }

                self.assertEqual(token, canonical_unit_token(unit_type_id))
                self.assertIn(category, {SC2_UNIT_CATEGORY, SC2_BUILDING_CATEGORY})
                self.assertEqual(spoken_name, unit_speak_name(unit_type_id))
                text = build_starcraft2_reaction_text(event)
                self.assertIn(spoken_name, text)
                self.assertNotIn("새 건물", text)
                self.assertNotIn("내가 유닛", text)

    def test_all_unit_and_building_types_get_independent_cooldowns(self):
        policy = StarCraft2ReactionPolicy(min_interval_sec=8)
        events = []
        for token, unit_type_id in SC2_UNIT_TYPE_ID_BY_TOKEN.items():
            category = unit_category(unit_type_id)
            events.append({
                "event_type": (
                    "building_started"
                    if category == SC2_BUILDING_CATEGORY
                    else "unit_produced"
                ),
                "details": {
                    "unit_type_id": str(unit_type_id),
                    "unit_token": token,
                    "unit_changes": {str(unit_type_id): 1},
                },
            })

        for event in events:
            with self.subTest(event=event):
                self.assertTrue(policy.should_emit(event))
        self.assertFalse(policy.should_emit(events[0]))

    def test_all_upgrade_ids_render_configured_korean_names(self):
        for token, spoken_name in SC2_UPGRADE_SPEAK_NAMES.items():
            with self.subTest(token=token):
                upgrade_id = SC2_UPGRADE_ID_BY_TOKEN[token]
                self.assertEqual(token, canonical_upgrade_token(upgrade_id))
                self.assertEqual(spoken_name, upgrade_speak_name(upgrade_id))
                text = build_starcraft2_reaction_text({
                    "event_type": "upgrade_completed",
                    "details": {
                        "upgrade_id": str(upgrade_id),
                        "upgrade_token": token,
                    },
                })
                self.assertIn(spoken_name, text)

                parsed = SC2EventParser().parse_event(
                    f"Upgrade queue: [UpgradeId.{token}]"
                )
                self.assertIsNotNone(parsed)
                self.assertIn(spoken_name, parsed.message)

    def test_all_strategy_tokens_render_configured_korean_names(self):
        for token, spoken_name in SC2_STRATEGY_SPEAK_NAMES.items():
            with self.subTest(token=token):
                parsed = SC2EventParser().parse_event(
                    f"Chosen opening: {token}"
                )
                self.assertIsNotNone(parsed)
                self.assertEqual("strategy", parsed.category)
                self.assertIn(spoken_name, parsed.message)


if __name__ == "__main__":
    unittest.main()
