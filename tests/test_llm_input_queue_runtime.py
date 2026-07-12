#20260706_kpopmodder: Added focused tests for LLM input queue runtime extraction.
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from llm_core.input_queue_worker import LLMInputQueueWorker


class LLMInputQueueRuntimeTests(unittest.TestCase):
    def test_generate_response_drains_queue_and_updates_ui(self):
        calls = []
        updates = []

        def response_callback(message, history, system_prompt):
            calls.append((message, history, system_prompt))
            yield "partial"
            yield "done"

        worker = LLMInputQueueWorker(
            response_callback=response_callback,
            history_callback=lambda: [["old", "answer"]],
            system_prompt_callback=lambda: "system",
            queue_updated_callback=lambda: updates.append("updated"),
        )
        worker.input_queue.put("hello")

        worker.generate_response()

        self.assertTrue(worker.input_queue.empty())
        self.assertEqual([("hello", [["old", "answer"]], "system")], calls)
        self.assertEqual(["updated"], updates)

    def test_process_input_queue_does_not_start_second_live_thread(self):
        worker = LLMInputQueueWorker(
            response_callback=lambda *_args: iter(()),
            history_callback=lambda: [],
            system_prompt_callback=lambda: "",
            queue_updated_callback=lambda: None,
        )

        class LiveThread:
            def is_alive(self):
                return True

        live_thread = LiveThread()
        worker.input_process_thread = live_thread

        worker.process_input_queue()

        self.assertIs(live_thread, worker.input_process_thread)


if __name__ == "__main__":
    unittest.main()
