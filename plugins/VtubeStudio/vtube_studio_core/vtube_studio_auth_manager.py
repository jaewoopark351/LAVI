#20260620_kpopmodder: VTube Studio helper modules are grouped under vtube_studio_core without changing behavior.
import json
import os

from core.logger import log_print


class VTubeStudioAuthManager:#20260620_kpopmodder
    def __init__(self, token_path, send_callback, authenticated_callback):
        self.token_path = token_path
        self.send_callback = send_callback
        self.authenticated_callback = authenticated_callback

        self.is_authenticated = False
        self.token = ""

    def reset_authentication(self):
        self.is_authenticated = False

    def get_token(self):
        self.send_callback({
            "apiName": "VTubeStudioPublicAPI",
            "apiVersion": "1.0",
            "requestID": "token_request",
            "messageType": "AuthenticationTokenRequest",
            "data": {
                "pluginName": "LocalAIVtuberPlugin",
                "pluginDeveloper": "Xiaohei"
            }
        })

    def on_open(self):
        if not os.path.exists(self.token_path):
            with open(self.token_path, "w", encoding="utf-8") as file:
                file.write("")
            self.get_token()
        else:
            with open(self.token_path, "r", encoding="utf-8") as file:
                self.token = file.read().strip()
            self.send_authentication_request()

    def on_message(self, message):
        response = json.loads(message)

        if response.get("messageType") == "InjectParameterDataResponse":
            return

        log_print(f"Received message: {message}")

        if response.get("messageType") == "AuthenticationTokenResponse":
            self.token = response["data"]["authenticationToken"]
            log_print(f"Authentication token received: {self.token}")

            with open(self.token_path, "w", encoding="utf-8") as file:
                file.write(self.token)

            self.send_authentication_request()
            return

        if response.get("messageType") == "AuthenticationResponse":
            self.is_authenticated = response["data"]["authenticated"] is True
            log_print(response["data"]["reason"])

            if self.is_authenticated:
                self.authenticated_callback()
            return

    def send_authentication_request(self):
        self.send_callback({
            "apiName": "VTubeStudioPublicAPI",
            "apiVersion": "1.0",
            "requestID": "auth_request",
            "messageType": "AuthenticationRequest",
            "data": {
                "pluginName": "LocalAIVtuberPlugin",
                "pluginDeveloper": "Xiaohei",
                "authenticationToken": self.token
            }
        })
