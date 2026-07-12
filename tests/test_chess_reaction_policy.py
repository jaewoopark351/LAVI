#20260630_kpopmodder: Added tests for Chess reaction routing and TTS wording policy.
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from plugins.Chess.chess_core.chess_reaction_policy import (
    CHESS_AI_REACTION_SYSTEM_PROMPT,
    build_chess_ai_reaction_tts_text,
    build_chess_fallback_reaction,
    should_request_openai_chess_reaction,
)


class ChessReactionPolicyTests(unittest.TestCase):
    def test_normal_move_does_not_request_openai_reaction(self):
        event = {
            "display_text": "pawn e4",
            "spoken_text": "pawn e four",
            "is_check": False,
            "is_checkmate": False,
        }

        self.assertFalse(should_request_openai_chess_reaction(event))

    def test_check_and_checkmate_request_openai_reaction(self):
        self.assertTrue(should_request_openai_chess_reaction({
            "is_check": True,
            "is_checkmate": False,
        }))
        self.assertTrue(should_request_openai_chess_reaction({
            "is_check": False,
            "is_checkmate": True,
        }))

    def test_normal_move_tts_says_spoken_square_only(self):
        event = {
            "display_text": "pawn e4",
            "spoken_text": "pawn e four",
        }

        self.assertEqual(
            "pawn e four",
            build_chess_ai_reaction_tts_text(event, "pawn e4"),
        )

    def test_reaction_tts_replaces_display_square_with_spoken_square(self):
        event = {
            "display_text": "rook a3",
            "spoken_text": "rook a three",
        }

        self.assertEqual(
            "cold line: rook a three",
            build_chess_ai_reaction_tts_text(event, "cold line: rook a3"),
        )

    def test_check_prompt_requests_cold_banmal_tone(self):
        self.assertIn("casual banmal", CHESS_AI_REACTION_SYSTEM_PROMPT)
        self.assertIn("cold", CHESS_AI_REACTION_SYSTEM_PROMPT)
        self.assertIn("ruthless", CHESS_AI_REACTION_SYSTEM_PROMPT)
        self.assertIn("No warm praise", CHESS_AI_REACTION_SYSTEM_PROMPT)
        self.assertIn("체크", CHESS_AI_REACTION_SYSTEM_PROMPT)
        self.assertIn("체크메이트", CHESS_AI_REACTION_SYSTEM_PROMPT)
        self.assertNotIn("protected-class insults", CHESS_AI_REACTION_SYSTEM_PROMPT)
        self.assertNotIn("sexual content", CHESS_AI_REACTION_SYSTEM_PROMPT)
        self.assertNotIn("threats", CHESS_AI_REACTION_SYSTEM_PROMPT)
        self.assertIn("Do not copy the same taunt every time", CHESS_AI_REACTION_SYSTEM_PROMPT)
        self.assertIn("blunders", CHESS_AI_REACTION_SYSTEM_PROMPT)
        self.assertIn("helplessness", CHESS_AI_REACTION_SYSTEM_PROMPT)
        self.assertIn("개못하네", CHESS_AI_REACTION_SYSTEM_PROMPT)
        self.assertIn("개잘 핵", CHESS_AI_REACTION_SYSTEM_PROMPT)
        self.assertIn("허접", CHESS_AI_REACTION_SYSTEM_PROMPT)
        self.assertIn("수 읽는 척은 그만해", CHESS_AI_REACTION_SYSTEM_PROMPT)

    def test_check_fallback_reaction_says_check_before_move(self):
        reaction = build_chess_fallback_reaction({
            "display_text": "queen h4",
            "is_check": True,
            "is_checkmate": False,
        })

        self.assertEqual("체크. queen h4", reaction)

    def test_checkmate_fallback_reaction_says_checkmate_before_move(self):
        reaction = build_chess_fallback_reaction({
            "display_text": "queen e2",
            "is_check": True,
            "is_checkmate": True,
        })

        self.assertEqual("체크메이트. queen e2. 개못하네, 내가 쓰는 건 개잘 핵이고. 허접.", reaction)


if __name__ == "__main__":
    unittest.main()
