#20260908_kpopmodder: Verify direct START and queued terminal share one identity contract without collision.
from __future__ import annotations

import unittest
from types import SimpleNamespace

import gradio as gr

from llm_core.input_routing.yielding import RoutedInputChatUiYieldAdapter
from llm_core.routed_response import (
    RoutedExternalResponsePublisher,
    RoutedResponseEmission,
    RoutedResponsePresentationMetadata,
    RoutedResponseUiPresentationDrain,
    RoutedResponseUiPresentationIdentity,
    RoutedResponseUiPresentationQueue,
)


class RoutedResponseUiCrossPathIdentityTests(unittest.TestCase):
    def test_direct_start_and_async_terminal_each_render_once_for_same_event(self):
        event_id = "5" * 32
        metadata = RoutedResponsePresentationMetadata.minecraft()
        recorder = _ChatUiDeliveryRecorder()
        start_emission = RoutedResponseEmission(
            text="다이아 곡괭이 만들어 줄게",
            source="minecraft_chatclef",
            response_generation=1,
            output_delivered=True,
            full_output_delivered=False,
            history_remembered=False,
            event_id=event_id,
            route_kind="command_lifecycle",
            response_kind="command_start",
            presentation_metadata=metadata,
        )
        start_message = RoutedInputChatUiYieldAdapter(
            lambda: recorder
        ).adapt(
            SimpleNamespace(source="lavi_chat_ui"),
            start_emission,
        )
        queue = RoutedResponseUiPresentationQueue()
        terminal_emission = _publisher(queue).emit_external_response(
            "다이아 곡괭이 다 만들었어",
            event_id=event_id,
            route_kind="command_lifecycle",
            response_kind="command_terminal",
            presentation_metadata=metadata,
            send_ui=True,
        )
        terminal_message = queue.snapshot()[1][0]

        self.assertEqual(
            _identity(event_id, "command_start").token,
            start_message.metadata["id"],
        )
        self.assertEqual(
            _identity(event_id, "command_terminal").token,
            terminal_message.metadata["id"],
        )
        self.assertNotEqual(
            start_message.metadata["id"],
            terminal_message.metadata["id"],
        )
        self.assertEqual(event_id, recorder.emissions[0].presentation_receipt.event_id)
        self.assertEqual(event_id, terminal_emission.presentation_receipt.event_id)

        drain = RoutedResponseUiPresentationDrain(queue)
        proposed = drain.append_to_history([start_message])
        self.assertEqual(2, len(proposed))
        chatbot = gr.Chatbot()
        confirmed = chatbot.preprocess(chatbot.postprocess(proposed))
        self.assertEqual(gr.skip(), drain.append_to_history(confirmed))
        self.assertEqual((), queue.snapshot()[1])
        self.assertEqual(2, len(confirmed))


class _ChatUiDeliveryRecorder:
    def __init__(self):
        self.emissions = []

    def log_chat_ui_delivery(self, emission, *, delivered, reason):
        self.emissions.append(emission)
        return delivered is True and reason == "yielded"


def _identity(event_id, response_kind):
    return RoutedResponseUiPresentationIdentity.from_response(
        event_id=event_id,
        route_kind="command_lifecycle",
        response_kind=response_kind,
        response_source="minecraft_chatclef",
        source_kind="minecraft",
        badge_label="Minecraft",
    )


def _publisher(queue):
    return RoutedExternalResponsePublisher(
        begin_generation_callback=lambda: 2,
        build_output_payload_callback=lambda text, generation: {
            "text": text,
            "response_generation": generation,
        },
        send_output_callback=lambda _payload: None,
        send_full_output_callback=lambda _text: None,
        ui_presentation_callback=queue.enqueue,
    )


if __name__ == "__main__":
    unittest.main()
