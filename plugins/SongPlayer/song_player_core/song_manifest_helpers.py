#20260717_kpopmodder: Keeps SongPlayer manifest helper functions outside class modules.
import os


def _plugin_root_from_here():
    return os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def resolve_song_path(plugin_root, path):
    path = str(path or "").strip()
    if not path:
        return ""
    if os.path.isabs(path):
        return path
    return os.path.abspath(os.path.join(plugin_root, path))


def _as_float(value, default):
    try:
        return float(value)
    except Exception:
        return default


def _as_int(value, default):
    try:
        return int(value)
    except Exception:
        return default


def _as_bool(value, default):
    if value is None:
        return default
    if isinstance(value, bool):
        return value
    value = str(value).strip().lower()
    if value in {"1", "true", "yes", "on"}:
        return True
    if value in {"0", "false", "no", "off"}:
        return False
    return default
