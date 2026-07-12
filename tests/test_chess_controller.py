import os
import sys
import threading
import unittest

import chess

#20260628_kpopmodder: Added Chess controller regression tests.

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from plugins.Chess.chess_core.chess_game_controller import ChessGameController


class FakeEngine:
    def __init__(self, bestmove="e7e5"):
        self.bestmove_value = bestmove
        self.started = False
        self.new_game_count = 0
        self.bestmove_fens = []

    def start(self):
        self.started = True
        return "fake ready"

    def stop(self):
        self.started = False
        return "fake stopped"

    def bestmove(self, fen, movetime_ms):
        self.bestmove_fens.append(fen)
        return self.bestmove_value

    def new_game(self):
        self.new_game_count += 1
        return "fake new game"

    def status_text(self):
        return "running" if self.started else "stopped"

    def log_text(self):
        return "fake log"


class BlockingEngine(FakeEngine):
    def __init__(self, bestmove="e7e5"):
        super().__init__(bestmove)
        self.bestmove_started = threading.Event()
        self.release_bestmove = threading.Event()

    def bestmove(self, fen, movetime_ms):
        self.bestmove_fens.append(fen)
        self.bestmove_started.set()
        if not self.release_bestmove.wait(2):
            raise RuntimeError("test bestmove was not released")
        return self.bestmove_value


class FailingEngine(FakeEngine):#20260630_kpopmodder
    def bestmove(self, fen, movetime_ms):
        self.bestmove_fens.append(fen)
        raise TimeoutError("lc0 timeout")


class ChessGameControllerTests(unittest.TestCase):
    def test_new_game_starts_from_standard_fen(self):
        controller = ChessGameController()
        state = controller.new_game()
        self.assertTrue(state["ok"])
        self.assertEqual(state["fen"], chess.Board().fen())

    def test_state_is_safe_while_lock_is_already_held(self):
        controller = ChessGameController()
        with controller.lock:
            state = controller.state()
        self.assertTrue(state["ok"])
        self.assertEqual(state["fen"], chess.Board().fen())

    def test_human_move_then_ai_move(self):
        controller = ChessGameController(engine=FakeEngine("e7e5"))
        controller.start_engine()
        state = controller.apply_human_move("e2", "e4")
        self.assertTrue(state["ok"])
        self.assertIn("e4", state["move_history"])
        self.assertIn("e5", state["move_history"])
        self.assertEqual(state["last_move"], "e7e5")

    def test_state_is_available_while_ai_bestmove_is_running(self):
        engine = BlockingEngine("e7e5")
        controller = ChessGameController(engine=engine)
        controller.start_engine()
        result = []

        worker = threading.Thread(
            target=lambda: result.append(controller.apply_human_move("e2", "e4")),
            daemon=True,
        )
        worker.start()
        self.assertTrue(engine.bestmove_started.wait(1))

        state_result = []
        state_worker = threading.Thread(
            target=lambda: state_result.append(controller.state()),
            daemon=True,
        )
        state_worker.start()
        state_worker.join(0.2)

        self.assertTrue(state_result, "state() blocked while LC0 was thinking")
        self.assertTrue(state_result[0]["ai_thinking"])

        engine.release_bestmove.set()
        worker.join(1)
        self.assertFalse(worker.is_alive())
        self.assertEqual("e7e5", result[0]["last_move"])

    def test_ai_bestmove_is_discarded_if_board_changes_while_thinking(self):
        events = []
        engine = BlockingEngine("e7e5")
        controller = ChessGameController(
            engine=engine,
            on_ai_move_applied=events.append,
        )
        controller.start_engine()
        result = []

        worker = threading.Thread(
            target=lambda: result.append(controller.apply_human_move("e2", "e4")),
            daemon=True,
        )
        worker.start()
        self.assertTrue(engine.bestmove_started.wait(1))

        reset_state = controller.reset_or_resign()
        self.assertEqual(chess.Board().fen(), reset_state["fen"])

        engine.release_bestmove.set()
        worker.join(1)

        self.assertFalse(worker.is_alive())
        self.assertEqual([], events)
        self.assertFalse(result[0]["ok"])
        self.assertEqual("Game reset.", result[0]["message"])
        self.assertEqual(chess.Board().fen(), controller.state()["fen"])

    def test_ai_bestmove_failure_keeps_current_position(self):#20260630_kpopmodder
        engine = FailingEngine()
        controller = ChessGameController(engine=engine)
        controller.start_engine()

        state = controller.apply_human_move("e2", "e4")
        expected_board = chess.Board()
        expected_board.push(chess.Move.from_uci("e2e4"))

        self.assertTrue(state["ok"])
        self.assertEqual(expected_board.fen(), state["fen"])
        self.assertEqual("e2e4", state["last_move"])
        self.assertIn("AI move failed", state["message"])
        self.assertFalse(state["ai_thinking"])

    def test_illegal_human_move_is_rejected(self):
        controller = ChessGameController()
        before = controller.state()["fen"]
        state = controller.apply_human_move("e2", "e5")
        self.assertFalse(state["ok"])
        self.assertEqual(state["fen"], before)

    def test_find_legal_move_for_gui_fen(self):
        controller = ChessGameController()
        trial = chess.Board()
        trial.push(chess.Move.from_uci("e2e4"))
        move = controller.find_legal_move_for_fen(trial.fen())
        self.assertEqual(move.uci(), "e2e4")

    def test_ai_move_does_nothing_when_game_is_over(self):
        controller = ChessGameController(engine=FakeEngine())
        controller.board = chess.Board("7k/6Q1/6K1/8/8/8/8/8 b - - 0 1")
        state = controller.apply_ai_move()
        self.assertFalse(state["ok"])
        self.assertEqual(state["result"], "1-0")

    def test_ai_move_event_formats_pawn_square_for_display_and_speech(self):
        events = []
        controller = ChessGameController(
            engine=FakeEngine("e2e4"),
            human_side="black",
            ai_side="white",
            on_ai_move_applied=events.append,
        )

        state = controller.new_game()

        self.assertTrue(state["ok"])
        self.assertEqual(1, len(events))
        self.assertEqual("폰 e4", events[0]["display_text"])
        self.assertEqual("폰 e four", events[0]["spoken_text"])
        self.assertEqual("폰 e4", state["last_ai_move"])
        self.assertEqual("폰 e four", state["last_ai_move_spoken"])
        self.assertEqual("폰 e4", state["ai_reaction"])

    def test_new_game_and_reset_notify_lc0_new_game(self):
        engine = FakeEngine()
        controller = ChessGameController(engine=engine)

        controller.new_game()
        controller.reset_or_resign()

        self.assertEqual(2, engine.new_game_count)

    def test_matching_human_and_ai_sides_are_auto_corrected(self):
        controller = ChessGameController(human_side="white", ai_side="white")
        state = controller.state()

        self.assertEqual("white", state["human_color"])
        self.assertEqual("black", state["ai_color"])

    def test_ai_move_event_formats_rook_square_for_display_and_speech(self):
        events = []
        controller = ChessGameController(
            engine=FakeEngine("a2a3"),
            human_side="black",
            ai_side="white",
            on_ai_move_applied=events.append,
        )
        controller.board = chess.Board("7k/8/8/8/8/8/R7/7K w - - 0 1")

        state = controller.apply_ai_move()

        self.assertTrue(state["ok"])
        self.assertEqual(1, len(events))
        self.assertEqual("룩 a3", events[0]["display_text"])
        self.assertEqual("룩 a three", events[0]["spoken_text"])

    def test_ai_move_event_is_not_sent_for_illegal_engine_move(self):
        events = []
        controller = ChessGameController(
            engine=FakeEngine("e2e5"),
            human_side="black",
            ai_side="white",
            on_ai_move_applied=events.append,
        )

        state = controller.new_game()

        self.assertTrue(state["ok"])
        self.assertEqual([], events)
        self.assertIn("AI move illegal", state["message"])

    def test_stale_ai_reaction_update_is_ignored(self):
        controller = ChessGameController(
            engine=FakeEngine("e2e4"),
            human_side="black",
            ai_side="white",
        )
        controller.new_game()
        old_event_id = controller.ai_move_event_id
        controller.reset_or_resign()

        updated = controller.set_ai_reaction(
            old_event_id,
            "stale",
        )

        self.assertFalse(updated)
        self.assertEqual("", controller.state()["ai_reaction"])


if __name__ == "__main__":
    unittest.main()
