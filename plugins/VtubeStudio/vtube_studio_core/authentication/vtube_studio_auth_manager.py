#20260904_kpopmodder: Own attempt-scoped VTube Studio authentication without logging credentials.
import json
import threading

from core.logger import log_print
from plugins.VtubeStudio.vtube_studio_core.diagnostics import (
    VTubeStudioEventLogLimiter,
)
from .vtube_studio_authentication_phase import VTubeStudioAuthenticationPhase
from .vtube_studio_token_store import VTubeStudioTokenStore


class VTubeStudioAuthManager:#20260620_kpopmodder
    def __init__(
        self,
        token_path,
        send_callback,
        authenticated_callback,
        token_store=None,
        attempt_send_callback=None,
        attempt_authenticated_callback=None,
        attempt_failed_callback=None,
        attempt_rejected_callback=None,
        log_limiter=None,
    ):
        self.token_path = token_path
        self.send_callback = send_callback
        self.authenticated_callback = authenticated_callback
        self.token_store = token_store or VTubeStudioTokenStore(token_path)
        self.attempt_send_callback = attempt_send_callback
        self.attempt_authenticated_callback = attempt_authenticated_callback
        self.attempt_failed_callback = attempt_failed_callback
        self.attempt_rejected_callback = attempt_rejected_callback
        self.log_limiter = log_limiter or VTubeStudioEventLogLimiter()

        self._lock = threading.RLock()
        self._stop_event = threading.Event()
        self._is_authenticated = False
        self._token = ""
        self._active_attempt_id = None
        self._phase = VTubeStudioAuthenticationPhase.IDLE
        self._stopped = False

    @property
    def is_authenticated(self):
        with self._lock:
            return self._is_authenticated

    @is_authenticated.setter
    def is_authenticated(self, value):
        with self._lock:
            self._is_authenticated = bool(value)

    @property
    def token(self):
        with self._lock:
            return self._token

    @token.setter
    def token(self, value):
        with self._lock:
            self._token = str(value or "")

    @property
    def active_attempt_id(self):
        with self._lock:
            return self._active_attempt_id

    def begin_attempt(self, attempt_id=None):
        with self._lock:
            if self._stopped or self._stop_event.is_set():
                return False
            self._active_attempt_id = attempt_id
            self._is_authenticated = False
            self._phase = VTubeStudioAuthenticationPhase.IDLE
            return True

    def reset_authentication(self, attempt_id=None):
        with self._lock:
            if (
                attempt_id is not None
                and self._active_attempt_id is not None
                and attempt_id != self._active_attempt_id
            ):
                return False
            self._is_authenticated = False
            if attempt_id is None or attempt_id == self._active_attempt_id:
                self._active_attempt_id = None
                self._phase = VTubeStudioAuthenticationPhase.IDLE
            return True

    def stop(self):
        with self._lock:
            already_stopped = self._stopped
            self._stop_event.set()
            self._stopped = True
            self._is_authenticated = False
            self._token = ""
            self._active_attempt_id = None
            self._phase = VTubeStudioAuthenticationPhase.IDLE
            return not already_stopped

    def get_token(self, attempt_id=None):
        attempt_id = self._resolve_attempt(attempt_id)
        if not self._begin_request(
            attempt_id,
            VTubeStudioAuthenticationPhase.TOKEN_REQUESTED,
            (VTubeStudioAuthenticationPhase.IDLE,),
        ):
            return False
        sent = self._send({
            "apiName": "VTubeStudioPublicAPI",
            "apiVersion": "1.0",
            "requestID": "token_request",
            "messageType": "AuthenticationTokenRequest",
            "data": {
                "pluginName": "LocalAIVtuberPlugin",
                "pluginDeveloper": "Xiaohei",
            },
        }, attempt_id)
        if not sent:
            self._restore_phase(
                attempt_id,
                VTubeStudioAuthenticationPhase.TOKEN_REQUESTED,
                VTubeStudioAuthenticationPhase.IDLE,
            )
        return sent

    def on_open(self, attempt_id=None):
        if not self.begin_attempt(attempt_id):
            return False

        try:
            if self._stop_event.is_set():
                return False
            token = self.token_store.read() if self.token_store.exists() else ""
            with self._lock:
                if not self._accept_attempt_locked(attempt_id):
                    return False
                self._token = token
        except Exception as error:
            self._authentication_storage_failed(attempt_id, "token_read", error)
            return False

        if not token:
            return self.get_token(attempt_id)
        return self.send_authentication_request(attempt_id)

    def on_message(self, message, attempt_id=None):
        attempt_id = self._resolve_attempt(attempt_id)
        if not self._accept_attempt(attempt_id):
            return False

        try:
            response = json.loads(message)
        except (TypeError, ValueError):
            self._log_limited(
                "invalid_json",
                "[VtubeStudio] authentication message rejected reason=invalid_json",
            )
            return False

        if not isinstance(response, dict):
            self._log_limited(
                "invalid_shape",
                "[VtubeStudio] authentication message rejected reason=invalid_shape",
            )
            return False

        message_type = response.get("messageType")
        if message_type == "InjectParameterDataResponse":
            return True

        data = response.get("data")
        if not isinstance(data, dict):
            self._log_limited(
                "missing_data",
                "[VtubeStudio] authentication message rejected "
                f"message_type_kind={_message_type_kind(message_type)} "
                "reason=missing_data",
            )
            return False

        if message_type == "AuthenticationTokenResponse":
            if not self._phase_is(
                attempt_id,
                VTubeStudioAuthenticationPhase.TOKEN_REQUESTED,
            ):
                self._log_limited(
                    "unexpected_token_response",
                    "[VtubeStudio] authentication message rejected "
                    "message_type_kind=authentication_token_response "
                    "reason=unexpected_phase",
                )
                return False
            token = data.get("authenticationToken")
            if not isinstance(token, str) or not token:
                self._log_limited(
                    "missing_token",
                    "[VtubeStudio] authentication token rejected reason=missing_token",
                )
                return False
            staged_path = None
            try:
                staged_path = self.token_store.stage(token)
                with self._lock:
                    if (
                        not self._accept_attempt_locked(attempt_id)
                        or self._phase
                        != VTubeStudioAuthenticationPhase.TOKEN_REQUESTED
                    ):
                        return False
                    # publish_staged is deliberately restricted to one local
                    # atomic replace so stop/publish have one linearization point.
                    self.token_store.publish_staged(staged_path)
                    staged_path = None
                    self._token = token
                    self._phase = VTubeStudioAuthenticationPhase.TOKEN_RECEIVED
            except Exception as error:
                self._authentication_storage_failed(
                    attempt_id,
                    "token_write",
                    error,
                )
                return False
            finally:
                if staged_path is not None:
                    self.token_store.discard(staged_path)
            log_print("[VtubeStudio] authentication token received")
            return self.send_authentication_request(attempt_id)

        if message_type == "AuthenticationResponse":
            authenticated = data.get("authenticated") is True
            reason = "unspecified" if authenticated else "api_rejected"
            with self._lock:
                expected_response = (
                    self._accept_attempt_locked(attempt_id)
                    and self._phase
                    == VTubeStudioAuthenticationPhase.AUTHENTICATION_REQUESTED
                )
                if expected_response:
                    self._is_authenticated = authenticated
                    self._phase = (
                        VTubeStudioAuthenticationPhase.AUTHENTICATED
                        if authenticated
                        else VTubeStudioAuthenticationPhase.REJECTED
                    )
            if not expected_response:
                self._log_limited(
                    "unexpected_authentication_response",
                    "[VtubeStudio] authentication message rejected "
                    "message_type_kind=authentication_response "
                    "reason=unexpected_phase",
                )
                return False

            if authenticated:
                log_print("[VtubeStudio] authentication succeeded")
                self._notify_authenticated(attempt_id)
            else:
                self._log_limited(
                    "authentication_rejected",
                    f"[VtubeStudio] authentication rejected reason={reason}",
                )
                if self.attempt_rejected_callback is not None:
                    self.attempt_rejected_callback(attempt_id, reason)
            return True

        self._log_limited(
            "ignored_message",
            "[VtubeStudio] authentication message ignored "
            f"message_type_kind={_message_type_kind(message_type)}",
            level="debug",
        )
        return False

    def send_authentication_request(self, attempt_id=None):
        attempt_id = self._resolve_attempt(attempt_id)
        with self._lock:
            token = self._token
        if not self._begin_request(
            attempt_id,
            VTubeStudioAuthenticationPhase.AUTHENTICATION_REQUESTED,
            (
                VTubeStudioAuthenticationPhase.IDLE,
                VTubeStudioAuthenticationPhase.TOKEN_RECEIVED,
            ),
        ):
            return False
        sent = self._send({
            "apiName": "VTubeStudioPublicAPI",
            "apiVersion": "1.0",
            "requestID": "auth_request",
            "messageType": "AuthenticationRequest",
            "data": {
                "pluginName": "LocalAIVtuberPlugin",
                "pluginDeveloper": "Xiaohei",
                "authenticationToken": token,
            },
        }, attempt_id)
        if not sent:
            self._restore_phase(
                attempt_id,
                VTubeStudioAuthenticationPhase.AUTHENTICATION_REQUESTED,
                VTubeStudioAuthenticationPhase.IDLE,
            )
        return sent

    def _send(self, message, attempt_id):
        if not self._accept_attempt(attempt_id):
            return False
        if self.attempt_send_callback is not None:
            return bool(self.attempt_send_callback(message, attempt_id))
        result = self.send_callback(message)
        return True if result is None else bool(result)

    def _notify_authenticated(self, attempt_id):
        if not self._accept_attempt(attempt_id):
            return
        if self.attempt_authenticated_callback is not None:
            self.attempt_authenticated_callback(attempt_id)
            return
        self.authenticated_callback()

    def _authentication_storage_failed(self, attempt_id, operation, error):
        error_type = type(error).__name__
        self._log_limited(
            f"authentication_storage_failed_{operation}",
            "[VtubeStudio] authentication storage failed "
            f"operation={operation} error_type={error_type}",
            level="warning",
        )
        callback = self.attempt_failed_callback
        if callback is not None and self._accept_attempt(attempt_id):
            callback(attempt_id, f"{operation}_failed")

    def _resolve_attempt(self, attempt_id):
        if attempt_id is not None:
            return attempt_id
        with self._lock:
            return self._active_attempt_id

    def _accept_attempt(self, attempt_id):
        with self._lock:
            return self._accept_attempt_locked(attempt_id)

    def _accept_attempt_locked(self, attempt_id):
        if self._stopped or self._stop_event.is_set():
            return False
        if self._active_attempt_id is None:
            return attempt_id is None
        return attempt_id == self._active_attempt_id

    def _begin_request(self, attempt_id, next_phase, allowed_phases):
        with self._lock:
            if not self._accept_attempt_locked(attempt_id):
                return False
            if self._phase not in allowed_phases:
                return False
            self._phase = next_phase
            return True

    def _restore_phase(self, attempt_id, expected_phase, restored_phase):
        with self._lock:
            if (
                self._accept_attempt_locked(attempt_id)
                and self._phase == expected_phase
            ):
                self._phase = restored_phase

    def _phase_is(self, attempt_id, expected_phase):
        with self._lock:
            return (
                self._accept_attempt_locked(attempt_id)
                and self._phase == expected_phase
            )

    def _log_limited(self, event, message, level="warning"):
        if self.log_limiter.should_log(event):
            log_print(message, level=level)


def _message_type_kind(value):
    known_types = {
        "AuthenticationTokenResponse": "authentication_token_response",
        "AuthenticationResponse": "authentication_response",
        "InjectParameterDataResponse": "inject_parameter_data_response",
    }
    if not isinstance(value, str):
        return "invalid"
    return known_types.get(value, "unknown")
