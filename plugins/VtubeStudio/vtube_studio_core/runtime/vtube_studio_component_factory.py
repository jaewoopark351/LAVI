#20260904_kpopmodder: Construct and wire VTube Studio runtime collaborators in one composition boundary.
import os

from core.event_manager import event_manager
from core.global_state import GlobalKeys, global_state
from plugins.VtubeStudio.vtube_studio_core.authentication import (
    VTubeStudioAuthManager,
    VTubeStudioTokenStore,
)
from plugins.VtubeStudio.vtube_studio_core.configuration import (
    load_idle_pose_config,
    load_speaking_pose_config,
)
from plugins.VtubeStudio.vtube_studio_core.connection import VTubeStudioConnection
from plugins.VtubeStudio.vtube_studio_core.controllers.mouth import (
    VTubeStudioMouthController,
)
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_blink_controller import (
    VTubeStudioBlinkController,
)
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_idle_pose_controller import (
    VTubeStudioIdlePoseController,
)
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_smile_controller import (
    VTubeStudioSmileController,
)
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_song_expression_controller import (
    VTubeStudioSongExpressionController,
)
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_speaking_pose_controller import (
    VTubeStudioSpeakingPoseController,
)
from .vtube_studio_runtime import VTubeStudioRuntime


class VTubeStudioComponentFactory:
    def __init__(
        self,
        websocket_url="ws://localhost:8001",
        event_manager_instance=None,
        connection_factory=None,
    ):
        self.websocket_url = websocket_url
        self.event_manager = event_manager_instance or event_manager
        self.connection_factory = connection_factory or VTubeStudioConnection

    def create(self, module_directory, avatar_data_callback, callback_owner=None):
        token_path = os.path.join(module_directory, "token.txt")
        speaking_config = load_speaking_pose_config(
            os.path.join(module_directory, "config", "speaking_pose.json")
        )
        idle_config = load_idle_pose_config(
            os.path.join(module_directory, "config", "idle_pose.json")
        )

        runtime_reference = {}

        def route_callback(method_name, runtime_callback, *args):
            owner_callback = getattr(callback_owner, method_name, None)
            if callable(owner_callback):
                return owner_callback(*args)
            return runtime_callback()

        connection = self.connection_factory(
            websocket_url=self.websocket_url,
            on_open=lambda ws, attempt: route_callback(
                "on_open",
                lambda: runtime_reference["runtime"].on_open(ws, attempt),
                ws,
            ),
            on_message=lambda ws, message, attempt: route_callback(
                "on_message",
                lambda: runtime_reference["runtime"].on_message(
                    ws,
                    message,
                    attempt,
                ),
                ws,
                message,
            ),
            on_error=lambda ws, error, attempt: route_callback(
                "on_error",
                lambda: runtime_reference["runtime"].on_error(ws, error, attempt),
                ws,
                error,
            ),
            on_close=lambda ws, code, message, attempt: route_callback(
                "on_close",
                lambda: runtime_reference["runtime"].on_close(
                    ws,
                    code,
                    message,
                    attempt,
                ),
                ws,
                code,
                message,
            ),
            reset_authentication_callback=lambda attempt: runtime_reference[
                "runtime"
            ].reset_authentication(attempt),
            attempt_aware_callbacks=True,
        )
        token_store = VTubeStudioTokenStore(token_path)
        auth_manager = VTubeStudioAuthManager(
            token_path=token_path,
            token_store=token_store,
            send_callback=connection.safe_send,
            attempt_send_callback=connection.safe_send_auth,
            authenticated_callback=lambda: None,
            attempt_authenticated_callback=lambda attempt: runtime_reference[
                "runtime"
            ].on_authenticated(attempt),
            attempt_failed_callback=lambda attempt, reason: connection.disconnect_attempt(
                attempt,
                reason,
            ),
            attempt_rejected_callback=lambda attempt, reason: runtime_reference[
                "runtime"
            ].on_authentication_rejected(attempt, reason),
        )

        authenticated = connection.is_attempt_authenticated
        connected = lambda: connection.connected
        send_control = connection.safe_send_control

        mouth_controller = VTubeStudioMouthController(
            send_callback=send_control,
            connected_callback=connected,
            authenticated_callback=authenticated,
            avatar_data_callback=avatar_data_callback,
        )
        song_expression_controller = VTubeStudioSongExpressionController(#20260628_kpopmodder
            send_callback=send_control,
            connected_callback=connected,
            authenticated_callback=authenticated,
        )
        blink_controller = VTubeStudioBlinkController(#20260628_kpopmodder
            send_callback=send_control,
            connected_callback=connected,
            authenticated_callback=authenticated,
            override_callback=song_expression_controller.is_active,
        )
        speaking_pose_controller = VTubeStudioSpeakingPoseController(#20260628_kpopmodder
            send_callback=send_control,
            connected_callback=connected,
            authenticated_callback=authenticated,
            avatar_data_callback=avatar_data_callback,
            eye_open=speaking_config["eye_open"],
            face_angle_x=speaking_config["face_angle_x"],
            face_angle_y=speaking_config["face_angle_y"],
            face_angle_z=speaking_config["face_angle_z"],
            eye_ball_x=speaking_config["eye_ball_x"],
            eye_ball_y=speaking_config["eye_ball_y"],
            mouth_smile=speaking_config["mouth_smile"],
            reset_eye_open=speaking_config["reset_eye_open"],
            reset_face_angle_x=speaking_config["reset_face_angle_x"],
            reset_face_angle_y=speaking_config["reset_face_angle_y"],
            reset_face_angle_z=speaking_config["reset_face_angle_z"],
            reset_eye_ball_x=speaking_config["reset_eye_ball_x"],
            reset_eye_ball_y=speaking_config["reset_eye_ball_y"],
            reset_mouth_smile=speaking_config["reset_mouth_smile"],
            mouth_threshold=speaking_config["mouth_threshold"],
            release_hold_sec=speaking_config["release_hold_sec"],
            refresh_interval_sec=speaking_config["refresh_interval_sec"],
            speaking_callback=lambda: global_state.get_value(
                GlobalKeys.IS_AI_SPEAKING,
                False,
            ),
            override_callback=song_expression_controller.is_active,
            eye_open_override_callback=blink_controller.is_blinking,
        )
        idle_pose_controller = VTubeStudioIdlePoseController(#20260628_kpopmodder
            send_callback=send_control,
            connected_callback=connected,
            authenticated_callback=authenticated,
            eye_open=idle_config["eye_open"],
            face_angle_x=idle_config["face_angle_x"],
            face_angle_y=idle_config["face_angle_y"],
            face_angle_z=idle_config["face_angle_z"],
            eye_ball_x=idle_config["eye_ball_x"],
            eye_ball_y=idle_config["eye_ball_y"],
            mouth_smile=idle_config["mouth_smile"],
            reset_eye_open=idle_config["reset_eye_open"],
            reset_face_angle_x=idle_config["reset_face_angle_x"],
            reset_face_angle_y=idle_config["reset_face_angle_y"],
            reset_face_angle_z=idle_config["reset_face_angle_z"],
            reset_eye_ball_x=idle_config["reset_eye_ball_x"],
            reset_eye_ball_y=idle_config["reset_eye_ball_y"],
            reset_mouth_smile=idle_config["reset_mouth_smile"],
            refresh_interval_sec=idle_config["refresh_interval_sec"],
            idle_callback=lambda: not global_state.get_value(
                GlobalKeys.IS_AI_SPEAKING,
                False,
            ),
            override_callback=lambda: (
                song_expression_controller.is_active()
                or speaking_pose_controller.is_active()
            ),
            eye_open_override_callback=blink_controller.is_blinking,
        )
        smile_controller = VTubeStudioSmileController(#20260628_kpopmodder
            send_callback=send_control,
            connected_callback=connected,
            authenticated_callback=authenticated,
            override_callback=lambda: (
                song_expression_controller.is_active()
                or speaking_pose_controller.is_active()
                or idle_pose_controller.is_active()
            ),
        )

        runtime = VTubeStudioRuntime(
            connection=connection,
            auth_manager=auth_manager,
            mouth_controller=mouth_controller,
            song_expression_controller=song_expression_controller,
            blink_controller=blink_controller,
            speaking_pose_controller=speaking_pose_controller,
            idle_pose_controller=idle_pose_controller,
            smile_controller=smile_controller,
            event_manager=self.event_manager,
        )
        runtime_reference["runtime"] = runtime
        return runtime, token_store
