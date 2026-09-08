#20260908_kpopmodder: Verify non-lossy exactly-once Gradio UI queue confirmation.
from __future__ import annotations

import unittest

import gradio as gr

from llm_core.routed_response import (
    RoutedResponsePresentationMetadata,
    RoutedResponseUiPresentationAdapter,
    RoutedResponseUiPresentationDrain,
    RoutedResponseUiPresentationIdentity,
    RoutedResponseUiPresentationQueue,
)


class RoutedResponseUiPresentationDrainTests(unittest.TestCase):
    def setUp(self):
        self.chatbot = gr.Chatbot()

    def test_real_gradio_round_trip_acknowledges_once_then_always_skips(self):
        queue = RoutedResponseUiPresentationQueue()
        queue.enqueue(_message("다이아 곡괭이 다 만들었어"))
        drain = RoutedResponseUiPresentationDrain(queue)

        proposed = drain.append_to_history([])
        self.assertEqual(1, len(proposed))
        self.assertEqual(1, len(queue.snapshot()[1]))
        confirmed = self._round_trip(proposed)

        self.assertEqual(gr.skip(), drain.append_to_history(confirmed))
        self.assertEqual((), queue.snapshot()[1])
        for _ in range(10):
            self.assertEqual(gr.skip(), drain.append_to_history(confirmed))
            self.assertEqual(1, len(confirmed))

    def test_failed_first_ui_update_is_retried_without_losing_item(self):
        queue = RoutedResponseUiPresentationQueue()
        queue.enqueue(_message("terminal"))
        drain = RoutedResponseUiPresentationDrain(queue)
        initial = [{"role": "user", "content": "request"}]

        first_proposal = drain.append_to_history(initial)
        retry_proposal = drain.append_to_history(initial)

        self.assertEqual(first_proposal, retry_proposal)
        self.assertEqual(1, len(queue.snapshot()[1]))
        confirmed = self._round_trip(retry_proposal)
        self.assertEqual(gr.skip(), drain.append_to_history(confirmed))
        self.assertEqual((), queue.snapshot()[1])

    def test_same_event_and_body_with_different_response_kind_are_distinct(self):
        queue = RoutedResponseUiPresentationQueue()
        event_id = "c" * 32
        queue.enqueue(
            _message(
                "응답",
                event_id=event_id,
                response_kind="command_start",
            )
        )
        queue.enqueue(
            _message(
                "응답",
                event_id=event_id,
                response_kind="command_terminal",
            )
        )
        drain = RoutedResponseUiPresentationDrain(queue)

        proposed = drain.append_to_history([])

        self.assertEqual(2, len(proposed))
        self.assertNotEqual(
            proposed[0].metadata["id"],
            proposed[1].metadata["id"],
        )
        confirmed = self._round_trip(proposed)
        self.assertEqual(gr.skip(), drain.append_to_history(confirmed))
        self.assertEqual((), queue.snapshot()[1])

    def test_same_body_with_different_event_ids_is_not_text_deduplicated(self):
        queue = RoutedResponseUiPresentationQueue()
        queue.enqueue(_message("같은 문구", event_id="d" * 32))
        queue.enqueue(_message("같은 문구", event_id="e" * 32))
        drain = RoutedResponseUiPresentationDrain(queue)

        proposed = drain.append_to_history([])

        self.assertEqual(2, len(proposed))
        self.assertNotEqual(
            proposed[0].metadata["id"],
            proposed[1].metadata["id"],
        )
        confirmed = self._round_trip(proposed)
        self.assertEqual(gr.skip(), drain.append_to_history(confirmed))
        self.assertEqual((), queue.snapshot()[1])

    def test_only_confirmed_fifo_prefix_is_acknowledged(self):
        queue = RoutedResponseUiPresentationQueue()
        for index in range(3):
            queue.enqueue(_message(f"message-{index}", event_id=str(index)))
        drain = RoutedResponseUiPresentationDrain(queue)
        proposed = drain.append_to_history([])
        confirmed_first = self._round_trip([proposed[0]])

        next_proposal = drain.append_to_history(confirmed_first)

        self.assertEqual(
            ["message-0", "message-1", "message-2"],
            [_body(item) for item in next_proposal],
        )
        self.assertEqual(2, len(queue.snapshot()[1]))
        confirmed_all = self._round_trip(next_proposal)
        self.assertEqual(gr.skip(), drain.append_to_history(confirmed_all))
        self.assertEqual((), queue.snapshot()[1])

    def test_reversed_history_does_not_acknowledge_fifo_prefix(self):
        queue = RoutedResponseUiPresentationQueue()
        queue.enqueue(_message("first", event_id="7" * 32))
        queue.enqueue(_message("second", event_id="8" * 32))
        drain = RoutedResponseUiPresentationDrain(queue)
        proposed = drain.append_to_history([])
        reversed_history = self._round_trip(list(reversed(proposed)))

        self.assertEqual(gr.skip(), drain.append_to_history(reversed_history))
        self.assertEqual(2, len(queue.snapshot()[1]))

    def test_enqueue_overlap_preserves_newer_item_until_its_confirmation(self):
        queue = RoutedResponseUiPresentationQueue()
        queue.enqueue(_message("first", event_id="f" * 32))
        drain = RoutedResponseUiPresentationDrain(queue)
        first_proposal = drain.append_to_history([])
        queue.enqueue(_message("second", event_id="1" * 32))
        confirmed_first = self._round_trip(first_proposal)

        overlap_proposal = drain.append_to_history(confirmed_first)

        self.assertEqual(
            ["first", "second"],
            [_body(item) for item in overlap_proposal],
        )
        self.assertEqual(1, len(queue.snapshot()[1]))
        confirmed_both = self._round_trip(overlap_proposal)
        self.assertEqual(gr.skip(), drain.append_to_history(confirmed_both))
        self.assertEqual((), queue.snapshot()[1])

    def test_clear_prevents_unconfirmed_pre_reset_item_from_reappearing(self):
        queue = RoutedResponseUiPresentationQueue()
        queue.enqueue(_message("pre-reset terminal", event_id="2" * 32))
        drain = RoutedResponseUiPresentationDrain(queue)
        stale_proposal = drain.append_to_history([])

        queue.clear()

        self.assertEqual(gr.skip(), drain.append_to_history([]))
        self.assertEqual(1, len(stale_proposal))
        self.assertEqual((), queue.snapshot()[1])

    def test_same_token_with_wrong_body_is_not_acknowledged_or_duplicated(self):
        queue = RoutedResponseUiPresentationQueue()
        pending = _message("expected", event_id="3" * 32)
        queue.enqueue(pending)
        drain = RoutedResponseUiPresentationDrain(queue)
        conflicting_history = [
            {
                "role": "assistant",
                "content": [{"text": "different", "type": "text"}],
                "metadata": dict(pending.metadata),
            }
        ]

        self.assertEqual(
            gr.skip(),
            drain.append_to_history(conflicting_history),
        )
        self.assertEqual((pending,), queue.snapshot()[1])

    def test_same_token_with_unknown_body_shape_is_not_acknowledged(self):
        queue = RoutedResponseUiPresentationQueue()
        pending = _message("expected", event_id="4" * 32)
        queue.enqueue(pending)
        drain = RoutedResponseUiPresentationDrain(queue)
        malformed_history = [
            {
                "role": "assistant",
                "content": [{"text": "expected", "type": "video"}],
                "metadata": dict(pending.metadata),
            }
        ]

        self.assertEqual(gr.skip(), drain.append_to_history(malformed_history))
        self.assertEqual((pending,), queue.snapshot()[1])

    def _round_trip(self, history):
        postprocessed = self.chatbot.postprocess(history)
        browser_round_trip = type(postprocessed).model_validate_json(
            postprocessed.model_dump_json()
        )
        return self.chatbot.preprocess(browser_round_trip)


def _message(
    text,
    *,
    event_id="a" * 32,
    route_kind="command_lifecycle",
    response_kind="command_terminal",
    response_source="minecraft_chatclef",
    source_kind="minecraft",
):
    metadata = RoutedResponsePresentationMetadata(
        source_kind=source_kind,
        badge_label="Minecraft",
    )
    identity = RoutedResponseUiPresentationIdentity.from_response(
        event_id=event_id,
        route_kind=route_kind,
        response_kind=response_kind,
        response_source=response_source,
        source_kind=source_kind,
        badge_label=metadata.badge_label,
    )
    return RoutedResponseUiPresentationAdapter().render(
        text,
        metadata,
        presentation_identity=identity,
    )


def _body(item):
    content = item.get("content") if isinstance(item, dict) else item.content
    if isinstance(content, list):
        return content[0]["text"]
    return content


if __name__ == "__main__":
    unittest.main()
