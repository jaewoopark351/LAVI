import os
import sys
import unittest

#20260628_kpopmodder: Added LC0 UCI parsing tests without launching lc0.exe.

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from plugins.Chess.chess_core.chess_game_controller import ChessGameController
from plugins.Chess.chess_core.lc0_uci_engine import LC0EngineError, LC0UCIEngine


class FakeUCIEngine(LC0UCIEngine):
    def __init__(self, lines_after_stop=None, recover_by_restart=False):
        super().__init__(
            lc0_path="fake-lc0.exe",
            weights_path="fake-weights.pb.gz",
            move_timeout_sec=0.01,
            stop_timeout_sec=0.01,
        )
        self.sent_commands = []
        self.lines_after_stop = list(lines_after_stop or [])
        self.recover_by_restart = recover_by_restart
        self.restart_count = 0
        self.waited_tokens = []

    def is_running(self):
        return True

    def _send(self, command):
        self.sent_commands.append(command)

    def _wait_for(self, token, timeout_sec):
        self.waited_tokens.append(token)
        return token

    def _read_line(self, timeout_sec):
        if "stop" in self.sent_commands and self.lines_after_stop:
            return self.lines_after_stop.pop(0)
        return None

    def restart(self):
        self.restart_count += 1
        if not self.recover_by_restart:
            raise LC0EngineError("fake restart failed")
        return "fake restarted"


class FakeRuntimeDownloader:
    def __init__(self, result):
        self.result = dict(result)
        self.calls = []

    def ensure_runtime(self, **kwargs):
        self.calls.append(kwargs)
        return dict(self.result)


class ChessLC0EngineTests(unittest.TestCase):
    def test_parse_bestmove_with_ponder(self):
        self.assertEqual(
            LC0UCIEngine.parse_bestmove("bestmove e2e4 ponder e7e5"),
            "e2e4",
        )

    def test_parse_bestmove_0000(self):
        self.assertIsNone(LC0UCIEngine.parse_bestmove("bestmove 0000"))

    def test_new_game_sends_ucinewgame_and_isready(self):
        engine = FakeUCIEngine()

        result = engine.new_game()

        self.assertEqual("LC0 new game ready.", result)
        self.assertEqual(["ucinewgame", "isready"], engine.sent_commands)
        self.assertEqual(["readyok"], engine.waited_tokens)

    def test_bestmove_timeout_sends_stop_and_uses_late_bestmove(self):
        engine = FakeUCIEngine(lines_after_stop=["bestmove e7e5 ponder g1f3"])

        bestmove = engine.bestmove(chess_start_fen(), 1000)

        self.assertEqual("e7e5", bestmove)
        self.assertIn("stop", engine.sent_commands)
        self.assertEqual(0, engine.restart_count)

    def test_bestmove_timeout_restarts_and_raises_recoverable_error(self):
        engine = FakeUCIEngine(recover_by_restart=True)

        with self.assertRaises(LC0EngineError) as context:
            engine.bestmove(chess_start_fen(), 1000)

        self.assertIn("LC0 restarted", str(context.exception))
        self.assertIn("stop", engine.sent_commands)
        self.assertEqual(1, engine.restart_count)

    def test_missing_lc0_does_not_escape_controller(self):
        engine = LC0UCIEngine(
            lc0_path=r"C:\missing\lc0.exe",
            weights_path=r"C:\missing\BT4-it332.pb.gz",
        )
        controller = ChessGameController(engine=engine)
        state = controller.start_engine()
        self.assertFalse(state["ok"])
        self.assertIn("LC0 start failed", state["message"])

    def test_start_ensures_runtime_before_file_checks(self):
        downloader = FakeRuntimeDownloader({"ok": True, "downloaded": True})
        engine = LC0UCIEngine(
            lc0_path=r"C:\missing\lc0.exe",
            weights_path=r"C:\missing\BT4-it332.pb.gz",
            runtime_downloader=downloader,
            runtime_download_config={"runtime_dir": "plugins/Chess/lc0"},
        )

        with self.assertRaises(LC0EngineError):
            engine.start()

        self.assertEqual(
            [{"runtime_dir": "plugins/Chess/lc0"}],
            downloader.calls,
        )

    def test_runtime_download_failure_is_actionable(self):
        downloader = FakeRuntimeDownloader(
            {
                "ok": False,
                "downloaded": False,
                "error": "lc0_runtime_download_disabled",
            }
        )
        engine = LC0UCIEngine(
            lc0_path=r"C:\missing\lc0.exe",
            weights_path=r"C:\missing\BT4-it332.pb.gz",
            runtime_downloader=downloader,
            runtime_download_config={"runtime_dir": "plugins/Chess/lc0"},
        )

        with self.assertRaises(LC0EngineError) as context:
            engine._ensure_runtime_available()

        self.assertIn("LC0 runtime download failed", str(context.exception))


def chess_start_fen():
    return "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"


if __name__ == "__main__":
    unittest.main()
