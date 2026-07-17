import os
import queue
import subprocess
import threading
import time

from core.logger import log_print
from core.gpu_device_manager import gpu_device_manager
from core.process import launch_process


#20260628_kpopmodder: Added LC0 UCI wrapper so Chess never implements an engine.
class LC0EngineError(RuntimeError):
    pass


#20260628_kpopmodder: This class talks to lc0.exe through UCI subprocess IO.
class LC0UCIEngine:
    def __init__(
        self,
        lc0_path,
        weights_path,
        backend="cuda",
        cuda_visible_devices="",
        init_timeout_sec=15.0,
        move_timeout_sec=10.0,
        stop_timeout_sec=2.0,
        log_limit=80,
    ):
        self.lc0_path = str(lc0_path or "").strip()
        self.weights_path = str(weights_path or "").strip()
        self.backend = str(backend or "").strip()
        self.cuda_visible_devices = str(cuda_visible_devices or "").strip()
        self.init_timeout_sec = float(init_timeout_sec)
        self.move_timeout_sec = float(move_timeout_sec)
        self.stop_timeout_sec = float(stop_timeout_sec)
        self.log_limit = int(log_limit)
        self.process = None
        self.stdout_thread = None
        self.output_queue = queue.Queue()
        self.write_lock = threading.RLock()
        self.log_lines = []

    @staticmethod
    def parse_bestmove(line):
        parts = str(line or "").strip().split()
        if len(parts) < 2 or parts[0] != "bestmove":
            return None
        bestmove = parts[1].strip()
        if bestmove in {"", "0000", "(none)", "none"}:
            return None
        return bestmove

    def start(self):
        if self.is_running():
            return "LC0 already running."

        self._drain_stale_output()
        if not self.lc0_path or not os.path.exists(self.lc0_path):
            raise LC0EngineError(f"lc0.exe not found: {self.lc0_path}")
        if not self.weights_path or not os.path.exists(self.weights_path):
            raise LC0EngineError(
                f"BT4-it332 weights file not found: {self.weights_path}"
            )

        env = os.environ.copy()
        if self.cuda_visible_devices:
            gpu_device_manager.apply_cuda_visible_devices(
                env,
                "Chess",
                self.cuda_visible_devices,
            )

        log_print(
            f"[Chess] LC0 start requested: path={self.lc0_path} "
            f"weights={self.weights_path} backend={self.backend}"
        )

        try:
            self.process = launch_process(
                [self.lc0_path],
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                cwd=os.path.dirname(self.lc0_path) or None,
                env=env,
                text=True,
                encoding="utf-8",
                errors="replace",
                bufsize=1,
            )
        except Exception as e:
            self.process = None
            raise LC0EngineError(f"failed to start lc0.exe: {e}") from e

        self.stdout_thread = threading.Thread(
            target=self._reader_loop,
            name="ChessLC0Stdout",
            daemon=True,
        )
        self.stdout_thread.start()

        with self.write_lock:
            try:
                self._send("uci")
                self._wait_for("uciok", self.init_timeout_sec)
                self._send(f"setoption name WeightsFile value {self.weights_path}")
                if self.backend:
                    self._send(f"setoption name Backend value {self.backend}")
                self._send("isready")
                self._wait_for("readyok", self.init_timeout_sec)
            except Exception:
                self.stop()
                raise

        log_print("[Chess] LC0 ready.")
        return "LC0 ready."

    def bestmove(self, fen, movetime_ms):
        if not self.is_running():
            raise LC0EngineError("LC0 is not running.")

        with self.write_lock:
            self._drain_stale_output()
            log_print(
                f"[Chess] LC0 bestmove request: fen={fen} "
                f"movetime_ms={int(movetime_ms)}"
            )
            self._send(f"position fen {fen}")
            self._send(f"go movetime {int(movetime_ms)}")

            found, bestmove = self._read_bestmove_until(
                time.monotonic() + self.move_timeout_sec,
                "response",
            )
            if found:
                return bestmove

            self._append_log("LC0 bestmove timeout.")
            log_print("[Chess] LC0 bestmove timeout.", level="warning")
            return self._recover_bestmove_timeout()

    def new_game(self):
        if not self.is_running():
            return "LC0 not running."

        with self.write_lock:
            log_print("[Chess] LC0 new game requested.")
            self._drain_stale_output()
            self._send("ucinewgame")
            self._send("isready")
            self._wait_for("readyok", self.init_timeout_sec)
        return "LC0 new game ready."

    def restart(self):
        self.stop()
        return self.start()

    def stop(self):
        with self.write_lock:
            process = self.process
            if process is None:
                return "LC0 already stopped."

            log_print("[Chess] LC0 stop requested.")
            if process.poll() is None:
                try:
                    self._send("quit")
                except Exception:
                    pass
                try:
                    process.wait(timeout=2)
                except Exception:
                    try:
                        process.terminate()
                        process.wait(timeout=2)
                    except Exception:
                        try:
                            process.kill()
                        except Exception:
                            pass

            self.process = None
            self._drain_stale_output()
            log_print("[Chess] LC0 stopped.")
            return "LC0 stopped."

    def is_running(self):
        return self.process is not None and self.process.poll() is None

    def status_text(self):
        return "running" if self.is_running() else "stopped"

    def log_text(self):
        return "\n".join(self.log_lines[-self.log_limit:])

    def _reader_loop(self):
        process = self.process
        if process is None or process.stdout is None:
            return

        try:
            for line in process.stdout:
                clean = line.rstrip("\r\n")
                self._append_log(clean)
                self.output_queue.put(clean)
        except Exception as e:
            self._append_log(f"LC0 stdout reader failed: {e}")

    def _send(self, command):
        with self.write_lock:
            if self.process is None or self.process.stdin is None:
                raise LC0EngineError("LC0 process is not available.")
            if self.process.poll() is not None:
                raise LC0EngineError("LC0 process exited.")
            self._append_log("> " + command)
            self.process.stdin.write(command + "\n")
            self.process.stdin.flush()

    def _wait_for(self, token, timeout_sec):
        deadline = time.monotonic() + float(timeout_sec)
        while time.monotonic() < deadline:
            timeout = max(0.05, deadline - time.monotonic())
            line = self._read_line(timeout)
            if line is None:
                continue
            if token in line:
                return line
        raise LC0EngineError(f"LC0 timeout waiting for {token}.")

    def _read_line(self, timeout_sec):
        try:
            return self.output_queue.get(timeout=float(timeout_sec))
        except queue.Empty:
            return None

    def _read_bestmove_until(self, deadline, label):
        while time.monotonic() < deadline:
            timeout = max(0.05, deadline - time.monotonic())
            line = self._read_line(timeout)
            if line is None:
                continue
            if line.startswith("bestmove"):
                bestmove = self.parse_bestmove(line)
                log_print(f"[Chess] LC0 bestmove {label}: {bestmove} raw={line}")
                return True, bestmove
        return False, None

    def _recover_bestmove_timeout(self):
        try:
            self._send("stop")
        except Exception as e:
            self._append_log(f"LC0 stop after timeout failed: {e}")
            log_print(f"[Chess] LC0 stop after timeout failed: {e}", level="warning")

        found, bestmove = self._read_bestmove_until(
            time.monotonic() + max(0.1, self.stop_timeout_sec),
            "after stop",
        )
        if found:
            return bestmove

        self._append_log("LC0 did not return bestmove after stop; restarting.")
        log_print(
            "[Chess] LC0 did not return bestmove after stop; restarting.",
            level="warning",
        )
        try:
            self.restart()
        except Exception as e:
            raise LC0EngineError(
                "LC0 bestmove timeout; stop did not recover and restart failed: "
                f"{e}"
            ) from e
        raise LC0EngineError("LC0 bestmove timeout; LC0 restarted. Try again.")

    def _drain_stale_output(self):
        while True:
            try:
                self.output_queue.get_nowait()
            except queue.Empty:
                return

    def _append_log(self, text):
        self.log_lines.append(str(text))
        if len(self.log_lines) > self.log_limit:
            del self.log_lines[:-self.log_limit]
