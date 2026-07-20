import unittest

from llm_core.streaming_chunker import LLMStreamingChunker
from tts_core.text_processor import TTSTextProcessor


class TTSDecimalAndInitialismTests(unittest.TestCase):
    def test_streaming_chunker_does_not_split_decimal_points(self):
        chunker = LLMStreamingChunker()
        text = "청자 수는 31.6만명, 좋아요 수는 1.7천개로 나타났습니다. 다음"

        chunk, processed_idx = chunker.get_streaming_tts_chunk(text, 0)

        self.assertEqual(
            "청자 수는 31.6만명, 좋아요 수는 1.7천개로 나타났습니다.",
            chunk,
        )
        self.assertEqual(text.index(" 다음"), processed_idx)

    def test_streaming_chunker_waits_on_digit_dot_at_stream_tail(self):
        chunker = LLMStreamingChunker()

        chunk, processed_idx = chunker.get_streaming_tts_chunk("청자 수는 31.", 0)

        self.assertIsNone(chunk)
        self.assertEqual(0, processed_idx)

    def test_streaming_chunker_keeps_markdown_list_marker_with_item(self):
        chunker = LLMStreamingChunker()
        text = "1.\n**\uc815\uc758**: \ud569\uc131 CDO\uc57c. \ub2e4\uc74c"

        chunk, processed_idx = chunker.get_streaming_tts_chunk(text, 0)

        self.assertEqual(
            "1.\n**\uc815\uc758**: \ud569\uc131 CDO\uc57c.",
            chunk,
        )
        self.assertEqual(text.index(" \ub2e4\uc74c"), processed_idx)

    def test_streaming_chunker_does_not_split_initialism_dots(self):
        chunker = LLMStreamingChunker()
        text = "S.T.A.R.S가 화면에 표시됩니다. 다음"

        chunk, processed_idx = chunker.get_streaming_tts_chunk(text, 0)

        self.assertEqual("S.T.A.R.S가 화면에 표시됩니다.", chunk)
        self.assertEqual(text.index(" 다음"), processed_idx)

    def test_streaming_chunker_keeps_regular_sentence_dot(self):
        chunker = LLMStreamingChunker()

        chunk, processed_idx = chunker.get_streaming_tts_chunk(
            "준비됐습니다.",
            0,
        )

        self.assertEqual("준비됐습니다.", chunk)
        self.assertEqual(len("준비됐습니다."), processed_idx)

    def test_tts_normalizes_decimal_points_for_speech(self):
        processor = TTSTextProcessor()

        normalized = processor.normalize_text_item(
            "청자 수는 31.6만명, 좋아요 수는 1.7천개로 나타났습니다."
        )

        self.assertIn("31점6만명", normalized)
        self.assertIn("1점7천개", normalized)

    def test_tts_normalizes_initialism_dots_for_speech(self):
        processor = TTSTextProcessor()

        normalized = processor.normalize_text_item("S.T.A.R.S가 표시됩니다.")

        self.assertEqual("S T A R S 가 표시됩니다.", normalized)

    def test_tts_sentence_split_keeps_decimal_and_initialism_together(self):
        processor = TTSTextProcessor()
        text = processor.normalize_text_item(
            "청자 수는 31.6만명입니다. S.T.A.R.S가 표시됩니다."
        )

        self.assertEqual(
            [
                "청자 수는 31점6만명입니다.",
                "S T A R S 가 표시됩니다.",
            ],
            processor.split_tts_sentences(text),
        )


if __name__ == "__main__":
    unittest.main()
