import sys
import unittest
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


from plugin_system.interfaces import VtuberPluginInterface
from plugins.VtubeStudio.VtubeStudio import VtubeStudio
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_auth_manager import (
    VTubeStudioAuthManager,
)
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_blink_controller import (
    VTubeStudioBlinkController,
)
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_mouth_controller import (
    VTubeStudioMouthController,
)
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_smile_controller import (
    VTubeStudioSmileController,
)
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_song_expression_controller import (
    VTubeStudioSongExpressionController,
)


class VTubeStudioImportTests(unittest.TestCase):
    def test_plugin_entry_class_imports(self):
        self.assertTrue(issubclass(VtubeStudio, VtuberPluginInterface))

    def test_authentication_request_uses_current_token(self):
        sent_messages = []
        manager = VTubeStudioAuthManager(
            token_path="unused-token-path.txt",
            send_callback=sent_messages.append,
            authenticated_callback=lambda: None,
        )
        manager.token = "test-token"

        manager.send_authentication_request()

        self.assertEqual(len(sent_messages), 1)
        request = sent_messages[0]
        self.assertEqual(request["messageType"], "AuthenticationRequest")
        self.assertEqual(
            request["data"]["authenticationToken"],
            "test-token",
        )

    def test_mouth_value_is_clamped_without_websocket_connection(self):
        sent_messages = []
        controller = VTubeStudioMouthController(
            send_callback=sent_messages.append,
            connected_callback=lambda: False,
            authenticated_callback=lambda: False,
            avatar_data_callback=lambda: None,
        )

        controller.set_mouth_open(1.5)
        controller.set_mouth_open(-0.5)

        values = [
            message["data"]["parameterValues"][0]["value"]
            for message in sent_messages
        ]
        self.assertEqual(values, [1.0, 0.0])

    def test_blink_controller_sets_both_eye_open_parameters(self):
        sent_messages = []
        controller = VTubeStudioBlinkController(
            send_callback=sent_messages.append,
            connected_callback=lambda: True,
            authenticated_callback=lambda: True,
            close_sec=0,
            close_transition_sec=0,
            open_transition_sec=0,
            transition_steps=1,
        )

        self.assertTrue(controller.blink_once())

        values = [
            [
                (parameter["id"], parameter["value"])
                for parameter in message["data"]["parameterValues"]
            ]
            for message in sent_messages
        ]
        self.assertEqual(
            values,
            [
                [("EyeOpenLeft", 0.0), ("EyeOpenRight", 0.0)],
                [("EyeOpenLeft", 0.52), ("EyeOpenRight", 0.52)],
            ],
        )

    def test_blink_controller_can_send_gradual_eye_values(self):
        sent_messages = []
        controller = VTubeStudioBlinkController(
            send_callback=sent_messages.append,
            connected_callback=lambda: True,
            authenticated_callback=lambda: True,
            close_sec=0,
            close_transition_sec=0,
            open_transition_sec=0,
            transition_steps=4,
        )

        self.assertTrue(controller.blink_once())

        values = [
            message["data"]["parameterValues"][0]["value"]
            for message in sent_messages
        ]
        expected_values = [
            0.39,
            0.26,
            0.13,
            0.0,
            0.13,
            0.26,
            0.39,
            0.52,
        ]
        self.assertEqual(len(values), len(expected_values))
        for value, expected_value in zip(values, expected_values):
            self.assertAlmostEqual(value, expected_value)

    def test_smile_controller_sets_mouth_smile_parameter(self):
        sent_messages = []
        controller = VTubeStudioSmileController(
            send_callback=sent_messages.append,
            connected_callback=lambda: True,
            authenticated_callback=lambda: True,
        )

        self.assertTrue(controller.apply_smile_once())

        self.assertEqual(len(sent_messages), 1)
        message = sent_messages[0]
        self.assertEqual(message["messageType"], "InjectParameterDataRequest")
        self.assertEqual(
            message["data"]["parameterValues"],
            [{"id": "MouthSmile", "value": 1.0}],
        )

    def test_smile_value_is_clamped_without_websocket_connection(self):
        sent_messages = []
        controller = VTubeStudioSmileController(
            send_callback=sent_messages.append,
            connected_callback=lambda: False,
            authenticated_callback=lambda: False,
        )

        controller.set_mouth_smile(1.5)
        controller.set_mouth_smile(-0.5)

        values = [
            message["data"]["parameterValues"][0]["value"]
            for message in sent_messages
        ]
        self.assertEqual(values, [1.0, 0.0])

    def test_smile_controller_stop_keeps_default_smile(self):
        sent_messages = []
        controller = VTubeStudioSmileController(
            send_callback=sent_messages.append,
            connected_callback=lambda: False,
            authenticated_callback=lambda: False,
        )

        controller.stop()

        self.assertEqual(
            sent_messages[-1]["data"]["parameterValues"],
            [{"id": "MouthSmile", "value": 1.0}],
        )

    def test_smile_controller_uses_fast_refresh_to_hold_value(self):
        controller = VTubeStudioSmileController(
            send_callback=lambda message: None,
            connected_callback=lambda: False,
            authenticated_callback=lambda: False,
        )

        self.assertLessEqual(controller.refresh_interval_sec, 0.05)

    def test_song_expression_controller_sends_loud_pose_parameters(self):
        sent_messages = []
        controller = VTubeStudioSongExpressionController(
            send_callback=sent_messages.append,
            connected_callback=lambda: True,
            authenticated_callback=lambda: True,
        )

        self.assertTrue(controller.apply_song_expression({
            "active": True,
            "eye_open": 0.0,
            "mouth_smile": 0.0,
            "face_angle_x": -6.0,
            "face_angle_y": -10.0,
        }))

        self.assertTrue(controller.is_active())
        self.assertEqual(
            sent_messages[0]["data"]["parameterValues"],
            [
                {"id": "EyeOpenLeft", "value": 0.0},
                {"id": "EyeOpenRight", "value": 0.0},
                {"id": "MouthSmile", "value": 0.0},
                {"id": "FaceAngleX", "value": -6.0},
                {"id": "FaceAngleY", "value": -10.0},
            ],
        )

    def test_song_expression_controller_resets_pose_parameters(self):
        sent_messages = []
        controller = VTubeStudioSongExpressionController(
            send_callback=sent_messages.append,
            connected_callback=lambda: True,
            authenticated_callback=lambda: True,
        )

        controller.apply_song_expression({"active": True})
        controller.reset()

        self.assertFalse(controller.is_active())
        self.assertEqual(
            sent_messages[-1]["data"]["parameterValues"],
            [
                {"id": "EyeOpenLeft", "value": 0.5},
                {"id": "EyeOpenRight", "value": 0.5},
                {"id": "MouthSmile", "value": 1.0},
                {"id": "FaceAngleX", "value": 0.0},
                {"id": "FaceAngleY", "value": 0.0},
                {"id": "FaceAngleZ", "value": 0.0},
            ],
        )

    def test_song_expression_controller_sends_rhythm_face_angle_z(self):
        sent_messages = []
        controller = VTubeStudioSongExpressionController(
            send_callback=sent_messages.append,
            connected_callback=lambda: True,
            authenticated_callback=lambda: True,
        )

        self.assertTrue(controller.apply_song_expression({
            "rhythm_active": True,
            "face_angle_z": 7.5,
        }))

        self.assertFalse(controller.is_active())
        self.assertEqual(
            sent_messages[0]["data"]["parameterValues"],
            [{"id": "FaceAngleZ", "value": 7.5}],
        )

    def test_blink_controller_skips_when_song_expression_overrides(self):
        sent_messages = []
        controller = VTubeStudioBlinkController(
            send_callback=sent_messages.append,
            connected_callback=lambda: True,
            authenticated_callback=lambda: True,
            override_callback=lambda: True,
        )

        self.assertFalse(controller.blink_once())
        self.assertEqual([], sent_messages)

    def test_smile_controller_skips_when_song_expression_overrides(self):
        sent_messages = []
        controller = VTubeStudioSmileController(
            send_callback=sent_messages.append,
            connected_callback=lambda: True,
            authenticated_callback=lambda: True,
            override_callback=lambda: True,
        )

        self.assertFalse(controller.apply_smile_once())
        self.assertEqual([], sent_messages)


if __name__ == "__main__":
    unittest.main()
