#20260717_kpopmodder: Isolates SongPlayer manifest entry DTO.
from dataclasses import dataclass

from .song_manifest_helpers import resolve_song_path


@dataclass(frozen=True)
class SongEntry:#20260628_kpopmodder
    song_id: str
    title: str
    audio_path: str
    mouth_path: str
    offset_ms: int = 0
    mouth_gain: float = 1.0
    mouth_floor: float = 0.05
    expression_enabled: bool = True
    expression_threshold: float = 0.65
    expression_hold_ms: int = 250
    expression_refresh_sec: float = 0.05
    expression_eye_open: float = 0.0
    expression_mouth_smile: float = 0.0
    expression_face_angle_x: float = -6.0
    expression_face_angle_y: float = -10.0
    rhythm_enabled: bool = True
    rhythm_threshold: float = 0.35
    rhythm_min_interval_ms: int = 280
    rhythm_pulse_ms: int = 160
    rhythm_face_angle_z: float = 10.0

    def resolved_audio_path(self, plugin_root):
        return resolve_song_path(plugin_root, self.audio_path)

    def resolved_mouth_path(self, plugin_root):
        return resolve_song_path(plugin_root, self.mouth_path)

    def expression_payload(self, active):
        return {
            "active": bool(active),
            "eye_open": self.expression_eye_open,
            "mouth_smile": self.expression_mouth_smile,
            "face_angle_x": self.expression_face_angle_x,
            "face_angle_y": self.expression_face_angle_y,
        }
