import socket

from core.logger import log_print


#20260630_kpopmodder: Keep Gradio port probing separate from main.py startup wiring.
def find_available_port(host="127.0.0.1", start_port=7860, max_attempts=100):
    for port in range(start_port, start_port + max_attempts):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as test_socket:
            try:
                test_socket.bind((host, port))
                return port
            except OSError:
                log_print(f"[Gradio] Port {port} is already in use.")

    raise RuntimeError(
        f"[Gradio] No available port found from "
        f"{start_port} to {start_port + max_attempts - 1}."
    )
