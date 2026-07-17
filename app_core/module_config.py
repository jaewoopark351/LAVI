import os

from core.logger import log_print
from core.profile_resolver import (
    ModuleSettingsNotFound,
    ModuleSettingsSnapshot,
    load_module_settings,
)


#20260630_kpopmodder: Moved modules.json lookup out of main.py without changing default module behavior.
def module_enabled(module_name, default=True, current_module_directory=None):#20260629_kpopmodder: Let direct main.py components follow modules.json.
    project_root = current_module_directory or os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    try:
        resolution = load_module_settings(project_root)
        modules = ModuleSettingsSnapshot.from_resolution(resolution)
        if not modules.has_module(module_name):
            #20260716_kpopmodder: Missing optional modules must not become enabled by accident.
            return False
        return modules.is_enabled(module_name)
    except ModuleSettingsNotFound:
        raise
    except Exception as e:
        log_print(f"[Modules] {module_name} setting unavailable: {e}")
        #20260716_kpopmodder: Malformed/read-failed settings fail closed instead of honoring default_enabled=True.
        return False
