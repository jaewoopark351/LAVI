import json
import os

from core.logger import log_print


#20260630_kpopmodder: Moved modules.json lookup out of main.py without changing default module behavior.
def module_enabled(module_name, default=True, current_module_directory=None):#20260629_kpopmodder: Let direct main.py components follow modules.json.
    if current_module_directory is None:
        current_module_directory = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

    modules_path = os.path.join(current_module_directory, "modules.json")
    try:
        if not os.path.exists(modules_path):
            return default
        with open(modules_path, "r", encoding="utf-8") as file:
            modules = json.load(file)
        return modules.get(module_name, default) is not False
    except Exception as e:
        log_print(f"[Modules] {module_name} setting unavailable: {e}")
        return default
