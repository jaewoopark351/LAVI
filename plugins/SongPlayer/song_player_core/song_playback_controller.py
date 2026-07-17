import os
import struct
import threading
import time
import traceback
import wave

from core.global_state import global_state, GlobalKeys
from core.logger import log_print
from tts_core.winsound_player import (
    play_wav_file_async,
    stop_winsound_playback,
)
from plugins.SongPlayer.song_player_core.song_mouth_animator import (
    SongMouthAnimator,
)
from plugins.SongPlayer.song_player_core.song_rhythm_animator import (
    SongRhythmAnimator,
)


class SongPlaybackController:#20260628_kpopmodder
    def __init__(
        self,
        plugin_root,
        output_callback,
        expression_callback=None,
        status_callback=None,
    ):
        self.plugin_root = plugin_root
        self.output_callback = output_callback
        self.expression_callback = expression_callback
        self.status_callback = status_callback
        self.lock = threading.RLock()
        self.stop_event = threading.Event()
        self.thread = None
        self.current_song = None
        self.last_expression_active = False
        self.last_loud_time = 0.0
        self.expression_active_payload = None
        self.expression_refresh_thread = None
        self.expression_refresh_interval_sec = 0.05
        self.mouth_animator = SongMouthAnimator(
            output_callback=self._handle_mouth_output,
            stop_event=self.stop_event,
        )
        self.rhythm_animator = SongRhythmAnimator(#20260629_kpopmodder
            output_callback=self._send_song_expression,
            stop_event=self.stop_event,
        )

    def play(self, song):
        if song is None:
            return False, "No song selected."

        audio_path = song.resolved_audio_path(self.plugin_root)
        mouth_path = song.resolved_mouth_path(self.plugin_root)
        missing_paths = [
            path for path in (audio_path, mouth_path)
            if not path or not os.path.exists(path)
        ]
        if missing_paths:
            return False, "Missing file: " + missing_paths[0]

        self.stop(join=True)#20260628_kpopmodder: New song playback replaces the current song.

        with self.lock:
            self.stop_event.clear()
            self.current_song = song
            self.thread = threading.Thread(
                target=self._play_loop,
                args=(song, audio_path, mouth_path),
                daemon=True,
            )
            self.thread.start()

        return True, f"Playing: {song.title}"

    def stop(self, join=False):
        with self.lock:
            self.stop_event.set()
            self.mouth_animator.stop()
            self.rhythm_animator.stop()
            self._stop_winsound()

            thread = self.thread

        self._reset_song_expression(force=True)

        if (
            join
            and thread
            and thread.is_alive()
            and threading.current_thread() != thread
        ):
            try:
                thread.join(timeout=0.5)
            except KeyboardInterrupt:
                log_print("[SongPlayer] playback join skipped during Ctrl+C shutdown.")#20260630_kpopmodder

        self.output_callback(0)

    def is_playing(self):
        thread = self.thread
        return bool(thread and thread.is_alive())

    def _play_loop(self, song, audio_path, mouth_path):
        self._set_status(f"Playing: {song.title}")
        #20260628_kpopmodder: Keep song state separate from TTS AI-speaking state.
        global_state.set_value(GlobalKeys.IS_SONG_PLAYING, True)#20260628_kpopmodder

        try:
            log_print(
                "[SongPlayer] play song "
                f"title={song.title}, audio={audio_path}, mouth={mouth_path}"
            )
            self.mouth_animator.start(
                mouth_path=mouth_path,
                mouth_gain=song.mouth_gain,
                mouth_floor=song.mouth_floor,
                offset_ms=song.offset_ms,
            )
            self.rhythm_animator.start(
                audio_path=audio_path,
                song=song,
                mouth_path=mouth_path,
            )#20260629_kpopmodder: Rough beat motion follows the mixed WAV, gated by Vocal_only mouth volume.
            completed = self._play_audio_until_stopped(audio_path)#20260629_kpopmodder: Async playback lets Stop interrupt long songs.

            if self.stop_event.is_set() or not completed:
                status = f"Stopped: {song.title}"
            else:
                status = f"Finished: {song.title}"

            log_print(f"[SongPlayer] {status}")#20260629_kpopmodder: Log final playback state for Stop/finish diagnostics.
            self._set_status(status)

        except Exception as e:
            log_print(f"[SongPlayer] playback error: {e}")
            log_print(traceback.format_exc())
            self._set_status(f"Playback error: {e}")

        finally:
            self.mouth_animator.stop()
            self.rhythm_animator.stop()
            self.output_callback(0)
            self._reset_song_expression(force=True)

            with self.lock:
                if threading.current_thread() == self.thread:
                    #20260628_kpopmodder: Only the active playback thread clears song state.
                    global_state.set_value(GlobalKeys.IS_SONG_PLAYING, False)#20260628_kpopmodder
                    self.thread = None
                    self.current_song = None

    def _set_status(self, status):
        if self.status_callback is None:
            return
        try:
            self.status_callback(status)
        except Exception as e:
            log_print(f"[SongPlayer] status callback error: {e}")

    def _handle_mouth_output(self, volume):
        self.output_callback(volume)
        self._update_song_expression(volume)

    def _update_song_expression(self, volume):
        with self.lock:
            song = self.current_song

        if song is None or not song.expression_enabled:
            self._reset_song_expression()
            return

        try:
            volume = float(volume)
        except Exception:
            volume = 0.0

        now = time.time()
        if volume >= song.expression_threshold:
            self.last_loud_time = now

        hold_sec = max(0.0, float(song.expression_hold_ms or 0)) / 1000.0
        active = (
            volume >= song.expression_threshold
            or (
                self.last_loud_time > 0
                and now - self.last_loud_time <= hold_sec
            )
        )

        if active:
            self._activate_song_expression(song)
        else:
            self._reset_song_expression()

    def _activate_song_expression(self, song):
        payload = song.expression_payload(True)
        refresh_sec = max(0.01, float(song.expression_refresh_sec or 0.05))

        with self.lock:
            was_active = self.last_expression_active
            self.last_expression_active = True
            self.expression_active_payload = payload
            self.expression_refresh_interval_sec = refresh_sec

        if not was_active:
            self._send_song_expression(payload)

        self._start_expression_refresh_loop()

    def _start_expression_refresh_loop(self):
        with self.lock:
            thread = self.expression_refresh_thread
            if thread and thread.is_alive():
                return

            self.expression_refresh_thread = threading.Thread(
                target=self._expression_refresh_loop,
                daemon=True,
            )
            self.expression_refresh_thread.start()

    def _expression_refresh_loop(self):
        while not self.stop_event.is_set():
            with self.lock:
                active = self.last_expression_active
                payload = self.expression_active_payload
                refresh_sec = self.expression_refresh_interval_sec

            if not active or not payload:
                break

            if self.stop_event.wait(refresh_sec):
                break

            with self.lock:
                active = self.last_expression_active
                payload = self.expression_active_payload

            if active and payload:
                self._send_song_expression(payload)

    def _reset_song_expression(self, force=False):
        with self.lock:
            was_active = self.last_expression_active
            self.last_expression_active = False
            self.last_loud_time = 0.0
            self.expression_active_payload = None

        if not force and not was_active:
            return

        self._send_song_expression({"active": False})

    def _send_song_expression(self, payload):
        if self.expression_callback is None:
            return
        try:
            self.expression_callback(payload)
        except Exception as e:
            log_print(f"[SongPlayer] expression callback error: {e}")

    def _play_audio_until_stopped(self, audio_path):
        duration_sec = self._get_wav_duration_sec(audio_path)
        play_wav_file_async(audio_path)

        deadline = time.monotonic() + max(0.0, duration_sec)
        while not self.stop_event.is_set():
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                break

            if self.stop_event.wait(min(0.05, remaining)):
                break

        if self.stop_event.is_set():
            self._stop_winsound()
            return False

        return True

    def _stop_winsound(self):
        try:
            stop_winsound_playback()
        except Exception as e:
            log_print(f"[SongPlayer] winsound stop error: {e}")

    def _get_wav_duration_sec(self, audio_path):
        try:
            with wave.open(audio_path, "rb") as wav_file:
                framerate = wav_file.getframerate()
                if framerate <= 0:
                    raise ValueError("invalid wav framerate")
                return wav_file.getnframes() / float(framerate)
        except Exception:
            return self._get_wav_duration_sec_from_riff(audio_path)

    def _get_wav_duration_sec_from_riff(self, audio_path):
        with open(audio_path, "rb") as file:
            header = file.read(12)
            if len(header) != 12 or header[:4] != b"RIFF" or header[8:12] != b"WAVE":
                raise ValueError("invalid wav header")

            byte_rate = None
            data_size = None

            while True:
                chunk_header = file.read(8)
                if len(chunk_header) < 8:
                    break

                chunk_id = chunk_header[:4]
                chunk_size = struct.unpack("<I", chunk_header[4:8])[0]

                if chunk_id == b"fmt ":
                    chunk_data = file.read(chunk_size)
                    if len(chunk_data) >= 12:
                        byte_rate = struct.unpack("<I", chunk_data[8:12])[0]
                    else:
                        raise ValueError("invalid wav fmt chunk")
                elif chunk_id == b"data":
                    data_size = chunk_size
                    file.seek(chunk_size, os.SEEK_CUR)
                else:
                    file.seek(chunk_size, os.SEEK_CUR)

                if chunk_size % 2:
                    file.seek(1, os.SEEK_CUR)

                if byte_rate and data_size is not None:
                    break

            if not byte_rate or data_size is None:
                raise ValueError("missing wav duration metadata")

            return data_size / float(byte_rate)
