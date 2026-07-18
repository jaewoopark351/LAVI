#20260718_kpopmodder: Added this module to share plugin availability probes across loaders.
import importlib
import importlib.util
import os
from pathlib import Path
import re
import shutil
import socket
from urllib.parse import urlparse


class AvailabilityProbeService:
    _ENDPOINT_RE = re.compile(r"(?P<url>(?:https?|wss?|ws|tcp)://[^\s]+)")
    _LOOPBACK_HOSTS = {
        "localhost",
        "127.0.0.1",
        "::1",
    }
    _SERVICE_ENDPOINT_PREFIXES = (
        "VTube Studio websocket ",
        "VOICEVOX engine ",
        "Silero API server ",
    )
    _MANAGED_SERVICE_PREFIXES = (
        "GPT-SoVITS API server ",
    )
    _DEFERRED_EXTERNAL_SERVICES = {
        "OpenAI API",
        "Twitch chat API",
        "YouTube live chat",
    }

    def __init__(
        self,
        *,
        environ=None,
        module_importer=importlib.import_module,
        spec_finder=importlib.util.find_spec,
        executable_finder=shutil.which,
        tcp_connector=socket.create_connection,
    ):
        self.environ = environ if environ is not None else os.environ
        self.module_importer = module_importer
        self.spec_finder = spec_finder
        self.executable_finder = executable_finder
        self.tcp_connector = tcp_connector

    def missing_python_packages(self, packages):
        return [
            package
            for package in packages
            if self.spec_finder(package) is None
        ]

    def missing_files(self, required_files, resolve_file):
        missing = []
        for required_path in required_files:
            resolved = Path(resolve_file(required_path))
            if not resolved.exists():
                missing.append(required_path)
        return missing

    def missing_executables(self, executables):
        return [
            executable
            for executable in executables
            if self.executable_finder(executable) is None
        ]

    def missing_services(self, services, timeout_sec=0.25):
        return [
            service
            for service in services
            if not self.service_available(service, timeout_sec=timeout_sec)
        ]

    def service_available(self, service, timeout_sec=0.25):
        service_name = str(service or "").strip()
        if not service_name:
            return False
        if service_name == "microphone_input_device":
            return self._probe_microphone_input_device()
        if service_name in self._DEFERRED_EXTERNAL_SERVICES:
            return True
        if self._is_managed_service(service_name):
            return True

        endpoint = self._service_endpoint(service_name)
        if endpoint:
            return self._probe_local_tcp_endpoint(endpoint, timeout_sec=timeout_sec)

        return False

    def _service_endpoint(self, service_name):
        for prefix in self._SERVICE_ENDPOINT_PREFIXES:
            if service_name.startswith(prefix):
                return service_name.removeprefix(prefix).strip()

        lower_name = service_name.lower()
        if lower_name.startswith("tcp "):
            target = service_name[4:].strip()
            if "://" not in target:
                return f"tcp://{target}"
            return target

        match = self._ENDPOINT_RE.search(service_name)
        if match is not None:
            return match.group("url").strip()
        return ""

    def _is_managed_service(self, service_name):
        return any(
            service_name.startswith(prefix)
            for prefix in self._MANAGED_SERVICE_PREFIXES
        )

    def _probe_microphone_input_device(self):
        try:
            sounddevice = self.module_importer("sounddevice")
            devices = sounddevice.query_devices()
        except Exception:
            return False
        try:
            return any(int(device.get("max_input_channels", 0)) > 0 for device in devices)
        except Exception:
            return False

    def _probe_local_tcp_endpoint(self, service_url, timeout_sec=0.25):
        parsed = urlparse(service_url)
        host = parsed.hostname
        port = parsed.port or self._default_port(parsed.scheme)
        if not host or not port:
            return False
        if not self._is_loopback_host(host):
            return False
        try:
            with self.tcp_connector((host, port), timeout=float(timeout_sec)):
                return True
        except OSError:
            return False

    def _default_port(self, scheme):
        normalized = str(scheme or "").lower()
        if normalized in {"http", "ws", "tcp"}:
            return 80
        if normalized in {"https", "wss"}:
            return 443
        return None

    def _is_loopback_host(self, host):
        normalized = str(host or "").strip().lower()
        return normalized in self._LOOPBACK_HOSTS
