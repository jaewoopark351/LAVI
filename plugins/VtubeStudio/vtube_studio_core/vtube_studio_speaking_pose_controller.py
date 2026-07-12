#20260628_kpopmodder: Speaking pose controller adds a light talk posture without changing MouthOpen sync.
import threading
import time

from core.logger import log_print


class VTubeStudioSpeakingPoseController:#20260628_kpopmodder
    def __init__(
        self,
        send_callback,
        connected_callback,
        authenticated_callback,
        avatar_data_callback,
        eye_open=0.4,#20260707_kpopmodder
        face_angle_x=-4.0,
        face_angle_y=-6.0,
        face_angle_z=-4.0,
        eye_ball_x=0.35,
        eye_ball_y=-0.35,
        mouth_smile=0.0,
        reset_eye_open=0.5,
        reset_face_angle_x=0.0,
        reset_face_angle_y=0.0,
        reset_face_angle_z=0.0,
        reset_eye_ball_x=0.0,
        reset_eye_ball_y=0.0,
        reset_mouth_smile=1.0,
        mouth_threshold=0.04,
        release_hold_sec=0.15,
        refresh_interval_sec=0.05,
        speaking_callback=None,
        override_callback=None,
        eye_open_override_callback=None,
        join_timeout=0.3,
    ):
        self.send_callback = send_callback
        self.connected_callback = connected_callback
        self.authenticated_callback = authenticated_callback
        self.avatar_data_callback = avatar_data_callback
        self.eye_open = eye_open
        self.face_angle_x = face_angle_x
        self.face_angle_y = face_angle_y
        self.face_angle_z = face_angle_z
        self.eye_ball_x = eye_ball_x
        self.eye_ball_y = eye_ball_y
        self.mouth_smile = mouth_smile
        self.reset_eye_open = reset_eye_open
        self.reset_face_angle_x = reset_face_angle_x
        self.reset_face_angle_y = reset_face_angle_y
        self.reset_face_angle_z = reset_face_angle_z
        self.reset_eye_ball_x = reset_eye_ball_x
        self.reset_eye_ball_y = reset_eye_ball_y
        self.reset_mouth_smile = reset_mouth_smile
        self.mouth_threshold = float(mouth_threshold)
        self.release_hold_sec = float(release_hold_sec)
        self.refresh_interval_sec = float(refresh_interval_sec)
        self.speaking_callback = speaking_callback or (lambda: True)
        self.override_callback = override_callback or (lambda: False)
        self.eye_open_override_callback = eye_open_override_callback or (lambda: False)
        self.join_timeout = join_timeout

        self.lock = threading.RLock()
        self.thread = None
        self.stop_event = threading.Event()
        self.active = False
        self.last_mouth_active_time = 0.0

    def start_if_needed(self):
        if self.thread and self.thread.is_alive():
            return

        self.stop_event.clear()
        self.thread = threading.Thread(
            target=self.speaking_pose_loop,
            daemon=True,
        )
        self.thread.start()

    def stop(self):
        self.stop_event.set()

        try:
            if (
                self.thread
                and self.thread.is_alive()
                and threading.current_thread() != self.thread
            ):
                self.thread.join(timeout=self.join_timeout)
        except Exception as e:
            log_print(f"[VtubeStudio] speaking pose stop error ignored: {e}")

        try:
            self.reset()
        except Exception as e:
            log_print(f"[VtubeStudio] speaking pose reset error ignored: {e}")

    def is_active(self):
        with self.lock:
            return self.active

    def speaking_pose_loop(self):
        while not self.stop_event.is_set():
            try:
                self.apply_speaking_pose_once()
            except Exception as e:
                log_print(f"[VtubeStudio] speaking pose loop error ignored: {e}")

            if self.stop_event.wait(self.refresh_interval_sec):
                break

    def apply_speaking_pose_once(self):
        if (
            not self.connected_callback()
            or not self.authenticated_callback()
        ):
            return False

        if self.is_overridden():
            self._clear_active_state()
            return False

        if not self.is_speaking_allowed():
            if self.is_active():
                self.reset()
            return False

        mouth_open = self._current_mouth_open()
        now = time.monotonic()

        with self.lock:
            if mouth_open >= self.mouth_threshold:
                self.last_mouth_active_time = now

            should_apply = (
                mouth_open >= self.mouth_threshold
                or (
                    self.active
                    and self.last_mouth_active_time > 0
                    and now - self.last_mouth_active_time <= self.release_hold_sec
                )
            )

            was_active = self.active
            self.active = should_apply

        if should_apply:
            self.set_speaking_pose()
            return True

        if was_active:
            self.reset()

        return False

    def reset(self):
        self._clear_active_state()
        self.send_pose(
            eye_open=self.reset_eye_open,
            face_angle_x=self.reset_face_angle_x,
            face_angle_y=self.reset_face_angle_y,
            face_angle_z=self.reset_face_angle_z,
            eye_ball_x=self.reset_eye_ball_x,
            eye_ball_y=self.reset_eye_ball_y,
            mouth_smile=self.reset_mouth_smile,
            request_id="speaking_pose_reset",
        )

    def set_speaking_pose(self):
        self.send_pose(
            eye_open=self.eye_open,
            face_angle_x=self.face_angle_x,
            face_angle_y=self.face_angle_y,
            face_angle_z=self.face_angle_z,
            eye_ball_x=self.eye_ball_x,
            eye_ball_y=self.eye_ball_y,
            mouth_smile=self.mouth_smile,
            request_id="speaking_pose_set",
        )

    def send_pose(
        self,
        eye_open,
        face_angle_x,
        face_angle_y,
        face_angle_z,
        eye_ball_x,
        eye_ball_y,
        mouth_smile,
        request_id,
    ):
        eye_open = _clamp_float(eye_open, 0.0, 1.0)
        face_angle_x = _clamp_float(face_angle_x, -30.0, 30.0)
        face_angle_y = _clamp_float(face_angle_y, -30.0, 30.0)
        face_angle_z = _clamp_float(face_angle_z, -30.0, 30.0)
        eye_ball_x = _clamp_float(eye_ball_x, -1.0, 1.0)
        eye_ball_y = _clamp_float(eye_ball_y, -1.0, 1.0)
        mouth_smile = _clamp_float(mouth_smile, 0.0, 1.0)

        parameter_values = []
        if not self.is_eye_open_overridden():
            parameter_values.extend([
                {"id": "EyeOpenLeft", "value": eye_open},
                {"id": "EyeOpenRight", "value": eye_open},
            ])

        parameter_values.extend([
            {"id": "FaceAngleX", "value": face_angle_x},
            {"id": "FaceAngleY", "value": face_angle_y},
            {"id": "FaceAngleZ", "value": face_angle_z},
            {"id": "EyeRightX", "value": eye_ball_x},
            {"id": "EyeRightY", "value": eye_ball_y},
            {"id": "MouthSmile", "value": mouth_smile},
        ])

        self.send_callback({
            "apiName": "VTubeStudioPublicAPI",
            "apiVersion": "1.0",
            "requestID": request_id,
            "messageType": "InjectParameterDataRequest",
            "data": {
                "mode": "set",
                "parameterValues": parameter_values,
            },
        })

    def is_overridden(self):
        try:
            return bool(self.override_callback())
        except Exception as e:
            log_print(f"[VtubeStudio] speaking pose override check error ignored: {e}")
            return False

    def is_speaking_allowed(self):
        try:
            return bool(self.speaking_callback())
        except Exception as e:
            log_print(f"[VtubeStudio] speaking pose state check error ignored: {e}")
            return False

    def is_eye_open_overridden(self):
        try:
            return bool(self.eye_open_override_callback())
        except Exception as e:
            log_print(f"[VtubeStudio] speaking pose eye override check error ignored: {e}")
            return False

    def _current_mouth_open(self):
        try:
            avatar_data = self.avatar_data_callback()
            return _clamp_float(getattr(avatar_data, "mouth_open", 0.0), 0.0, 1.0)
        except Exception:
            return 0.0

    def _clear_active_state(self):
        with self.lock:
            self.active = False
            self.last_mouth_active_time = 0.0


def _clamp_float(value, minimum, maximum):
    try:
        value = float(value)
    except Exception:
        value = minimum
    return max(minimum, min(maximum, value))
