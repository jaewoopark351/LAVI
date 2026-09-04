#20260904_kpopmodder: Validate VTube Studio pose values independently from JSON file loading.


def validate_pose_config(payload, defaults, ranges):
    config = dict(defaults)
    if not isinstance(payload, dict):
        return config, False

    for key, default in defaults.items():
        config[key] = clamp_config_float(
            payload.get(key, default),
            default,
            ranges[key],
        )
    return config, True


def clamp_config_float(value, default, value_range):
    try:
        value = float(value)
    except Exception:
        value = default

    minimum, maximum = value_range
    return max(minimum, min(maximum, value))
