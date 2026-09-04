#20260904_kpopmodder: Load VTube Studio pose JSON while delegating value validation.
import json
import os

from core.logger import log_print
from .vtube_studio_pose_config_validator import validate_pose_config
from .vtube_studio_pose_defaults import (
    DEFAULT_IDLE_POSE_CONFIG,
    DEFAULT_SPEAKING_POSE_CONFIG,
    IDLE_POSE_CONFIG_RANGES,
    SPEAKING_POSE_CONFIG_RANGES,
)


def load_speaking_pose_config(config_path):
    return _load_pose_config(
        config_path,
        DEFAULT_SPEAKING_POSE_CONFIG,
        SPEAKING_POSE_CONFIG_RANGES,
        "speaking",
    )


def load_idle_pose_config(config_path):
    return _load_pose_config(
        config_path,
        DEFAULT_IDLE_POSE_CONFIG,
        IDLE_POSE_CONFIG_RANGES,
        "idle",
    )


def _load_pose_config(config_path, defaults, ranges, pose_name):
    try:
        if not os.path.exists(config_path):
            return dict(defaults)

        with open(config_path, "r", encoding="utf-8") as file:
            payload = json.load(file)

        config, valid_shape = validate_pose_config(payload, defaults, ranges)
        if not valid_shape:
            log_print(
                f"[VtubeStudio] {pose_name} pose config must be a JSON object.",
                level="warning",
            )
        return config
    except Exception as error:
        log_print(
            f"[VtubeStudio] {pose_name} pose config load failed: {error}",
            level="warning",
        )
        return dict(defaults)
