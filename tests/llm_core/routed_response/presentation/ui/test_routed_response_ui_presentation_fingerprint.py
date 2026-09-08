#20260908_kpopmodder: Verify strict canonical fingerprints for Gradio UI history.
from __future__ import annotations

import unittest

from llm_core.routed_response import RoutedResponseUiPresentationIdentity
from llm_core.routed_response.presentation.ui.routed_response_ui_presentation_fingerprint import (
    RoutedResponseUiPresentationFingerprint,
)


class RoutedResponseUiPresentationFingerprintTests(unittest.TestCase):
    def test_plain_text_and_gradio_text_block_have_the_same_fingerprint(self):
        identity = _identity()
        metadata = {"title": "Minecraft", "id": identity.token}
        plain = {
            "role": "assistant",
            "content": "다이아 곡괭이 다 만들었어",
            "metadata": metadata,
        }
        normalized = {
            "role": "assistant",
            "content": [
                {
                    "text": "다이아 곡괭이 다 만들었어",
                    "type": "text",
                }
            ],
            "metadata": metadata,
        }

        plain_fingerprint = (
            RoutedResponseUiPresentationFingerprint.from_message(plain)
        )
        normalized_fingerprint = (
            RoutedResponseUiPresentationFingerprint.from_message(normalized)
        )

        self.assertIsNotNone(plain_fingerprint)
        self.assertEqual(plain_fingerprint, normalized_fingerprint)

    def test_unknown_or_malformed_content_does_not_confirm_presentation(self):
        identity = _identity()
        metadata = {"title": "Minecraft", "id": identity.token}
        invalid_messages = (
            {"role": "assistant", "content": [], "metadata": metadata},
            {
                "role": "assistant",
                "content": [{"type": "video", "text": "done"}],
                "metadata": metadata,
            },
            {
                "role": "assistant",
                "content": [{"type": "text", "text": 1}],
                "metadata": metadata,
            },
            {
                "role": "assistant",
                "content": [
                    {"type": "text", "text": "one"},
                    {"type": "text", "text": "two"},
                ],
                "metadata": metadata,
            },
            {"role": "assistant", "content": "done", "metadata": {}},
            {"role": "user", "content": "done", "metadata": metadata},
        )

        for message in invalid_messages:
            with self.subTest(message=message):
                self.assertIsNone(
                    RoutedResponseUiPresentationFingerprint.from_message(
                        message
                    )
                )

    def test_role_title_and_body_are_confirmation_integrity_fields(self):
        identity = _identity()
        expected = _message(identity, title="Minecraft", content="done")
        wrong_title = _message(identity, title="Other", content="done")
        wrong_body = _message(identity, title="Minecraft", content="different")
        wrong_role = dict(expected, role="user")

        expected_fingerprint = (
            RoutedResponseUiPresentationFingerprint.from_message(expected)
        )
        self.assertNotEqual(
            expected_fingerprint,
            RoutedResponseUiPresentationFingerprint.from_message(wrong_title),
        )
        self.assertNotEqual(
            expected_fingerprint,
            RoutedResponseUiPresentationFingerprint.from_message(wrong_body),
        )
        self.assertIsNone(
            RoutedResponseUiPresentationFingerprint.from_message(wrong_role)
        )

    def test_primary_identity_can_be_read_without_accepting_malformed_body(self):
        identity = _identity()
        malformed = _message(
            identity,
            title="Minecraft",
            content=[{"type": "video", "text": "done"}],
        )

        self.assertIsNone(
            RoutedResponseUiPresentationFingerprint.from_message(malformed)
        )
        self.assertEqual(
            identity,
            RoutedResponseUiPresentationFingerprint.identity_from_message(
                malformed
            ),
        )


def _identity():
    return RoutedResponseUiPresentationIdentity.from_response(
        event_id="a" * 32,
        route_kind="command_lifecycle",
        response_kind="command_terminal",
        response_source="minecraft_chatclef",
        source_kind="minecraft",
        badge_label="Minecraft",
    )


def _message(identity, *, title, content):
    return {
        "role": "assistant",
        "content": content,
        "metadata": {"title": title, "id": identity.token},
    }


if __name__ == "__main__":
    unittest.main()
