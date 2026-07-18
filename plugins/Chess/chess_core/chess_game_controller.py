import io
import threading

import chess
import chess.pgn

from core.logger import log_print


def color_name(color):
    return "white" if color == chess.WHITE else "black"


def parse_side(value, default):
    text = str(value or "").strip().lower()
    if text == "white":
        return chess.WHITE
    if text == "black":
        return chess.BLACK
    return default


def opposite_color(color):
    return chess.BLACK if color == chess.WHITE else chess.WHITE


PIECE_DISPLAY_NAMES = {
    chess.PAWN: "폰",
    chess.KNIGHT: "나이트",
    chess.BISHOP: "비숍",
    chess.ROOK: "룩",
    chess.QUEEN: "퀸",
    chess.KING: "킹",
}

SQUARE_RANK_SPOKEN = {
    "1": "one",
    "2": "two",
    "3": "three",
    "4": "four",
    "5": "five",
    "6": "six",
    "7": "seven",
    "8": "eight",
}


#20260628_kpopmodder: Added controller so python-chess remains source of truth.
class ChessGameController:
    def __init__(
        self,
        engine=None,
        movetime_ms=1000,
        human_side="white",
        ai_side="black",
        on_ai_move_applied=None,
    ):
        self.engine = engine
        self.movetime_ms = int(movetime_ms)
        self.human_color = parse_side(human_side, chess.WHITE)
        configured_ai_color = parse_side(ai_side, None)
        if configured_ai_color is None:
            self.ai_color = opposite_color(self.human_color)
        elif configured_ai_color == self.human_color:
            self.ai_color = opposite_color(self.human_color)
            log_print(
                "[Chess] ai_side matched human_side; "
                f"using ai_side={color_name(self.ai_color)} instead.",
                level="warning",
            )
        else:
            self.ai_color = configured_ai_color
        self.board = chess.Board()
        self.last_move = ""
        self.last_ai_move_text = ""
        self.last_ai_move_spoken_text = ""
        self.ai_reaction = ""
        self.ai_move_event_id = 0
        self.ai_thinking = False
        self.engine_starting = False
        self.engine_ready = False
        self.on_ai_move_applied = on_ai_move_applied
        self.message = "New game ready."
        self.lock = threading.RLock()

    def new_game(self):
        ai_move_event = None
        with self.lock:
            self.board = chess.Board()
            self.last_move = ""
            self.last_ai_move_text = ""
            self.last_ai_move_spoken_text = ""
            self.ai_reaction = ""
            self.ai_move_event_id += 1
            self.ai_thinking = False
            self.message = "New game ready."
            should_apply_ai_move = self.board.turn == self.ai_color

        self._notify_engine_new_game()
        if should_apply_ai_move:
            state, ai_move_event = self._run_ai_move_for_current_position()
        else:
            state = self.state()
        self._notify_ai_move_applied(ai_move_event)
        return state

    def apply_human_move(self, from_square, to_square, promotion=None):
        ai_move_event = None
        with self.lock:
            if self.board.is_game_over():
                self.message = "Game is already over."
                return self._state_locked(ok=False)
            if self.board.turn != self.human_color:
                self.message = "It is not the human turn."
                return self._state_locked(ok=False)
            if self.engine is not None and not self._engine_ready_locked():
                self.message = self._engine_not_ready_message_locked("Human move")
                log_print(f"[Chess] {self.message}", level="warning")
                return self._state_locked(ok=False)

            move = self._resolve_legal_move(from_square, to_square, promotion)
            if move is None:
                self.message = f"Illegal move: {from_square}{to_square}"
                return self._state_locked(ok=False)

            self.board.push(move)
            self.last_move = move.uci()
            self.message = f"Human move: {move.uci()}"
            log_print(f"[Chess] human move applied: {move.uci()} fen={self.board.fen()}")

            should_apply_ai_move = (
                not self.board.is_game_over()
                and self.board.turn == self.ai_color
            )

            state = self._state_locked()
        if should_apply_ai_move:
            state, ai_move_event = self._run_ai_move_for_current_position()
        self._notify_ai_move_applied(ai_move_event)
        return state

    def apply_human_fen(self, fen):
        ai_move_event = None
        with self.lock:
            if self.engine is not None and not self._engine_ready_locked():
                self.message = self._engine_not_ready_message_locked("Human move")
                log_print(f"[Chess] {self.message}", level="warning")
                return self._state_locked(ok=False)

            move = self._find_legal_move_for_fen_locked(fen)
            if move is None:
                self.message = "GUI FEN did not match a legal move."
                return self._state_locked(ok=False)

            self.board.push(move)
            self.last_move = move.uci()
            self.message = f"Human move: {move.uci()}"
            log_print(f"[Chess] human move applied: {move.uci()} fen={self.board.fen()}")

            should_apply_ai_move = (
                not self.board.is_game_over()
                and self.board.turn == self.ai_color
            )

            state = self._state_locked()
        if should_apply_ai_move:
            state, ai_move_event = self._run_ai_move_for_current_position()
        self._notify_ai_move_applied(ai_move_event)
        return state

    def apply_ai_move(self):
        state, ai_move_event = self._run_ai_move_for_current_position()
        self._notify_ai_move_applied(ai_move_event)
        return state

    def start_engine(self):
        with self.lock:
            if self.engine is None:
                self.message = "LC0 engine is not configured."
                return self._state_locked(ok=False)
            if self.engine_starting:
                self.message = "LC0 engine is already starting."
                return self._state_locked(ok=False)
            if self._engine_ready_locked():
                self.message = "LC0 already ready."
                return self._state_locked()
            self.engine_starting = True
            self.engine_ready = False
            self.message = "LC0 starting..."
            engine = self.engine

        try:
            message = engine.start()
        except Exception as e:
            with self.lock:
                self.engine_starting = False
                self.engine_ready = False
                self.message = f"LC0 start failed: {e}"
                return self._state_locked(ok=False)
        with self.lock:
            self.engine_starting = False
            self.engine_ready = True
            self.message = message
            return self._state_locked()

    def stop_engine(self):
        with self.lock:
            if self.engine is None:
                self.message = "LC0 engine is not configured."
                return self._state_locked(ok=False)
            if self.engine_starting:
                self.message = "LC0 stop skipped: LC0 engine is still starting."
                return self._state_locked(ok=False)
            self.engine_ready = False
            engine = self.engine

        try:
            message = engine.stop()
        except Exception as e:
            with self.lock:
                self.message = f"LC0 stop failed: {e}"
                return self._state_locked(ok=False)
        with self.lock:
            self.message = message
            return self._state_locked()

    def reset_or_resign(self):
        with self.lock:
            self.board = chess.Board()
            self.last_move = ""
            self.last_ai_move_text = ""
            self.last_ai_move_spoken_text = ""
            self.ai_reaction = ""
            self.ai_move_event_id += 1
            self.ai_thinking = False
            self.message = "Game reset."
        self._notify_engine_new_game()
        return self.state()

    def find_legal_move_for_fen(self, fen):
        with self.lock:
            return self._find_legal_move_for_fen_locked(fen)

    def _find_legal_move_for_fen_locked(self, fen):
        target = self._normalized_fen(fen)
        for move in self.board.legal_moves:
            trial = self.board.copy(stack=False)
            trial.push(move)
            if self._normalized_fen(trial.fen()) == target:
                return move
        return None

    def state(self, ok=True):
        with self.lock:
            return self._state_locked(ok=ok)

    def _state_locked(self, ok=True):
        return {
            "ok": bool(ok),
            "fen": self.board.fen(),
            "pgn": self._pgn_locked(),
            "move_history": self._move_history_text_locked(),
            "last_move": self.last_move,
            "last_ai_move": self.last_ai_move_text,
            "last_ai_move_spoken": self.last_ai_move_spoken_text,
            "ai_reaction": self.ai_reaction,
            "result": self._result_text_locked(),
            "game_over": self.board.is_game_over(),
            "turn": color_name(self.board.turn),
            "human_color": color_name(self.human_color),
            "ai_color": color_name(self.ai_color),
            "ai_thinking": self.ai_thinking,
            "engine_starting": self.engine_starting,
            "engine_ready": self._engine_ready_locked(),
            "message": self.message,
            "engine_status": self._engine_status_locked(),
            "engine_log": self.engine_log(),
        }

    def pgn(self):
        with self.lock:
            return self._pgn_locked()

    def _pgn_locked(self):
        game = chess.pgn.Game.from_board(self.board)
        output = io.StringIO()
        print(game, file=output, end="")
        return output.getvalue()

    def move_history_text(self):
        with self.lock:
            return self._move_history_text_locked()

    def _move_history_text_locked(self):
        moves = []
        board = chess.Board()
        for index, move in enumerate(self.board.move_stack):
            if index % 2 == 0:
                moves.append(f"{(index // 2) + 1}.")
            moves.append(board.san(move))
            board.push(move)
        return " ".join(moves)

    def result_text(self):
        with self.lock:
            return self._result_text_locked()

    def _result_text_locked(self):
        if not self.board.is_game_over():
            return "*"
        return self.board.result(claim_draw=True)

    def engine_status(self):
        with self.lock:
            return self._engine_status_locked()

    def engine_log(self):
        if self.engine is None:
            return ""
        return self.engine.log_text()

    def shutdown(self):
        if self.engine is not None:
            self.engine.stop()

    def set_ai_move_applied_callback(self, callback):
        with self.lock:
            self.on_ai_move_applied = callback

    def set_ai_reaction(self, event_id, reaction):
        with self.lock:
            if event_id is not None and int(event_id) != self.ai_move_event_id:
                return False
            self.ai_reaction = str(reaction or "").strip()
            return True

    def _notify_engine_new_game(self):
        with self.lock:
            if not self._engine_ready_locked():
                return
            engine = self.engine

        new_game = getattr(engine, "new_game", None)
        if not callable(new_game):
            return

        try:
            new_game()
        except Exception as e:
            with self.lock:
                self.message = f"LC0 new game failed: {e}"
            log_print(f"[Chess] LC0 new game failed: {e}", level="warning")

    def _run_ai_move_for_current_position(self):
        ai_move_event = None
        with self.lock:
            if self.board.is_game_over():
                self.message = "Game is already over."
                return self._state_locked(ok=False), ai_move_event
            if self.board.turn != self.ai_color:
                self.message = "It is not the AI turn."
                return self._state_locked(ok=False), ai_move_event
            if self.engine is None:
                self.message = "AI move skipped: LC0 engine is not configured."
                log_print(f"[Chess] {self.message}", level="warning")
                return self._state_locked(), ai_move_event
            if not self._engine_ready_locked():
                self.message = self._engine_not_ready_message_locked("AI move")
                log_print(f"[Chess] {self.message}", level="warning")
                return self._state_locked(ok=False), ai_move_event
            if self.ai_thinking:
                self.message = "AI move skipped: LC0 is already thinking."
                log_print(f"[Chess] {self.message}", level="warning")
                return self._state_locked(ok=False), ai_move_event

            engine = self.engine
            fen = self.board.fen()
            movetime_ms = self.movetime_ms
            request_event_id = self.ai_move_event_id
            self.ai_thinking = True
            self.message = "AI thinking..."
            log_print(f"[Chess] AI move requested: fen={fen}")

        bestmove = None
        error = None
        try:
            bestmove = engine.bestmove(fen, movetime_ms)
        except Exception as e:
            error = e

        with self.lock:
            self.ai_thinking = False
            if self.ai_move_event_id != request_event_id:
                log_print(
                    "[Chess] AI move discarded: game state changed while LC0 was thinking.",
                    level="warning",
                )
                return self._state_locked(ok=False), ai_move_event

            if error is not None:
                self.message = f"AI move failed: {error}"
                log_print(f"[Chess] {self.message}", level="warning")
                return self._state_locked(), ai_move_event

            if self.board.fen() != fen:
                self.message = "AI move discarded: board changed while LC0 was thinking."
                log_print(f"[Chess] {self.message}", level="warning")
                return self._state_locked(ok=False), ai_move_event

            ai_move_event = self._apply_ai_bestmove_locked(bestmove)
            return self._state_locked(), ai_move_event

    def _apply_ai_bestmove_locked(self, bestmove):
        if not bestmove:
            self.message = "AI move unavailable: LC0 returned no bestmove."
            log_print(f"[Chess] {self.message}", level="warning")
            return None

        try:
            move = chess.Move.from_uci(bestmove)
        except Exception:
            self.message = f"AI move invalid: {bestmove}"
            log_print(f"[Chess] {self.message}", level="warning")
            return None

        if move not in self.board.legal_moves:
            self.message = f"AI move illegal: {bestmove}"
            log_print(f"[Chess] {self.message}", level="warning")
            return None

        fen_before = self.board.fen()
        san = self.board.san(move)
        is_capture = self.board.is_capture(move)
        move_text = self._move_display_text(move)
        move_spoken_text = self._move_spoken_text(move)
        self.board.push(move)
        self.last_move = move.uci()
        self.last_ai_move_text = move_text
        self.last_ai_move_spoken_text = move_spoken_text
        self.ai_reaction = move_text
        self.ai_move_event_id += 1
        self.message = f"AI move: {move.uci()}"
        log_print(
            "[Chess] AI move applied: "
            f"{move.uci()} display={move_text} spoken={move_spoken_text} "
            f"fen={self.board.fen()}"
        )
        return self._build_ai_move_event_locked(
            event_id=self.ai_move_event_id,
            move=move,
            san=san,
            is_capture=is_capture,
            fen_before=fen_before,
            move_text=move_text,
            move_spoken_text=move_spoken_text,
        )

    def _resolve_legal_move(self, from_square, to_square, promotion=None):
        try:
            from_index = chess.parse_square(str(from_square))
            to_index = chess.parse_square(str(to_square))
        except Exception:
            return None

        promotion_piece = self._promotion_piece(promotion)
        matching = []
        for move in self.board.legal_moves:
            if move.from_square != from_index or move.to_square != to_index:
                continue
            if promotion_piece is not None and move.promotion != promotion_piece:
                continue
            matching.append(move)

        if not matching:
            return None
        if promotion_piece is None:
            queen = [move for move in matching if move.promotion == chess.QUEEN]
            if queen:
                return queen[0]
        return matching[0]

    def _promotion_piece(self, promotion):
        text = str(promotion or "").strip().lower()
        if not text:
            return None
        return {
            "q": chess.QUEEN,
            "r": chess.ROOK,
            "b": chess.BISHOP,
            "n": chess.KNIGHT,
        }.get(text)

    def _normalized_fen(self, fen):
        board = chess.Board(fen)
        return " ".join(board.fen().split()[:4])

    def _move_display_text(self, move):
        piece = self.board.piece_at(move.from_square)
        piece_name = PIECE_DISPLAY_NAMES.get(
            piece.piece_type if piece else None,
            "말",
        )
        return f"{piece_name} {chess.square_name(move.to_square)}"

    def _move_spoken_text(self, move):
        piece = self.board.piece_at(move.from_square)
        piece_name = PIECE_DISPLAY_NAMES.get(
            piece.piece_type if piece else None,
            "말",
        )
        return f"{piece_name} {self._square_spoken_text(move.to_square)}"

    def _square_spoken_text(self, square):
        name = chess.square_name(square)
        return f"{name[0]} {SQUARE_RANK_SPOKEN.get(name[1], name[1])}"

    def _build_ai_move_event_locked(
        self,
        event_id,
        move,
        san,
        is_capture,
        fen_before,
        move_text,
        move_spoken_text,
    ):
        return {
            "event": "ai_move_applied",
            "event_id": event_id,
            "uci": move.uci(),
            "san": san,
            "display_text": move_text,
            "spoken_text": move_spoken_text,
            "fen_before": fen_before,
            "fen_after": self.board.fen(),
            "move_history": self._move_history_text_locked(),
            "pgn": self._pgn_locked(),
            "result": self._result_text_locked(),
            "game_over": self.board.is_game_over(),
            "is_capture": is_capture,
            "is_check": self.board.is_check(),
            "is_checkmate": self.board.is_checkmate(),
            "ai_color": color_name(self.ai_color),
        }

    def _notify_ai_move_applied(self, event):
        if event is None:
            return
        callback = self.on_ai_move_applied
        if callback is None:
            return
        try:
            callback(dict(event))
        except Exception as e:
            log_print(f"[Chess] AI move callback failed: {e}", level="warning")

    def _engine_ready_locked(self):
        if self.engine is None or self.engine_starting:
            return False

        is_running = getattr(self.engine, "is_running", None)
        if callable(is_running):
            try:
                if not is_running():
                    self.engine_ready = False
                    return False
            except Exception:
                self.engine_ready = False
                return False

        return bool(self.engine_ready)

    def _engine_status_locked(self):
        if self.engine is None:
            return "not configured"
        if self.engine_starting:
            return "starting"
        return self.engine.status_text()

    def _engine_not_ready_message_locked(self, action):
        if self.engine_starting:
            return f"{action} skipped: LC0 engine is still starting."
        return f"{action} skipped: LC0 engine is not ready."
