#20260905_kpopmodder: Verifies the existing LLM queue transports immutable input events by identity.
import unittest

from input_core.input_event import LaviInputEvent
from llm_core.input_queue_worker import LLMInputQueueWorker


class LaviInputEventQueueContractTests(unittest.TestCase):
    def test_queue_keeps_the_same_event_object(self):
        worker = LLMInputQueueWorker(
            response_callback=lambda *_args: iter(()),
            history_callback=list,
            system_prompt_callback=str,
            queue_updated_callback=lambda: None,
        )
        worker.process_input_queue = lambda: None
        event = LaviInputEvent(
            text="hello",
            source="voice_input_final",
            event_id="e" * 32,
            event_kind="final_transcript",
            final=True,
            provider_id="VoiceInput",
            fallback_payload="hello",
        )

        worker.receive_input(event)

        self.assertIs(event, worker.input_queue.get_nowait())


__all__ = ["LaviInputEventQueueContractTests"]


if __name__ == "__main__":
    unittest.main()
