import json
import os

from core.logger import log_print
from core.paths import get_lavi_paths


#20260630_kpopmodder: Moved modules.json lookup out of main.py without changing default module behavior.
def module_enabled(module_name, default=True, current_module_directory=None):#20260629_kpopmodder: Let direct main.py components follow modules.json.
    project_root = current_module_directory or os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    paths = get_lavi_paths(project_root)
    modules_paths = [
        paths.config_path("modules.json"),
        paths.root_path("modules.json"),
        paths.config_path("modules.example.json"),
    ]
    try:
        for modules_path in modules_paths:
            if not os.path.exists(modules_path):
                continue
            with open(modules_path, "r", encoding="utf-8") as file:
                modules = json.load(file)
            if not isinstance(modules, dict):
                raise ValueError(f"{modules_path} root must be a JSON object")
            if module_name not in modules:
                #20260716_kpopmodder: Missing optional modules must not become enabled by accident.
                return False
            return modules.get(module_name) is True
        return default
    except Exception as e:
        log_print(f"[Modules] {module_name} setting unavailable: {e}")
        return default
