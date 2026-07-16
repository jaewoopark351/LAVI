import json
import os
import gradio as gr

from plugin_system.interfaces import VtuberPluginInterface
from core.logger import log_print
from core.event_manager import event_manager, EventType
from core.global_state import GlobalKeys, global_state
#20260620_kpopmodder: Import grouped VTube Studio helpers from vtube_studio_core.
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_auth_manager import VTubeStudioAuthManager
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_connection import VTubeStudioConnection
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_blink_controller import (
    VTubeStudioBlinkController,
)
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_mouth_controller import (
    VTubeStudioMouthController,
)
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_smile_controller import (
    VTubeStudioSmileController,
)
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_idle_pose_controller import (
    VTubeStudioIdlePoseController,
)
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_speaking_pose_controller import (
    VTubeStudioSpeakingPoseController,
)
from plugins.VtubeStudio.vtube_studio_core.vtube_studio_song_expression_controller import (
    VTubeStudioSongExpressionController,
)


#20260707_kpopmodder: Keep speaking pose tuning in JSON so VTube Studio values can be adjusted without code edits.
DEFAULT_SPEAKING_POSE_CONFIG = {
    "eye_open": 0.5,
    "face_angle_x": -3.0,
    "face_angle_y": -6.0,
    "face_angle_z": -4.0,
    "eye_ball_x": 0.35,
    "eye_ball_y": -0.35,
    "mouth_smile": 0.0,
    "reset_eye_open": 0.5,
    "reset_face_angle_x": 0.0,
    "reset_face_angle_y": 0.0,
    "reset_face_angle_z": 0.0,
    "reset_eye_ball_x": 0.0,
    "reset_eye_ball_y": 0.0,
    "reset_mouth_smile": 1.0,
    "mouth_threshold": 0.04,
    "release_hold_sec": 0.15,
    "refresh_interval_sec": 0.05,
}

SPEAKING_POSE_CONFIG_RANGES = {
    "eye_open": (0.0, 1.0),
    "face_angle_x": (-30.0, 30.0),
    "face_angle_y": (-30.0, 30.0),
    "face_angle_z": (-30.0, 30.0),
    "eye_ball_x": (-1.0, 1.0),
    "eye_ball_y": (-1.0, 1.0),
    "mouth_smile": (0.0, 1.0),
    "reset_eye_open": (0.0, 1.0),
    "reset_face_angle_x": (-30.0, 30.0),
    "reset_face_angle_y": (-30.0, 30.0),
    "reset_face_angle_z": (-30.0, 30.0),
    "reset_eye_ball_x": (-1.0, 1.0),
    "reset_eye_ball_y": (-1.0, 1.0),
    "reset_mouth_smile": (0.0, 1.0),
    "mouth_threshold": (0.0, 1.0),
    "release_hold_sec": (0.0, 5.0),
    "refresh_interval_sec": (0.01, 1.0),
}


DEFAULT_IDLE_POSE_CONFIG = {
    "eye_open": 0.52,
    "face_angle_x": 0.0,
    "face_angle_y": 0.0,
    "face_angle_z": 0.0,
    "eye_ball_x": 0.0,
    "eye_ball_y": 0.0,
    "mouth_smile": 1.0,
    "reset_eye_open": 0.52,
    "reset_face_angle_x": 0.0,
    "reset_face_angle_y": 0.0,
    "reset_face_angle_z": 0.0,
    "reset_eye_ball_x": 0.0,
    "reset_eye_ball_y": 0.0,
    "reset_mouth_smile": 1.0,
    "refresh_interval_sec": 0.05,
}

IDLE_POSE_CONFIG_RANGES = {
    "eye_open": (0.0, 1.0),
    "face_angle_x": (-30.0, 30.0),
    "face_angle_y": (-30.0, 30.0),
    "face_angle_z": (-30.0, 30.0),
    "eye_ball_x": (-1.0, 1.0),
    "eye_ball_y": (-1.0, 1.0),
    "mouth_smile": (0.0, 1.0),
    "reset_eye_open": (0.0, 1.0),
    "reset_face_angle_x": (-30.0, 30.0),
    "reset_face_angle_y": (-30.0, 30.0),
    "reset_face_angle_z": (-30.0, 30.0),
    "reset_eye_ball_x": (-1.0, 1.0),
    "reset_eye_ball_y": (-1.0, 1.0),
    "reset_mouth_smile": (0.0, 1.0),
    "refresh_interval_sec": (0.01, 1.0),
}


def _load_speaking_pose_config(config_path):
    config = dict(DEFAULT_SPEAKING_POSE_CONFIG)

    try:
        if not os.path.exists(config_path):
            return config

        with open(config_path, "r", encoding="utf-8") as file:
            payload = json.load(file)

        if not isinstance(payload, dict):
            log_print("[VtubeStudio] speaking pose config must be a JSON object.")
            return config

        for key, default in DEFAULT_SPEAKING_POSE_CONFIG.items():
            config[key] = _config_float(
                payload.get(key, default),
                default,
                SPEAKING_POSE_CONFIG_RANGES[key],
            )

    except Exception as e:
        log_print(f"[VtubeStudio] speaking pose config load failed: {e}")

    return config


def _load_idle_pose_config(config_path):
    config = dict(DEFAULT_IDLE_POSE_CONFIG)

    try:
        if not os.path.exists(config_path):
            return config

        with open(config_path, "r", encoding="utf-8") as file:
            payload = json.load(file)

        if not isinstance(payload, dict):
            log_print("[VtubeStudio] idle pose config must be a JSON object.")
            return config

        for key, default in DEFAULT_IDLE_POSE_CONFIG.items():
            config[key] = _config_float(
                payload.get(key, default),
                default,
                IDLE_POSE_CONFIG_RANGES[key],
            )

    except Exception as e:
        log_print(f"[VtubeStudio] idle pose config load failed: {e}")

    return config


def _config_float(value, default, value_range):
    try:
        value = float(value)
    except Exception:
        value = default

    minimum, maximum = value_range
    return max(minimum, min(maximum, value))


class VtubeStudio(VtuberPluginInterface):#20260614_kpopmodder
    #20260716_kpopmodder: P1-A static metadata is parsed by PluginLoader without importing this module.
    PLUGIN_METADATA = {
        "id": "VtubeStudio",
        "display_name": "VTube Studio",
        "api_version": "1",
        "category": "vtuber",
        "entrypoint": "plugins.VtubeStudio.VtubeStudio:VtubeStudio",
        "dependency_group": "Full",
        "capabilities": ["vtube_studio_websocket", "mouth_sync", "expression_control"],
        "config_schema": {
            "service": {
                "websocket_url": "ws://localhost:8001",
                "token_path": "plugins/VtubeStudio/token.txt",
            },
        },
        "required_python_packages": ["websocket"],
        "required_files": [],
        "required_executables": [],
        "required_services": ["VTube Studio websocket ws://localhost:8001"],
        "supports_offline": False,
        "supports_cpu": True,
    }

    def init(self):
        self.current_module_directory = os.path.dirname(__file__)
        self.token_path = os.path.join(self.current_module_directory, "token.txt")
        self.speaking_pose_config_path = os.path.join(
            self.current_module_directory,
            "config",
            "speaking_pose.json",
        )
        self.idle_pose_config_path = os.path.join(
            self.current_module_directory,
            "config",
            "idle_pose.json",
        )
        speaking_pose_config = _load_speaking_pose_config(
            self.speaking_pose_config_path,
        )
        idle_pose_config = _load_idle_pose_config(
            self.idle_pose_config_path,
        )

        self.auth_manager = VTubeStudioAuthManager(
            token_path=self.token_path,
            send_callback=self.safe_send,
            authenticated_callback=self._start_avatar_threads
        )
        self.connection = VTubeStudioConnection(
            websocket_url="ws://localhost:8001",
            on_open=self.on_open,
            on_message=self.on_message,
            on_error=self.on_error,
            on_close=self.on_close,
            reset_authentication_callback=self._reset_authentication
        )
        self.mouth_controller = VTubeStudioMouthController(
            send_callback=self.safe_send,
            connected_callback=lambda: self.connected,
            authenticated_callback=lambda: self.isAuthenticated,
            avatar_data_callback=lambda: self.avatar_data
        )
        self.song_expression_controller = VTubeStudioSongExpressionController(#20260628_kpopmodder
            send_callback=self.safe_send,
            connected_callback=lambda: self.connected,
            authenticated_callback=lambda: self.isAuthenticated
        )
        self.blink_controller = VTubeStudioBlinkController(#20260628_kpopmodder
            send_callback=self.safe_send,
            connected_callback=lambda: self.connected,
            authenticated_callback=lambda: self.isAuthenticated,
            override_callback=lambda: self.song_expression_controller.is_active()
        )
        self.speaking_pose_controller = VTubeStudioSpeakingPoseController(#20260628_kpopmodder
            send_callback=self.safe_send,
            connected_callback=lambda: self.connected,
            authenticated_callback=lambda: self.isAuthenticated,
            avatar_data_callback=lambda: self.avatar_data,
            eye_open=speaking_pose_config["eye_open"],
            face_angle_x=speaking_pose_config["face_angle_x"],
            face_angle_y=speaking_pose_config["face_angle_y"],
            face_angle_z=speaking_pose_config["face_angle_z"],
            eye_ball_x=speaking_pose_config["eye_ball_x"],
            eye_ball_y=speaking_pose_config["eye_ball_y"],
            mouth_smile=speaking_pose_config["mouth_smile"],
            reset_eye_open=speaking_pose_config["reset_eye_open"],
            reset_face_angle_x=speaking_pose_config["reset_face_angle_x"],
            reset_face_angle_y=speaking_pose_config["reset_face_angle_y"],
            reset_face_angle_z=speaking_pose_config["reset_face_angle_z"],
            reset_eye_ball_x=speaking_pose_config["reset_eye_ball_x"],
            reset_eye_ball_y=speaking_pose_config["reset_eye_ball_y"],
            reset_mouth_smile=speaking_pose_config["reset_mouth_smile"],
            mouth_threshold=speaking_pose_config["mouth_threshold"],
            release_hold_sec=speaking_pose_config["release_hold_sec"],
            refresh_interval_sec=speaking_pose_config["refresh_interval_sec"],
            speaking_callback=lambda: global_state.get_value(
                GlobalKeys.IS_AI_SPEAKING,
                False,
            ),
            override_callback=lambda: (
                self.song_expression_controller.is_active()
            ),
            eye_open_override_callback=lambda: self.blink_controller.is_blinking()
        )
        self.idle_pose_controller = VTubeStudioIdlePoseController(#20260628_kpopmodder
            send_callback=self.safe_send,
            connected_callback=lambda: self.connected,
            authenticated_callback=lambda: self.isAuthenticated,
            eye_open=idle_pose_config["eye_open"],
            face_angle_x=idle_pose_config["face_angle_x"],
            face_angle_y=idle_pose_config["face_angle_y"],
            face_angle_z=idle_pose_config["face_angle_z"],
            eye_ball_x=idle_pose_config["eye_ball_x"],
            eye_ball_y=idle_pose_config["eye_ball_y"],
            mouth_smile=idle_pose_config["mouth_smile"],
            reset_eye_open=idle_pose_config["reset_eye_open"],
            reset_face_angle_x=idle_pose_config["reset_face_angle_x"],
            reset_face_angle_y=idle_pose_config["reset_face_angle_y"],
            reset_face_angle_z=idle_pose_config["reset_face_angle_z"],
            reset_eye_ball_x=idle_pose_config["reset_eye_ball_x"],
            reset_eye_ball_y=idle_pose_config["reset_eye_ball_y"],
            reset_mouth_smile=idle_pose_config["reset_mouth_smile"],
            refresh_interval_sec=idle_pose_config["refresh_interval_sec"],
            idle_callback=lambda: not global_state.get_value(
                GlobalKeys.IS_AI_SPEAKING,
                False,
            ),
            override_callback=lambda: (
                self.song_expression_controller.is_active()
                or self.speaking_pose_controller.is_active()
            ),
            eye_open_override_callback=lambda: self.blink_controller.is_blinking()
        )
        self.smile_controller = VTubeStudioSmileController(#20260628_kpopmodder
            send_callback=self.safe_send,
            connected_callback=lambda: self.connected,
            authenticated_callback=lambda: self.isAuthenticated,
            override_callback=lambda: (
                self.song_expression_controller.is_active()
                or self.speaking_pose_controller.is_active()
                or self.idle_pose_controller.is_active()
            )
        )

        event_manager.subscribe(EventType.INTERRUPT, self.handle_interrupt)

        self.authenticate()

    @property
    def isAuthenticated(self):
        return self.auth_manager.is_authenticated

    @isAuthenticated.setter
    def isAuthenticated(self, value):
        self.auth_manager.is_authenticated = bool(value)

    @property
    def token(self):
        return self.auth_manager.token

    @token.setter
    def token(self, value):
        self.auth_manager.token = value

    @property
    def ws(self):
        return self.connection.ws

    @ws.setter
    def ws(self, value):
        self.connection.ws = value

    @property
    def ws_lock(self):
        return self.connection.ws_lock

    @ws_lock.setter
    def ws_lock(self, value):
        self.connection.ws_lock = value

    @property
    def mouth_thread_started(self):
        return self.mouth_controller.mouth_thread_started

    @mouth_thread_started.setter
    def mouth_thread_started(self, value):
        self.mouth_controller.mouth_thread_started = bool(value)

    @property
    def websocket_thread_started(self):
        return self.connection.websocket_thread_started

    @websocket_thread_started.setter
    def websocket_thread_started(self, value):
        self.connection.websocket_thread_started = bool(value)

    @property
    def should_reconnect(self):
        return self.connection.should_reconnect

    @should_reconnect.setter
    def should_reconnect(self, value):
        self.connection.should_reconnect = bool(value)

    @property
    def connected(self):
        return self.connection.connected

    @connected.setter
    def connected(self, value):
        self.connection.connected = bool(value)

    def create_ui(self):
        with gr.Accordion(label="Vtube Studio Options", open=False):
            self.authenticate_button = gr.Button("Authenticate")
            self.authenticate_button.click(self.on_authenticate_click)

    def on_authenticate_click(self):
        self.authenticate()

    def authenticate(self):#20260614_kpopmodder
        self.connection.start(self._show_authentication_info)

    def _show_authentication_info(self):
        if not os.path.exists(self.token_path):
            gr.Info("Aquiring token, please continue in Vtube Studio...")
        else:
            gr.Info("Token Found, attempting to authenticate with token...")

    # def authenticate(self):#20260614_kpopmodder
    #     if not os.path.exists(self.token_path):
    #         gr.Info("Aquiring token, please continue in Vtube Studio...")
    #     else:
    #         gr.Info("Token Found, attempting to authenticate with token...")

    #     thread = threading.Thread(target=self.websocket_thread, daemon=True)
    #     thread.start()

    def websocket_thread(self):#20260614_kpopmodder
        self.connection.websocket_thread()

    # def websocket_thread(self):#20260614_kpopmodder
    #     self.ws = websocket.WebSocketApp(
    #         "ws://localhost:8001",
    #         on_open=self.on_open,
    #         on_message=self.on_message,
    #         on_error=self.on_error,
    #         on_close=self.on_close,
    #     )
    #     self.ws.run_forever()

    def safe_send(self, message):#20260614_kpopmodder
        return self.connection.safe_send(message)

    # def safe_send(self, message):#20260614_kpopmodder
    #     try:
    #         if self.ws is None:
    #             return
    #         with self.ws_lock:
    #             self.ws.send(json.dumps(message))
    #     except Exception as e:
    #         log_print(f"[VtubeStudio] send error: {e}")

    def getToken(self):
        self.auth_manager.get_token()

    def on_open(self, ws):
        self.connection.mark_open()#20260614_kpopmodder
        self.auth_manager.on_open()

    def on_message(self, ws, message):
        self.auth_manager.on_message(message)

    def send_authentication_request(self):
        self.auth_manager.send_authentication_request()

    def on_error(self, ws, error):
        log_print(f"[VtubeStudio] Error: {error}")

    def on_close(self, ws, close_status_code, close_msg):#20260614_kpopmodder
        self.connection.mark_closed()
        log_print("### Connection closed ###")
        log_print("[VtubeStudio] Connection closed. Will retry automatically.")

    # def on_close(self, ws, close_status_code, close_msg):
    #     log_print("### Connection closed ###")
    #     log_print("Failed to connect to VTube Studio. Start VTube Studio and enable plugins.")

    def set_mouth_open(self, value):
        self.mouth_controller.set_mouth_open(value)

    def close_mouth_force(self):
        self.mouth_controller.close_mouth_force()

    def set_song_expression(self, expression):#20260628_kpopmodder
        self.song_expression_controller.apply_song_expression(expression)

    def handle_interrupt(self):
        log_print("[VtubeStudio] INTERRUPT received. Closing mouth.")
        self.close_mouth_force()

    def mouth_data_thread(self):#20260614_kpopmodder
        self.mouth_controller.mouth_data_thread()

    def _reset_authentication(self):
        self.auth_manager.reset_authentication()

    def _start_mouth_thread(self):
        self.mouth_controller.start_if_needed()

    def _start_avatar_threads(self):#20260628_kpopmodder
        self.mouth_controller.start_if_needed()
        self.speaking_pose_controller.start_if_needed()
        self.blink_controller.start_if_needed()
        self.idle_pose_controller.start_if_needed()
        self.smile_controller.start_if_needed()

    def shutdown(self):
        try:#20260628_kpopmodder
            self.song_expression_controller.reset()
        except Exception as e:
            log_print(f"[VtubeStudio] song expression reset error ignored: {e}")

        try:#20260628_kpopmodder
            self.blink_controller.stop()
        except Exception as e:
            log_print(f"[VtubeStudio] blink shutdown error ignored: {e}")

        try:#20260628_kpopmodder
            self.speaking_pose_controller.stop()
        except Exception as e:
            log_print(f"[VtubeStudio] speaking pose shutdown error ignored: {e}")

        try:#20260628_kpopmodder
            self.idle_pose_controller.stop()
        except Exception as e:
            log_print(f"[VtubeStudio] idle pose shutdown error ignored: {e}")

        try:#20260628_kpopmodder
            self.smile_controller.stop()
        except Exception as e:
            log_print(f"[VtubeStudio] smile shutdown error ignored: {e}")

    # def mouth_data_thread(self):
    #     while True:
    #         try:
    #             mouth_open = getattr(self.avatar_data, "mouth_open", 0)
    #             self.set_mouth_open(mouth_open)
    #         except Exception as e:
    #             log_print(f"[VtubeStudio] mouth_data_thread error: {e}")

    #         time.sleep(0.1)


# import json#20260614_kpopmodder
# import subprocess
# import threading
# import time
# import zipfile
# import gradio as gr
# import requests
# from tqdm import tqdm
# import websocket
# from plugin_system.interfaces import VtuberPluginInterface
# import os
# from core.logger import log_print, debug_print#20260612_kpopmodder


# class VtubeStudio(VtuberPluginInterface):
#     isAuthenticated = False
#     token = ""
#     current_module_directory = os.path.dirname(__file__)
#     token_path = os.path.join(
#         current_module_directory, "token.txt")
#     current_volume = 0

#     def init(self):
#         self.authenticate()

#     def create_ui(self):
#         with gr.Accordion(label="Vtube Studio Options", open=False):
#             with gr.Row():
#                 self.authenticate_button = gr.Button("Authenticate")
#         self.authenticate_button.click(self.on_authenticate_click)

#     def on_authenticate_click(self):
#         self.authenticate()

#     def authenticate(self):
#         if not os.path.exists(self.token_path):
#             gr.Info("Aquiring token, please continue in Vtube Studio...")
#         else:
#             gr.Info("Token Found, attempting to authenticate with token...")
#         thread = threading.Thread(target=self.websocket_thread)
#         thread.start()

#     def getToken(self):
#         token_request = {
#             "apiName": "VTubeStudioPublicAPI",
#             "apiVersion": "1.0",
#             "requestID": "123",
#             "messageType": "AuthenticationTokenRequest",
#             "data": {
#                 "pluginName": "LocalAIVtuberPlugin",
#                 "pluginDeveloper": "Xiaohei"
#             }
#         }
#         self.ws.send(json.dumps(token_request))

#     def on_open(self, ws):
#         # Check if the file exists. If not, create an empty file.
#         if not os.path.exists(self.token_path):
#             with open(self.token_path, 'w') as file:
#                 file.write('')
#             self.getToken()

#         else:
#             with open(self.token_path, 'r', encoding='utf-8') as file:
#                 content = file.read()
#                 self.token = content
#             self.send_authentication_request()

#     def on_message(self, ws, message):
#         response = json.loads(message)
#         if response['messageType'] == "InjectParameterDataResponse":
#             return
#         log_print("Received message:", message)#20260612_kpopmodder
#         if response['messageType'] == "AuthenticationTokenResponse":
#             self.token = response['data']['authenticationToken']
#             log_print("Authentication token received:", self.token)#20260612_kpopmodder
#             with open(self.token_path, 'w') as file:
#                 file.write(self.token)
#             self.send_authentication_request()
#             return
#         if response['messageType'] == "AuthenticationResponse":
#             self.token = response['data']['authenticated'] == True
#             log_print(response['data']['reason'])#20260612_kpopmodder
#             threading.Thread(target=self.mouth_data_thread).start()
#             return

#     def send_authentication_request(self):
#         auth_request = {
#             "apiName": "VTubeStudioPublicAPI",
#             "apiVersion": "1.0",
#             "requestID": "234",
#             "messageType": "AuthenticationRequest",
#             "data": {
#                 "pluginName": "LocalAIVtuberPlugin",
#                 "pluginDeveloper": "Xiaohei",
#                 "authenticationToken": self.token
#             }
#         }
#         self.ws.send(json.dumps(auth_request))

#     def on_error(self, ws, error):
#         log_print("Error:", error)#20260612_kpopmodder

#     def on_close(self, ws, close_status_code, close_msg):
#         log_print("### Connection closed ###")#20260612_kpopmodder
#         log_print("Failed to connect to vtube studio, if you want vtube studio functionalities, please start vtube studio and enable plugins.")#20260612_kpopmodder

#     def websocket_thread(self):
#         self.ws = websocket.WebSocketApp("ws://localhost:8001",
#                                          on_open=self.on_open,
#                                          on_message=self.on_message,
#                                          on_error=self.on_error,
#                                          on_close=self.on_close)
#         self.ws.run_forever()

#     def mouth_data_thread(self):
#         while True:
#             #log_print(f"Setting MouthOpen to {self.avatar_data.mouth_open}")#20260612_kpopmodder
#             message = {
#                 "apiName": "VTubeStudioPublicAPI",
#                 "apiVersion": "1.0",
#                 "requestID": "2",
#                 "messageType": "InjectParameterDataRequest",
#                 "data": {
#                     "mode": "set",
#                     "parameterValues": [
#                         {
#                             "id": "MouthOpen",
#                             "value": self.avatar_data.mouth_open
#                         },
#                     ]
#                 }
#             }
#             self.ws.send(json.dumps(message))
#             time.sleep(0.1)
