#20260908_kpopmodder: Verify bounded FIFO and epoch-owned UI presentation queue behavior.
from __future__ import annotations

import unittest

from llm_core.routed_response import RoutedResponseUiPresentationQueue


class RoutedResponseUiPresentationQueueTests(unittest.TestCase):
    def test_capacity_rejects_new_item_without_overwriting_fifo(self):
        queue = RoutedResponseUiPresentationQueue(capacity=2)
        initial_epoch, _pending = queue.snapshot()

        self.assertTrue(queue.enqueue("first"))
        self.assertTrue(queue.enqueue("second"))
        self.assertFalse(queue.enqueue("overflow"))

        epoch, pending = queue.snapshot()
        self.assertEqual(initial_epoch, epoch)
        self.assertEqual(("first", "second"), pending)

    def test_only_exact_fifo_prefix_can_be_acknowledged(self):
        queue = RoutedResponseUiPresentationQueue()
        queue.enqueue("first")
        queue.enqueue("second")
        epoch, _pending = queue.snapshot()

        self.assertFalse(
            queue.acknowledge_presented(epoch=epoch, items=("second",))
        )
        self.assertEqual(("first", "second"), queue.snapshot()[1])
        self.assertTrue(
            queue.acknowledge_presented(epoch=epoch, items=("first",))
        )
        self.assertEqual(("second",), queue.snapshot()[1])

    def test_stale_or_repeated_acknowledgement_is_rejected(self):
        queue = RoutedResponseUiPresentationQueue()
        queue.enqueue("first")
        queue.enqueue("second")
        epoch, _pending = queue.snapshot()

        self.assertTrue(
            queue.acknowledge_presented(epoch=epoch, items=("first",))
        )
        self.assertFalse(
            queue.acknowledge_presented(epoch=epoch, items=("first",))
        )
        self.assertEqual(("second",), queue.snapshot()[1])

    def test_clear_advances_epoch_and_invalidates_stale_acknowledgement(self):
        queue = RoutedResponseUiPresentationQueue()
        queue.enqueue("old")
        old_epoch, _pending = queue.snapshot()

        queue.clear()
        queue.enqueue("new")

        self.assertFalse(
            queue.acknowledge_presented(
                epoch=old_epoch,
                items=("old",),
            )
        )
        new_epoch, pending = queue.snapshot()
        self.assertGreater(new_epoch, old_epoch)
        self.assertEqual(("new",), pending)


if __name__ == "__main__":
    unittest.main()
