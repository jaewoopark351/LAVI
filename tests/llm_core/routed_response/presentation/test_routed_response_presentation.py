#20260907_kpopmodder: Verify routed presentation remains isolated from output and generation policy.
from __future__ import annotations

import unittest

from llm_core.routed_response import (
    RoutedExternalResponsePublisher,
    RoutedResponseNonPreemptingDeliveryPolicy,
    RoutedResponsePresentationMetadata,
    RoutedResponseUiPresentationIdentity,
    RoutedResponseUiPresentationQueue,
)


class RoutedResponsePresentationTests(unittest.TestCase):
    def test_command_detail_is_ui_only_and_never_enters_output_tts_payload(self):
        detail_log = (
            '{"command_name":"get","form_kind":"target_count",'
            '"requested_count":1,"target":"diamond_pickaxe"}'
        )
        outputs = []
        ui_queue = RoutedResponseUiPresentationQueue()
        publisher = _publisher(outputs=outputs, ui_queue=ui_queue)

        emission = publisher.emit_external_response(
            "다이아 곡괭이 다 만들었어",
            event_id="0" * 32,
            route_kind="command_lifecycle",
            response_kind="command_terminal",
            presentation_metadata=(
                RoutedResponsePresentationMetadata.minecraft(
                    detail_log=detail_log,
                )
            ),
            send_ui=True,
        )

        self.assertTrue(emission.presentation_receipt.accepted)
        self.assertEqual(1, len(outputs))
        self.assertNotIn("[Minecraft]", outputs[0]["text"])
        self.assertNotIn("diamond_pickaxe", repr(outputs[0]))
        self.assertNotIn("log", outputs[0]["presentation"])
        pending = ui_queue.snapshot()[1]
        self.assertEqual(1, len(pending))
        self.assertEqual("Minecraft", pending[0].metadata["title"])
        self.assertEqual(detail_log, pending[0].metadata["log"])
        self.assertTrue(
            pending[0].metadata["id"].startswith(
                RoutedResponseUiPresentationIdentity.TOKEN_PREFIX
            )
        )
        self.assertNotIn("diamond_pickaxe", pending[0].content)

    def test_non_preempting_terminal_does_not_begin_global_generation(self):
        generations = []
        outputs = []
        ui_queue = RoutedResponseUiPresentationQueue()
        publisher = RoutedExternalResponsePublisher(
            begin_generation_callback=lambda: generations.append(1) or 1,
            build_output_payload_callback=lambda text, generation: {
                "text": text,
                "response_generation": generation,
            },
            send_output_callback=outputs.append,
            send_full_output_callback=lambda _text: None,
            ui_presentation_callback=ui_queue.enqueue,
        )

        emission = publisher.emit_external_response(
            "다 만들었어",
            event_id="c" * 32,
            route_kind="crafting_lifecycle",
            response_kind="crafting_terminal",
            delivery_mode=(
                RoutedResponseNonPreemptingDeliveryPolicy.NON_PREEMPTING
            ),
            presentation_metadata=RoutedResponsePresentationMetadata.minecraft(),
            send_ui=True,
        )

        self.assertEqual([], generations)
        self.assertIsNone(emission.response_generation)
        self.assertEqual("non_preempting", emission.delivery_mode)
        self.assertTrue(emission.presentation_receipt.accepted)
        self.assertEqual(None, outputs[0]["response_generation"])
        self.assertEqual("non_preempting", outputs[0]["delivery_mode"])
        self.assertEqual("minecraft_chatclef", outputs[0]["source"])
        self.assertEqual("Minecraft", outputs[0]["presentation"]["badge_label"])
        self.assertNotIn("[Minecraft]", outputs[0]["text"])

    def test_ordinary_routed_output_payload_remains_unchanged(self):
        outputs = []
        publisher = RoutedExternalResponsePublisher(
            begin_generation_callback=lambda: 4,
            build_output_payload_callback=lambda text, generation: {
                "text": text,
                "response_generation": generation,
            },
            send_output_callback=outputs.append,
            send_full_output_callback=lambda _text: None,
        )

        emission = publisher.emit_external_response("ordinary")

        self.assertEqual(4, emission.response_generation)
        self.assertEqual(
            [{"text": "ordinary", "response_generation": 4}],
            outputs,
        )


def _publisher(*, outputs, ui_queue):
    return RoutedExternalResponsePublisher(
        begin_generation_callback=lambda: 1,
        build_output_payload_callback=lambda text, generation: {
            "text": text,
            "response_generation": generation,
        },
        send_output_callback=outputs.append,
        send_full_output_callback=lambda _text: None,
        ui_presentation_callback=ui_queue.enqueue,
    )


if __name__ == "__main__":
    unittest.main()
