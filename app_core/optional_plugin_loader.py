#20260703_kpopmodder: Added helpers to isolate optional direct plugin imports during startup.
import importlib
import traceback

from app_core.module_config import module_enabled
from core.logger import log_print
from plugin_system.registry import plugin_registry


def instantiate_optional_plugin(
    plugin_name,
    module_path,
    class_name,
    default_enabled,
    project_root,
    *args,
    **kwargs,
):
    if not module_enabled(plugin_name, default_enabled, project_root):
        plugin_registry.record(plugin_name, "DISABLED", kind="optional")
        log_print(
            f"[Startup][DISABLED] [{plugin_name}] optional plugin disabled in modules.json "
            f"(default_enabled={default_enabled})"
        )
        return None

    try:
        module = importlib.import_module(module_path)
    except Exception as e:
        trace = traceback.format_exc().strip()
        plugin_registry.record(plugin_name, "BROKEN", kind="optional", detail=e)
        log_print(
            f"[Startup][BROKEN] [{plugin_name}] enabled plugin module import failed: "
            f"{type(e).__name__}: {e}\n{trace}"
        )
        return None

    try:
        plugin_class = getattr(module, class_name)
    except Exception as e:
        trace = traceback.format_exc().strip()
        plugin_registry.record(plugin_name, "BROKEN", kind="optional", detail=e)
        log_print(
            f"[Startup][BROKEN] [{plugin_name}] enabled plugin missing class '{class_name}': "
            f"{class_name}: {type(e).__name__}: {e}\n{trace}"
        )
        return None

    try:
        plugin = plugin_class(*args, **kwargs)
        plugin_registry.record(plugin_name, "READY", kind="optional")
        return plugin
    except KeyboardInterrupt:
        log_print(
            f"[Startup] [{plugin_name}] optional plugin constructor interrupted."
        )
        raise
    except Exception as e:
        trace = traceback.format_exc().strip()
        plugin_registry.record(plugin_name, "BROKEN", kind="optional", detail=e)
        log_print(
            f"[Startup][BROKEN] [{plugin_name}] enabled plugin constructor failed: "
            f"{type(e).__name__}: {e}\n{trace}"
        )
        return None


def import_optional_attribute(plugin_name, module_path, attribute_name):
    try:
        module = importlib.import_module(module_path)
    except Exception as e:
        trace = traceback.format_exc().strip()
        log_print(
            f"[Startup][BROKEN] [{plugin_name}] enabled helper module import failed: "
            f"{type(e).__name__}: {e}\n{trace}"
        )
        return None

    try:
        return getattr(module, attribute_name)
    except Exception as e:
        trace = traceback.format_exc().strip()
        log_print(
            f"[Startup][BROKEN] [{plugin_name}] enabled helper attribute missing "
            f"'{attribute_name}': {type(e).__name__}: {e}\n{trace}"
        )
        return None
