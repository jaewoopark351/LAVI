import socket

from core.logger import log_print


#20260630_kpopmodder: Keep Gradio port probing separate from main.py startup wiring.
DEFAULT_GRADIO_HOST = "127.0.0.1"
DEFAULT_GRADIO_START_PORT = 47860
DEFAULT_GRADIO_PORT_MAX_ATTEMPTS = 100
DEFAULT_GRADIO_OPEN_BROWSER = True
DEFAULT_GRADIO_SHARE = False


def find_available_port(
    host=DEFAULT_GRADIO_HOST,
    start_port=DEFAULT_GRADIO_START_PORT,
    max_attempts=DEFAULT_GRADIO_PORT_MAX_ATTEMPTS,
):
    start_port = int(start_port)
    max_attempts = int(max_attempts)
    if start_port < 1 or start_port > 65535:
        raise ValueError(f"[Gradio] Invalid start port: {start_port}")
    if max_attempts < 1:
        raise ValueError(f"[Gradio] Invalid max_attempts: {max_attempts}")

    end_port = min(65535, start_port + max_attempts - 1)
    for port in range(start_port, end_port + 1):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as test_socket:
            try:
                test_socket.bind((host, port))
                return port
            except OSError:
                log_print(f"[Gradio] Port {port} is already in use.")

    raise RuntimeError(
        f"[Gradio] No available port found from "
        f"{start_port} to {end_port}."
    )


def load_gradio_launch_options(config):
    config = dict(config or {})
    host = _config_text(
        config,
        ("server_name", "host"),
        DEFAULT_GRADIO_HOST,
    )
    start_port = _config_int(
        config,
        ("server_port", "port"),
        DEFAULT_GRADIO_START_PORT,
        min_value=1,
        max_value=65535,
    )
    max_attempts = _config_int(
        config,
        ("port_max_attempts", "max_attempts"),
        DEFAULT_GRADIO_PORT_MAX_ATTEMPTS,
        min_value=1,
    )
    auto_increment = _config_bool(
        config,
        ("auto_increment_port", "port_auto_increment"),
        True,
    )
    if not auto_increment:
        max_attempts = 1

    return {
        "host": host,
        "start_port": start_port,
        "max_attempts": max_attempts,
        "open_browser": _config_bool(
            config,
            ("open_browser_on_start", "inbrowser"),
            DEFAULT_GRADIO_OPEN_BROWSER,
        ),
        "share": _config_bool(
            config,
            ("share",),
            DEFAULT_GRADIO_SHARE,
        ),
    }


def _config_text(config, keys, default):
    for key in keys:
        value = config.get(key)
        if value is None:
            continue
        value = str(value).strip()
        if value:
            return value
    return default


def _config_int(config, keys, default, min_value=None, max_value=None):
    for key in keys:
        value = config.get(key)
        if value is None or str(value).strip() == "":
            continue
        try:
            number = int(value)
        except (TypeError, ValueError):
            return default
        if min_value is not None and number < min_value:
            return default
        if max_value is not None and number > max_value:
            return default
        return number
    return default


def _config_bool(config, keys, default):
    for key in keys:
        value = config.get(key)
        if value is None or str(value).strip() == "":
            continue
        normalized = str(value).strip().lower()
        if normalized in {"1", "true", "yes", "on"}:
            return True
        if normalized in {"0", "false", "no", "off"}:
            return False
        return default
    return default
