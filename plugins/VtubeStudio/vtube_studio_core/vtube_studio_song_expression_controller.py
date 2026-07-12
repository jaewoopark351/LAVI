#20260628_kpopmodder: Song expression controller isolates performance-only VTube Studio parameter overrides.
import threading

from core.logger import log_print


class VTubeStudioSongExpressionController:#20260628_kpopmodder
    def __init__(
        self,
        send_callback,
        connected_callback,
        authenticated_callback,
        reset_eye_open=0.5,
        reset_mouth_smile=1.0,
        reset_face_angle_x=0.0,
        reset_face_angle_y=0.0,
        reset_face_angle_z=0.0,
    ):
        self.send_callback = send_callback
        self.connected_callback = connected_callback
        self.authenticated_callback = authenticated_callback
        self.reset_eye_open = reset_eye_open
        self.reset_mouth_smile = reset_mouth_smile
        self.reset_face_angle_x = reset_face_angle_x
        self.reset_face_angle_y = reset_face_angle_y
        self.reset_face_angle_z = reset_face_angle_z

        self.lock = threading.RLock()
        self.active = False
        self.rhythm_active = False

    def is_active(self):
        with self.lock:
            return self.active

    def apply_song_expression(self, expression):
        expression = expression if isinstance(expression, dict) else {}
        has_rhythm = "rhythm_active" in expression
        has_expression = "active" in expression or not has_rhythm
        values = []

        with self.lock:
            if has_expression:
                self.active = bool(expression.get("active", False))
            if has_rhythm:
                self.rhythm_active = bool(
                    expression.get("rhythm_active", False)
                )

            active = self.active
            rhythm_active = self.rhythm_active

        if has_expression:
            if active:
                values.extend(self._active_values(expression))
            else:
                values.extend(self._reset_values())

        if has_rhythm:
            if rhythm_active:
                values.extend(self._rhythm_values(expression))
            else:
                values.extend(self._rhythm_reset_values())

        if not values:
            return False

        if (
            not self.connected_callback()
            or not self.authenticated_callback()
        ):
            return False

        try:
            self.send_callback({
                "apiName": "VTubeStudioPublicAPI",
                "apiVersion": "1.0",
                "requestID": "song_expression_set",
                "messageType": "InjectParameterDataRequest",
                "data": {
                    "mode": "set",
                    "parameterValues": values,
                },
            })
            return True
        except Exception as e:
            log_print(f"[VtubeStudio] song expression send error ignored: {e}")
            return False

    def reset(self):
        return self.apply_song_expression({
            "active": False,
            "rhythm_active": False,
        })

    def _active_values(self, expression):
        eye_open = _clamp_float(expression.get("eye_open", 0.0), 0.0, 1.0)
        mouth_smile = _clamp_float(
            expression.get("mouth_smile", 0.0),
            0.0,
            1.0,
        )
        face_angle_x = _clamp_float(
            expression.get("face_angle_x", -6.0),
            -30.0,
            30.0,
        )
        face_angle_y = _clamp_float(
            expression.get("face_angle_y", -10.0),
            -30.0,
            30.0,
        )

        return [
            {"id": "EyeOpenLeft", "value": eye_open},
            {"id": "EyeOpenRight", "value": eye_open},
            {"id": "MouthSmile", "value": mouth_smile},
            {"id": "FaceAngleX", "value": face_angle_x},
            {"id": "FaceAngleY", "value": face_angle_y},
        ]

    def _rhythm_values(self, expression):
        face_angle_z = _clamp_float(
            expression.get("face_angle_z", 0.0),
            -30.0,
            30.0,
        )
        return [{"id": "FaceAngleZ", "value": face_angle_z}]

    def _rhythm_reset_values(self):
        return [
            {
                "id": "FaceAngleZ",
                "value": _clamp_float(self.reset_face_angle_z, -30.0, 30.0),
            },
        ]

    def _reset_values(self):
        return [
            {
                "id": "EyeOpenLeft",
                "value": _clamp_float(self.reset_eye_open, 0.0, 1.0),
            },
            {
                "id": "EyeOpenRight",
                "value": _clamp_float(self.reset_eye_open, 0.0, 1.0),
            },
            {
                "id": "MouthSmile",
                "value": _clamp_float(self.reset_mouth_smile, 0.0, 1.0),
            },
            {
                "id": "FaceAngleX",
                "value": _clamp_float(self.reset_face_angle_x, -30.0, 30.0),
            },
            {
                "id": "FaceAngleY",
                "value": _clamp_float(self.reset_face_angle_y, -30.0, 30.0),
            },
        ]


def _clamp_float(value, minimum, maximum):
    try:
        value = float(value)
    except Exception:
        value = minimum
    return max(minimum, min(maximum, value))
