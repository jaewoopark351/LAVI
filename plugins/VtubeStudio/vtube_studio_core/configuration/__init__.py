#20260904_kpopmodder: Expose VTube Studio pose configuration through one responsibility package.
from .vtube_studio_pose_config_loader import (
    load_idle_pose_config,
    load_speaking_pose_config,
)
from .vtube_studio_pose_defaults import (
    DEFAULT_IDLE_POSE_CONFIG,
    DEFAULT_SPEAKING_POSE_CONFIG,
    IDLE_POSE_CONFIG_RANGES,
    SPEAKING_POSE_CONFIG_RANGES,
)


__all__ = [
    "DEFAULT_IDLE_POSE_CONFIG",
    "DEFAULT_SPEAKING_POSE_CONFIG",
    "IDLE_POSE_CONFIG_RANGES",
    "SPEAKING_POSE_CONFIG_RANGES",
    "load_idle_pose_config",
    "load_speaking_pose_config",
]
