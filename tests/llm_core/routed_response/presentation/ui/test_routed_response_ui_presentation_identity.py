#20260908_kpopmodder: Verify deterministic immutable lifecycle presentation identity.
from __future__ import annotations

import unittest
from dataclasses import FrozenInstanceError

from llm_core.routed_response import RoutedResponseUiPresentationIdentity


class RoutedResponseUiPresentationIdentityTests(unittest.TestCase):
    def test_identity_is_immutable_versioned_bounded_and_deterministic(self):
        first = _identity()
        second = _identity()

        self.assertEqual(first, second)
        self.assertTrue(
            first.token.startswith(
                RoutedResponseUiPresentationIdentity.TOKEN_PREFIX
            )
        )
        self.assertEqual(
            len(RoutedResponseUiPresentationIdentity.TOKEN_PREFIX) + 64,
            len(first.token),
        )
        with self.assertRaises(FrozenInstanceError):
            first.token = "changed"

    def test_each_lifecycle_component_changes_the_presentation_token(self):
        baseline = {
            "event_id": "a" * 32,
            "route_kind": "command_lifecycle",
            "response_kind": "command_start",
            "response_source": "minecraft_chatclef",
            "source_kind": "minecraft",
            "badge_label": "Minecraft",
        }
        changes = {
            "event_id": "b" * 32,
            "route_kind": "crafting_lifecycle",
            "response_kind": "command_terminal",
            "response_source": "another_source",
            "source_kind": "another_kind",
            "badge_label": "Another badge",
        }
        baseline_token = RoutedResponseUiPresentationIdentity.from_response(
            **baseline
        ).token

        for field, value in changes.items():
            with self.subTest(field=field):
                changed = dict(baseline)
                changed[field] = value
                self.assertNotEqual(
                    baseline_token,
                    RoutedResponseUiPresentationIdentity.from_response(
                        **changed
                    ).token,
                )

    def test_canonical_encoding_does_not_have_delimiter_collisions(self):
        first = _identity(event_id="a|b", route_kind="c")
        second = _identity(event_id="a", route_kind="b|c")

        self.assertNotEqual(first.token, second.token)

    def test_only_versioned_exact_tokens_can_be_restored(self):
        identity = _identity()

        self.assertEqual(
            identity,
            RoutedResponseUiPresentationIdentity.from_token(identity.token),
        )
        for invalid in (
            None,
            "a" * 32,
            identity.token[:-1],
            identity.token[:-1] + "G",
        ):
            with self.subTest(invalid=invalid):
                self.assertIsNone(
                    RoutedResponseUiPresentationIdentity.from_token(invalid)
                )


def _identity(**overrides):
    values = {
        "event_id": "a" * 32,
        "route_kind": "command_lifecycle",
        "response_kind": "command_start",
        "response_source": "minecraft_chatclef",
        "source_kind": "minecraft",
        "badge_label": "Minecraft",
    }
    values.update(overrides)
    return RoutedResponseUiPresentationIdentity.from_response(**values)


if __name__ == "__main__":
    unittest.main()
