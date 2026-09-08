#20260908_kpopmodder: Verify Gradio rendering preserves presentation identity and UI-only detail.
from __future__ import annotations

import unittest
from dataclasses import FrozenInstanceError

import gradio as gr

from llm_core.routed_response import (
    RoutedExternalResponsePublisher,
    RoutedResponsePresentationMetadata,
    RoutedResponseUiPresentationAdapter,
    RoutedResponseUiPresentationIdentity,
    RoutedResponseUiPresentationQueue,
)


class RoutedResponseUiPresentationAdapterTests(unittest.TestCase):
    def test_adapter_renders_body_badge_detail_and_presentation_token(self):
        metadata = RoutedResponsePresentationMetadata.minecraft(
            detail_log='{"command_name":"get"}'
        )
        identity = _identity()

        message = RoutedResponseUiPresentationAdapter().render(
            "다이아 곡괭이 다 만들었어",
            metadata,
            presentation_identity=identity,
        )

        self.assertEqual("다이아 곡괭이 다 만들었어", message.content)
        self.assertNotIn("[Minecraft]", message.content)
        self.assertEqual("Minecraft", message.metadata["title"])
        self.assertEqual(identity.token, message.metadata["id"])
        self.assertEqual(
            '{"command_name":"get"}',
            message.metadata["log"],
        )
        with self.assertRaises(FrozenInstanceError):
            metadata.badge_label = "changed"

    def test_real_gradio_round_trip_preserves_supported_identity_metadata(self):
        metadata = RoutedResponsePresentationMetadata.minecraft(
            detail_log='{"command_name":"get"}'
        )
        identity = _identity()
        message = RoutedResponseUiPresentationAdapter().render(
            "다이아 곡괭이 다 만들었어",
            metadata,
            presentation_identity=identity,
        )

        normalized = _round_trip([message])[0]

        self.assertEqual(
            [{"text": "다이아 곡괭이 다 만들었어", "type": "text"}],
            normalized["content"],
        )
        self.assertEqual(identity.token, normalized["metadata"]["id"])
        self.assertEqual("Minecraft", normalized["metadata"]["title"])
        self.assertEqual(
            '{"command_name":"get"}',
            normalized["metadata"]["log"],
        )

    def test_sink_receipt_keeps_input_event_id_separate_from_browser_token(self):
        event_id = "b" * 32
        queue = RoutedResponseUiPresentationQueue()
        publisher = RoutedExternalResponsePublisher(
            begin_generation_callback=lambda: 1,
            build_output_payload_callback=lambda text, generation: {
                "text": text,
                "response_generation": generation,
            },
            send_output_callback=lambda _payload: None,
            send_full_output_callback=lambda _text: None,
            ui_presentation_callback=queue.enqueue,
        )

        emission = publisher.emit_external_response(
            "다 만들었어",
            event_id=event_id,
            route_kind="command_lifecycle",
            response_kind="command_terminal",
            presentation_metadata=RoutedResponsePresentationMetadata.minecraft(),
            send_ui=True,
        )

        browser_token = queue.snapshot()[1][0].metadata["id"]
        self.assertEqual(event_id, emission.presentation_receipt.event_id)
        self.assertNotEqual(event_id, browser_token)
        self.assertEqual(_identity(event_id=event_id).token, browser_token)

    def test_disabled_rejected_and_throwing_callbacks_return_bounded_receipts(self):
        callbacks = (
            (None, "disabled"),
            (lambda _message: False, "delivery_failed"),
            (_raise_delivery_failure, "delivery_failed"),
        )

        for callback, expected_reason in callbacks:
            with self.subTest(expected_reason=expected_reason):
                publisher = RoutedExternalResponsePublisher(
                    begin_generation_callback=lambda: 1,
                    build_output_payload_callback=lambda text, generation: {
                        "text": text,
                        "response_generation": generation,
                    },
                    send_output_callback=lambda _payload: None,
                    send_full_output_callback=lambda _text: None,
                    ui_presentation_callback=callback,
                )

                emission = publisher.emit_external_response(
                    "응답",
                    event_id="c" * 32,
                    route_kind="command_lifecycle",
                    response_kind="command_terminal",
                    presentation_metadata=(
                        RoutedResponsePresentationMetadata.minecraft()
                    ),
                    send_ui=True,
                )

                self.assertFalse(emission.presentation_receipt.accepted)
                self.assertEqual(
                    expected_reason,
                    emission.presentation_receipt.reason,
                )
                self.assertEqual(
                    "c" * 32,
                    emission.presentation_receipt.event_id,
                )


def _identity(**overrides):
    values = {
        "event_id": "a" * 32,
        "route_kind": "command_lifecycle",
        "response_kind": "command_terminal",
        "response_source": "minecraft_chatclef",
        "source_kind": "minecraft",
        "badge_label": "Minecraft",
    }
    values.update(overrides)
    return RoutedResponseUiPresentationIdentity.from_response(**values)


def _round_trip(history):
    chatbot = gr.Chatbot()
    postprocessed = chatbot.postprocess(history)
    browser_round_trip = type(postprocessed).model_validate_json(
        postprocessed.model_dump_json()
    )
    return chatbot.preprocess(browser_round_trip)


def _raise_delivery_failure(_message):
    raise RuntimeError("delivery failed")


if __name__ == "__main__":
    unittest.main()
