import threading
import unittest
from queue import Queue
from unittest import mock

from tts_core import TTS
from tts_core.tts_interrupt_controller import TTSInterruptController
from tts_core.tts_queue_worker import TTSQueueWorker


class FakeTTSOwner:
    def __init__(self):
        self.input_queue = Queue()
        self.interrupt_event = threading.Event()
        self.audio_process_thread = None
        self.queue_generation = 1
        self.queue_lock = threading.Lock()
        self.synth_lock = threading.Lock()
        self.audio_lock = threading.RLock()
        self.latest_response_generation = None
        self.outputs = []
        self.played_audio = []
        self.subtitles = []
        self.process_queue_live_textbox = FakeLiveTextbox()
        self.audio_player = mock.Mock()
        self.mouth_animator = mock.Mock()

    def get_queue_generation(self):
        return self.queue_generation

    def bump_queue_generation(self):
        self.queue_generation += 1
        return self.queue_generation

    def send_output(self, output):
        self.outputs.append(output)

    def is_stale_response_generation(self, response_generation):
        return (
            response_generation is not None
            and self.latest_response_generation is not None
            and response_generation < self.latest_response_generation
        )

    def parse_queue_item(self, item, default_queue_generation=None):
        if isinstance(item, dict):
            return (
                item.get("queue_generation", default_queue_generation),
                item.get("response_generation"),
                item.get("text", ""),
            )
        if isinstance(item, tuple) and len(item) == 2:
            queue_generation, text = item
            return queue_generation, None, text
        return default_queue_generation, None, item

    def get_queue_display_items(self):
        return []

    def update_subtitle_file(self, text):
        self.subtitles.append(text)

    def play_sound_from_bytes(self, audio_result):
        self.played_audio.append(audio_result)
        return True


class FakeLiveTextbox:
    def __init__(self):
        self.values = []

    def set(self, value):
        self.values.append(value)


class FakeTextProcessor:
    def is_tts_skippable(self, text):
        return False


class TTSQueueWorkerTests(unittest.TestCase):
    def test_pending_interrupt_does_not_dequeue_first_new_sentence(self):
        owner = FakeTTSOwner()
        first_item = (owner.queue_generation, "첫 문장")
        owner.input_queue.put(first_item)
        owner.interrupt_event.set()

        worker = TTSQueueWorker(owner)
        restart_calls = []
        synthesize_calls = []
        worker.start_if_needed = restart_calls.append

        worker.worker_loop(
            synthesize_function=synthesize_calls.append,
            worker_generation=owner.queue_generation,
        )

        self.assertFalse(owner.interrupt_event.is_set())
        self.assertEqual([], synthesize_calls)
        self.assertEqual(first_item, owner.input_queue.get_nowait())
        self.assertEqual(1, len(restart_calls))

    def test_new_response_generation_skips_old_queued_sentences(self):
        owner = FakeTTSOwner()
        owner.latest_response_generation = 2
        owner.input_queue.put({
            "queue_generation": owner.queue_generation,
            "response_generation": 1,
            "text": "old sentence",
        })
        owner.input_queue.put({
            "queue_generation": owner.queue_generation,
            "response_generation": 2,
            "text": "new first sentence",
        })

        worker = TTSQueueWorker(owner)
        synthesize_calls = []

        def synthesize(text):
            synthesize_calls.append(text)
            return f"audio:{text}".encode("utf-8")

        worker.worker_loop(
            synthesize_function=synthesize,
            worker_generation=owner.queue_generation,
        )

        self.assertEqual(["new first sentence"], synthesize_calls)
        self.assertEqual(["new first sentence"], owner.subtitles)
        self.assertEqual([b"audio:new first sentence"], owner.played_audio)

    def test_enqueue_new_response_generation_clears_older_pending_sentences(self):
        tts = TTS.__new__(TTS)
        tts.input_queue = Queue()
        tts.queue_lock = threading.Lock()
        tts.queue_generation_lock = threading.Lock()
        tts.queue_generation = 1
        tts.response_generation_lock = threading.Lock()
        tts.latest_response_generation = None
        tts.text_processor = FakeTextProcessor()
        tts.process_queue_live_textbox = FakeLiveTextbox()

        tts.enqueue_input_items(["old one", "old two"], response_generation=1)
        tts.enqueue_input_items(["new first"], response_generation=2)

        queued_items = list(tts.input_queue.queue)
        queued_texts = [
            tts.parse_queue_item(item)[2]
            for item in queued_items
        ]

        self.assertEqual(["new first"], queued_texts)

    def test_cancel_pending_restarts_new_generation_after_slow_synthesis(self):
        owner = FakeTTSOwner()
        worker = TTSQueueWorker(owner)
        controller = TTSInterruptController(owner)
        synth_started = threading.Event()
        release_old_synth = threading.Event()
        new_playback_finished = threading.Event()

        def synthesize(text):
            if text == "이전 스타2 문장":
                synth_started.set()
                release_old_synth.wait(timeout=2.0)
            return f"audio:{text}".encode("utf-8")

        def play_sound(audio_result):
            owner.played_audio.append(audio_result)
            if audio_result == "audio:승리 결과".encode("utf-8"):
                new_playback_finished.set()
            return True

        owner.play_sound_from_bytes = play_sound
        owner.input_queue.put((owner.queue_generation, "이전 스타2 문장"))
        worker.start_if_needed(synthesize)
        self.assertTrue(synth_started.wait(timeout=1.0))

        new_generation = controller.cancel_pending(reason="starcraft2_game_ended")
        owner.input_queue.put((new_generation, "승리 결과"))
        worker.start_if_needed(synthesize)
        release_old_synth.set()

        self.assertTrue(new_playback_finished.wait(timeout=2.0))
        active_thread = owner.audio_process_thread
        if active_thread is not None:
            active_thread.join(timeout=1.0)

        self.assertFalse(owner.interrupt_event.is_set())
        self.assertNotIn("이전 스타2 문장", owner.subtitles)
        self.assertNotIn(
            "audio:이전 스타2 문장".encode("utf-8"),
            owner.played_audio,
        )
        self.assertIn("승리 결과", owner.subtitles)
        self.assertIn("audio:승리 결과".encode("utf-8"), owner.played_audio)


if __name__ == "__main__":
    unittest.main()
