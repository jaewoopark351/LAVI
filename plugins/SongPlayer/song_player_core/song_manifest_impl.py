#20260717_kpopmodder: Isolates SongPlayer manifest loading and validation behavior.
import json
import os

from core.logger import log_print

from .song_entry import SongEntry
from .song_manifest_helpers import _as_bool, _as_float, _as_int, _plugin_root_from_here


class SongManifest:
    def __init__(
        self,
        plugin_root=None,
        config_path=None,
        example_path=None,
    ):
        self.plugin_root = plugin_root or _plugin_root_from_here()
        self.config_path = config_path or os.path.join(
            self.plugin_root,
            "config",
            "song_player_songs.json",
        )
        self.example_path = example_path or os.path.join(
            self.plugin_root,
            "config",
            "song_player_songs.example.json",
        )
        self.songs = []
        self.loaded_path = ""
        self.last_error = ""
        self.missing_files = []#20260629_kpopmodder: Keep invalid manifest rows visible in UI status.

    def load(self):
        self.songs = []
        self.last_error = ""
        self.missing_files = []
        self.loaded_path = self.find_manifest_path()

        if not self.loaded_path:
            self.last_error = self.missing_manifest_message()
            return []

        try:
            with open(self.loaded_path, "r", encoding="utf-8") as file:
                payload = json.load(file)

            raw_songs = payload.get("songs", [])
            if not isinstance(raw_songs, list):
                raise ValueError("songs must be a list")

            songs = []
            for raw_song in raw_songs:
                song = self.parse_song(raw_song)
                if song is None:
                    continue

                missing_files = self.find_missing_files(song)
                if missing_files:
                    self.missing_files.extend(missing_files)
                    continue

                songs.append(song)

            self.songs = songs
            if self.missing_files:
                log_print(
                    "[SongPlayer] skipped songs with missing files: "
                    + "; ".join(self.missing_files)
                )
            return list(self.songs)

        except Exception as e:
            self.last_error = f"SongPlayer manifest load failed: {e}"
            log_print(f"[SongPlayer] {self.last_error}")
            return []

    def find_manifest_path(self):
        if os.path.exists(self.config_path):
            return self.config_path
        return ""

    def missing_manifest_message(self):
        config_path = self.display_path(self.config_path)
        if os.path.exists(self.example_path):
            return (
                "No SongPlayer manifest found. Copy "
                f"{self.display_path(self.example_path)} to {config_path} "
                "and update audio_path/mouth_path."
            )
        return f"No SongPlayer manifest found. Create {config_path}."

    def find_missing_files(self, song):
        missing_files = []
        audio_path = song.resolved_audio_path(self.plugin_root)
        mouth_path = song.resolved_mouth_path(self.plugin_root)

        if not os.path.isfile(audio_path):
            missing_files.append(
                f"{song.title} audio_path={self.display_path(audio_path)}"
            )
        if not os.path.isfile(mouth_path):
            missing_files.append(
                f"{song.title} mouth_path={self.display_path(mouth_path)}"
            )

        return missing_files

    def display_path(self, path):
        try:
            return os.path.relpath(path, self.plugin_root)
        except Exception:
            return str(path or "")

    def missing_files_text(self, max_items=6):
        items = list(self.missing_files)
        visible_items = items[:max_items]
        text = "; ".join(visible_items)
        hidden_count = len(items) - len(visible_items)
        if hidden_count > 0:
            text += f"; and {hidden_count} more"
        return text

    def parse_song(self, raw_song):
        if not isinstance(raw_song, dict):
            return None

        title = str(raw_song.get("title", "")).strip()
        audio_path = str(raw_song.get("audio_path", "")).strip()
        mouth_path = str(raw_song.get("mouth_path", "")).strip()

        if not title or not audio_path or not mouth_path:
            return None

        song_id = str(raw_song.get("id", "")).strip() or title
        mouth_gain = max(0.0, _as_float(raw_song.get("mouth_gain"), 1.0))
        mouth_floor = min(
            1.0,
            max(0.0, _as_float(raw_song.get("mouth_floor"), 0.05)),
        )
        expression_threshold = min(
            1.0,
            max(0.0, _as_float(raw_song.get("expression_threshold"), 0.65)),
        )
        expression_eye_open = min(
            1.0,
            max(0.0, _as_float(raw_song.get("expression_eye_open"), 0.0)),
        )
        expression_mouth_smile = min(
            1.0,
            max(0.0, _as_float(raw_song.get("expression_mouth_smile"), 0.0)),
        )
        expression_refresh_sec = max(
            0.01,
            _as_float(raw_song.get("expression_refresh_sec"), 0.05),
        )
        rhythm_threshold = min(
            1.0,
            max(0.0, _as_float(raw_song.get("rhythm_threshold"), 0.35)),
        )

        return SongEntry(
            song_id=song_id,
            title=title,
            audio_path=audio_path,
            mouth_path=mouth_path,
            offset_ms=_as_int(raw_song.get("offset_ms"), 0),
            mouth_gain=mouth_gain,
            mouth_floor=mouth_floor,
            expression_enabled=_as_bool(
                raw_song.get("expression_enabled"),
                True,
            ),
            expression_threshold=expression_threshold,
            expression_hold_ms=max(
                0,
                _as_int(raw_song.get("expression_hold_ms"), 250),
            ),
            expression_refresh_sec=expression_refresh_sec,
            expression_eye_open=expression_eye_open,
            expression_mouth_smile=expression_mouth_smile,
            expression_face_angle_x=max(
                -30.0,
                min(
                    30.0,
                    _as_float(raw_song.get("expression_face_angle_x"), -6.0),
                ),
            ),
            expression_face_angle_y=max(
                -30.0,
                min(
                    30.0,
                    _as_float(raw_song.get("expression_face_angle_y"), -10.0),
                ),
            ),
            rhythm_enabled=_as_bool(
                raw_song.get("rhythm_enabled"),
                True,
            ),
            rhythm_threshold=rhythm_threshold,
            rhythm_min_interval_ms=max(
                120,
                _as_int(raw_song.get("rhythm_min_interval_ms"), 280),
            ),
            rhythm_pulse_ms=max(
                40,
                _as_int(raw_song.get("rhythm_pulse_ms"), 160),
            ),
            rhythm_face_angle_z=max(
                -30.0,
                min(
                    30.0,
                    _as_float(raw_song.get("rhythm_face_angle_z"), 10.0),
                ),
            ),
        )

    def get_titles(self):
        return [song.title for song in self.songs]

    def find_by_title(self, title):
        if not self.songs:
            self.load()

        title = str(title or "").strip()
        for song in self.songs:
            if song.title == title:
                return song

        return None

    def get_initial_title(self):
        if not self.songs:
            self.load()
        if not self.songs:
            return None
        return self.songs[0].title

    def status_text(self):
        if self.last_error:
            return self.last_error
        if not self.songs:
            if self.missing_files:
                return (
                    "No playable SongPlayer songs configured. Missing files: "
                    f"{self.missing_files_text()}"
                )
            return "No SongPlayer songs configured."
        loaded_from = os.path.relpath(self.loaded_path, self.plugin_root)
        status = f"Ready. Songs={len(self.songs)} manifest={loaded_from}"
        if self.missing_files:
            status += f" Skipped missing files: {self.missing_files_text()}"
        return status
