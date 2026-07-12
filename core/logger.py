#20260622_kpopmodder: Canonical logger module for app-wide logging.
import logging
import os
from datetime import datetime

LOG_DIR = "logs"
os.makedirs(LOG_DIR, exist_ok=True)

timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
LOG_FILE = f"{timestamp}_log.txt"
LOG_PATH = os.path.join(LOG_DIR, LOG_FILE)

DEBUG_MODE = True  # Keep debug logging enabled during active troubleshooting.

logger = logging.getLogger("LAV")
logger.setLevel(logging.DEBUG)
logger.propagate = False

MEMORY_LOGGER_NAME = "LAV.memory_core"#20260627_kpopmodder

if not logger.handlers:
    formatter = logging.Formatter(
        "[%(asctime)s] [%(levelname)s] %(filename)s:%(lineno)d - %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )

    #20260705_kpopmodder: Use UTF-8 with BOM so Windows editors detect Korean logs reliably.
    file_handler = logging.FileHandler(
        LOG_PATH,
        encoding="utf-8-sig",
    )
    file_handler.setLevel(logging.DEBUG)
    file_handler.setFormatter(formatter)

    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)
    console_handler.setFormatter(formatter)

    try:
        console_handler.stream.reconfigure(encoding="utf-8")
    except Exception:
        pass

    logger.addHandler(file_handler)
    logger.addHandler(console_handler)

memory_logger = logging.getLogger(MEMORY_LOGGER_NAME)
memory_logger.setLevel(logging.INFO)
memory_logger.propagate = True#20260627_kpopmodder: Keep memory recall/router evidence visible without raising root logging.


def log_print(*args, level="info", sep=" ", **kwargs):
    msg = sep.join(map(str, args))

    level = level.lower()

    if level == "debug":
        logger.debug(msg)
    elif level == "warning":
        logger.warning(msg)
    elif level == "error":
        logger.error(msg)
    elif level == "critical":
        logger.critical(msg)
    else:
        logger.info(msg)


def debug_print(*args, sep=" ", **kwargs):
    if DEBUG_MODE:
        msg = sep.join(map(str, args))
        logger.debug("[DEBUG] " + msg)


def log_exception(message="Exception occurred"):
    logger.exception(message)


def get_log_path():
    return LOG_PATH
