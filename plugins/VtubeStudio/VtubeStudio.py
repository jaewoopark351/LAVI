import os

from plugin_system.interfaces import VtuberPluginInterface
#20260620_kpopmodder: Import grouped VTube Studio helpers from vtube_studio_core.
#20260904_kpopmodder: Keep this module as the static plugin-discovery facade after responsibility extraction.
from plugins.VtubeStudio.vtube_studio_core.configuration import (
    DEFAULT_IDLE_POSE_CONFIG,
    DEFAULT_SPEAKING_POSE_CONFIG,
    IDLE_POSE_CONFIG_RANGES,
    SPEAKING_POSE_CONFIG_RANGES,
    load_idle_pose_config,
    load_speaking_pose_config,
)
from plugins.VtubeStudio.vtube_studio_core.configuration.vtube_studio_pose_config_validator import (
    clamp_config_float,
)
from plugins.VtubeStudio.vtube_studio_core.runtime import (
    VTubeStudioComponentFactory,
)
from plugins.VtubeStudio.vtube_studio_core.ui import VTubeStudioUI


_load_speaking_pose_config = load_speaking_pose_config
_load_idle_pose_config = load_idle_pose_config
_config_float = clamp_config_float


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
        #20260904_kpopmodder: Port availability is a reconnectable runtime state, not a startup dependency.
        "required_services": [],
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
        #20260904_kpopmodder: Delegate construction and lifecycle ownership to focused runtime components.
        self.runtime, self.token_store = VTubeStudioComponentFactory().create(
            module_directory=self.current_module_directory,
            avatar_data_callback=lambda: self.avatar_data,
            callback_owner=self,
        )
        self.auth_manager = self.runtime.auth_manager
        self.connection = self.runtime.connection
        self.mouth_controller = self.runtime.mouth_controller
        self.song_expression_controller = self.runtime.song_expression_controller
        self.blink_controller = self.runtime.blink_controller
        self.speaking_pose_controller = self.runtime.speaking_pose_controller
        self.idle_pose_controller = self.runtime.idle_pose_controller
        self.smile_controller = self.runtime.smile_controller
        self.ui = VTubeStudioUI(self.token_store)
        self.runtime.initialize()
        self.authenticate()

    @property
    def isAuthenticated(self):
        return (
            self.auth_manager.is_authenticated
            and self.connection.is_attempt_authenticated()
        )

    @isAuthenticated.setter
    def isAuthenticated(self, value):
        requested = bool(value)
        if requested:
            attempt_id = self.connection.current_attempt_id
            requested = self.connection.mark_authenticated(attempt_id)
        else:
            self.connection.clear_authenticated()
        self.auth_manager.is_authenticated = requested

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
        if value is None and hasattr(self, "auth_manager"):
            self.auth_manager.reset_authentication()

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
        if not value:
            self.auth_manager.reset_authentication()

    def create_ui(self):
        self.authenticate_button = self.ui.create(self.on_authenticate_click)
        return self.authenticate_button

    def on_authenticate_click(self):
        self.authenticate()

    def authenticate(self):#20260614_kpopmodder
        started = self.runtime.start_connection()
        if started:
            self._show_authentication_info()
        return started

    def _show_authentication_info(self):
        self.ui.show_authentication_info()

    def websocket_thread(self):#20260614_kpopmodder
        return self.connection.start()

    def safe_send(self, message):#20260614_kpopmodder
        return self.connection.safe_send(message)

    def getToken(self):
        self.auth_manager.get_token()

    def on_open(self, ws):
        attempt_id = self.connection.current_attempt_id
        if not self.connection.mark_open(ws, attempt_id):#20260614_kpopmodder
            return False
        return self.runtime.on_open(ws, attempt_id)

    def on_message(self, ws, message):
        attempt_id = self.connection.current_attempt_id
        if not self.connection.is_current_socket(ws, attempt_id):
            return False
        return self.runtime.on_message(ws, message, attempt_id)

    def send_authentication_request(self):
        self.auth_manager.send_authentication_request()

    def on_error(self, ws, error):
        attempt_id = self.connection.current_attempt_id
        if not self.connection.is_current_socket(ws, attempt_id):
            return False
        return self.runtime.on_error(ws, error, attempt_id)

    def on_close(self, ws, close_status_code, close_msg):#20260614_kpopmodder
        attempt_id = self.connection.current_attempt_id
        if not self.connection.mark_closed(ws, attempt_id):
            return False
        return self.runtime.on_close(
            ws,
            close_status_code,
            close_msg,
            attempt_id,
        )

    def set_mouth_open(self, value):
        self.mouth_controller.set_mouth_open(value)

    def close_mouth_force(self):
        self.mouth_controller.close_mouth_force()

    def set_song_expression(self, expression):#20260628_kpopmodder
        self.song_expression_controller.apply_song_expression(expression)

    def handle_interrupt(self):
        self.runtime.handle_interrupt()

    def mouth_data_thread(self):#20260614_kpopmodder
        self.mouth_controller.mouth_data_thread()

    def _reset_authentication(self):
        self.auth_manager.reset_authentication()

    def _start_mouth_thread(self):
        self.mouth_controller.start_if_needed()

    def _start_avatar_threads(self):#20260628_kpopmodder
        return self.runtime.start_avatar_threads()

    def shutdown(self):
        runtime = getattr(self, "runtime", None)
        if runtime is None:
            return False
        return runtime.shutdown()
