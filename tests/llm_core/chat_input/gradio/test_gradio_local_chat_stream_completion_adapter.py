#20260908_kpopmodder: Verifies the Gradio-only completion contract for empty streams.
import unittest

from llm_core.chat_input.gradio import GradioLocalChatStreamCompletionAdapter


class GradioLocalChatStreamCompletionAdapterTests(unittest.TestCase):
    def test_passes_through_every_product_chunk_unchanged(self):
        first = object()
        second = object()

        def predict(_message, _history, _system_prompt):
            yield first
            yield second

        outputs = list(self._adapter(predict).predict("hello", [], "system"))

        self.assertEqual(2, len(outputs))
        self.assertIs(first, outputs[0])
        self.assertIs(second, outputs[1])

    def test_emits_one_invisible_completion_after_normal_empty_stream(self):
        def predict(_message, _history, _system_prompt):
            return
            yield

        first_outputs = list(
            self._adapter(predict).predict("STOP", [], "system")
        )
        second_outputs = list(
            self._adapter(predict).predict("STOP", [], "system")
        )

        self.assertEqual([[]], first_outputs)
        self.assertEqual([[]], second_outputs)
        self.assertIsNot(first_outputs[0], second_outputs[0])

    def test_propagates_inner_exception_without_completion_value(self):
        expected_error = RuntimeError("prediction failed")

        def predict(_message, _history, _system_prompt):
            raise expected_error
            yield

        stream = self._adapter(predict).predict("hello", [], "system")

        with self.assertRaises(RuntimeError) as raised:
            next(stream)

        self.assertIs(expected_error, raised.exception)

    def test_propagates_exception_after_product_chunk_without_completion_value(self):
        expected_error = RuntimeError("prediction failed after one chunk")

        def predict(_message, _history, _system_prompt):
            yield "first"
            raise expected_error

        stream = self._adapter(predict).predict("hello", [], "system")

        self.assertEqual("first", next(stream))
        with self.assertRaises(RuntimeError) as raised:
            next(stream)

        self.assertIs(expected_error, raised.exception)

    def test_close_propagates_generator_exit_and_closes_inner_stream(self):
        inner_closed = []

        def predict(_message, _history, _system_prompt):
            try:
                yield "first"
                yield "second"
            finally:
                inner_closed.append(True)

        stream = self._adapter(predict).predict("hello", [], "system")
        self.assertEqual("first", next(stream))

        stream.close()

        self.assertEqual([True], inner_closed)
        with self.assertRaises(StopIteration):
            next(stream)

    def test_rejects_non_generator_callback(self):
        with self.assertRaisesRegex(
            TypeError,
            "prediction_callback must be a generator function",
        ):
            GradioLocalChatStreamCompletionAdapter(
                prediction_callback=lambda *_args: "not a stream",
            )

    @staticmethod
    def _adapter(prediction_callback):
        return GradioLocalChatStreamCompletionAdapter(
            prediction_callback=prediction_callback,
        )


__all__ = ["GradioLocalChatStreamCompletionAdapterTests"]


if __name__ == "__main__":
    unittest.main()
