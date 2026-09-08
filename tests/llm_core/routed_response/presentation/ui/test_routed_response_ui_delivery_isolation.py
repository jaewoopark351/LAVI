#20260908_kpopmodder: Verify UI confirmation retries cannot replay output or TTS listeners.
from __future__ import annotations

import unittest

import gradio as gr

from llm_core.output import LlmOutputListenerRegistry
from llm_core.routed_response import (
    RoutedExternalResponsePublisher,
    RoutedResponsePresentationMetadata,
    RoutedResponseUiPresentationDrain,
    RoutedResponseUiPresentationQueue,
)


class RoutedResponseUiDeliveryIsolationTests(unittest.TestCase):
    def test_ui_retry_and_ack_do_not_reinvoke_output_or_tts_listener(self):
        output_payloads = []
        tts_payloads = []
        output_registry = LlmOutputListenerRegistry()
        output_registry.add(output_payloads.append)
        output_registry.add(tts_payloads.append)
        queue = RoutedResponseUiPresentationQueue()
        publisher = RoutedExternalResponsePublisher(
            begin_generation_callback=lambda: 1,
            build_output_payload_callback=lambda text, generation: {
                "text": text,
                "response_generation": generation,
            },
            send_output_callback=output_registry.send_output,
            send_full_output_callback=lambda _text: None,
            ui_presentation_callback=queue.enqueue,
        )

        emission = publisher.emit_external_response(
            "다이아 곡괭이 다 만들었어",
            event_id="6" * 32,
            route_kind="command_lifecycle",
            response_kind="command_terminal",
            presentation_metadata=RoutedResponsePresentationMetadata.minecraft(),
            send_ui=True,
        )
        drain = RoutedResponseUiPresentationDrain(queue)
        first_proposal = drain.append_to_history([])
        retry_proposal = drain.append_to_history([])
        chatbot = gr.Chatbot()
        confirmed = chatbot.preprocess(chatbot.postprocess(retry_proposal))
        acknowledged = drain.append_to_history(confirmed)
        later_callbacks = [drain.append_to_history(confirmed) for _ in range(8)]

        self.assertTrue(emission.presentation_receipt.accepted)
        self.assertEqual(first_proposal, retry_proposal)
        self.assertEqual(gr.skip(), acknowledged)
        self.assertTrue(all(result == gr.skip() for result in later_callbacks))
        self.assertEqual(1, len(output_payloads))
        self.assertEqual(1, len(tts_payloads))
        self.assertEqual(output_payloads, tts_payloads)
        self.assertEqual((), queue.snapshot()[1])


if __name__ == "__main__":
    unittest.main()
