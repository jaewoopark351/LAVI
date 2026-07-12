#20260622_kpopmodder: Canonical plugin loading implementation.
import importlib.util
import json
import os
import subprocess
import sys

from plugin_system.interfaces import (
    InputPluginInterface,
    LLMPluginInterface,
    TranslationPluginInterface,
    TTSPluginInterface,
    VtuberPluginInterface,
)

from core.logger import log_print, debug_print#20260612_kpopmodder

plugin_directory = "plugins"
temp_ignore = [] #["silero", "Local_LLM", "voicevox"]


class PluginLoader:
    def __init__(self, plugin_directory):
        self.current_module_directory = os.path.dirname(
            os.path.dirname(__file__),
        )
        self.plugin_directory = os.path.join(
            self.current_module_directory,
            plugin_directory,
        )
        self.interface_to_category = {
            InputPluginInterface: 'input_gathering',
            LLMPluginInterface: 'language_model',
            TranslationPluginInterface: 'translation',
            TTSPluginInterface: 'text_to_speech',
            VtuberPluginInterface: 'vtuber'
        }
        self.plugins = {category: []
                        for category in self.interface_to_category.values()}

        self.plugin_setting_path = os.path.join(
            self.current_module_directory,
            "modules.json",
        )
        self.plugin_setting= {}
        self.runtime_pip_install_enabled = (
            str(os.environ.get("LAV_RUNTIME_PIP_INSTALL", "")).strip().lower()
            in {"1", "true", "yes", "on"}
        )#20260630_kpopmodder: Runtime pip install is opt-in; Windows CUDA deps are fragile.

    def load_plugins(self):
        # First, load plugins directly in the plugin_directory
        self._load_plugins_from_directory(self.plugin_directory)

        # Next, load plugins from subdirectories
        for item_name in os.listdir(self.plugin_directory):
            item_path = os.path.join(self.plugin_directory, item_name)

            # Check if the item is a directory
            if os.path.isdir(item_path):
                log_print(f"checking: {item_path}")#20260612_kpopmodder
                #log_print(f"temp_ignore: {temp_ignore}")#20260612_kpopmodder
                if item_name in temp_ignore:
                    log_print(f"ignoring {item_path}")#20260612_kpopmodder
                    continue

                # Check for requirements.txt in the plugin directory
                requirements_path = os.path.join(item_path, 'requirements.txt')
                if os.path.exists(requirements_path):
                    if self.runtime_pip_install_enabled:
                        try:
                            log_print(f"Installing requirements for plugin {item_path}")#20260612_kpopmodder
                            subprocess.run(
                                ['pip', 'install', '-r', requirements_path],
                                check=True,
                            )
                        except Exception as e:
                            #20260630_kpopmodder: Optional dependency install failure must not kill startup.
                            log_print(
                                "[PluginLoader] requirements install failed; "
                                f"plugin will still be imported if possible: "
                                f"{item_path}: {e}"
                            )
                    else:
                        log_print(
                            "[PluginLoader] runtime requirements install skipped "
                            f"(opt-in via LAV_RUNTIME_PIP_INSTALL=1): {item_path}"
                        )#20260630_kpopmodder

                self._load_plugins_from_directory(item_path)

        if not os.path.exists(self.plugin_setting_path):
            with open(self.plugin_setting_path, "w") as json_file:
                json.dump(self.plugin_setting, json_file, indent=4)

    def _load_plugins_from_directory(self, directory):
        for file in os.listdir(directory):
            if file.endswith('.py') and not file.startswith('_'):
                module_path = os.path.join(directory, file)
                #20260703_kpopmodder: Strip only the .py extension; rstrip('.py') mangles names like policy.py.
                module_name = os.path.splitext(module_path)[0].replace(os.sep, '.')
                #20260710_kpopmodder: Keep the package prefix for repository
                # plugins so relative imports resolve correctly even when the
                # loader receives an absolute directory path.
                try:
                    relative_module_path = os.path.relpath(module_path, os.getcwd())
                    if not relative_module_path.startswith(".." + os.sep):
                        module_name = os.path.splitext(relative_module_path)[0].replace(os.sep, '.')
                    elif os.path.isabs(self.plugin_directory):
                        module_name = module_name[len(self.plugin_directory) + 1:]
                except (TypeError, ValueError):
                    if os.path.isabs(self.plugin_directory):
                        module_name = module_name[len(self.plugin_directory) + 1:]
                plugin_name = os.path.basename(directory)

                # skip module loading based on config file
                if os.path.exists(self.plugin_setting_path):
                    try:
                        with open(self.plugin_setting_path, "r") as json_file:
                            plugin_setting = json.load(json_file)
                    except Exception as e:
                        #20260630_kpopmodder: Bad modules.json isolates this plugin instead of stopping the app.
                        log_print(
                            "[PluginLoader] modules.json read failed; "
                            f"skipping {plugin_name}: {e}"
                        )
                        continue
                    #20260627_kpopmodder: Missing modules.json keys should not break new plugin folders.
                    if plugin_setting.get(plugin_name, True) is False:
                        continue

                try:
                    #20260630_kpopmodder: Import failures are isolated per plugin file.
                    spec = importlib.util.spec_from_file_location(
                        module_name, module_path)
                    module = importlib.util.module_from_spec(spec)
                    #20260710_kpopmodder: Register file-loaded modules before execution.
                    # dataclasses and other stdlib decorators inspect sys.modules
                    # while the module body is executing.
                    sys.modules[module_name] = module
                    spec.loader.exec_module(module)
                except Exception as e:
                    log_print(
                        f"[PluginLoader] plugin import failed; skipped "
                        f"{plugin_name}/{file}: {e}"
                    )#20260630_kpopmodder
                    continue

                for attribute_name in dir(module):
                    attribute = getattr(module, attribute_name)
                    if isinstance(attribute, type):
                        # Exclude based on naming convention
                        if attribute_name.endswith('Interface'):
                            continue
                        for interface, category in self.interface_to_category.items():
                            if issubclass(attribute, interface):
                                try:
                                    #20260630_kpopmodder: Constructor failure disables only this provider.
                                    plugin = attribute()
                                except Exception as e:
                                    log_print(
                                        "[PluginLoader] plugin init failed; "
                                        f"skipped {plugin_name}.{attribute_name}: {e}"
                                    )#20260630_kpopmodder
                                    continue
                                self.plugins[category].append(plugin)
                                self.plugin_setting[plugin_name] = True
                                break  # Assumes one plugin class implements only one interface


plugin_loader = PluginLoader(plugin_directory)
