#20260622_kpopmodder: Canonical plugin loading implementation.
import ast
from dataclasses import dataclass
import importlib.util
import json
import os
from pathlib import Path
import subprocess  #20260716_kpopmodder: Kept only so older tests can assert runtime pip is unused.
import sys

from plugin_system.interfaces import (
    InputPluginInterface,
    LLMPluginInterface,
    TranslationPluginInterface,
    TTSPluginInterface,
    VtuberPluginInterface,
)
from plugin_system.registry import plugin_registry

from core.logger import log_print, debug_print#20260612_kpopmodder
from core.paths import get_lavi_paths

plugin_directory = "plugins"
temp_ignore = [] #["silero", "Local_LLM", "voicevox"]


class PluginState:
    #20260716_kpopmodder: Track plugin lifecycle without constructing every provider.
    DISABLED = "DISABLED"
    BROKEN = "BROKEN"
    READY = "READY"
    RUNNING = "RUNNING"


class PluginLoadError(RuntimeError):
    #20260716_kpopmodder: Clear plugin load error used by lazy handles.
    pass


@dataclass(frozen=True)
class PluginDescriptor:
    #20260716_kpopmodder: Describes a provider discovered without importing it.
    plugin_name: str
    class_name: str
    category: str
    interface_name: str
    module_name: str
    module_path: str

    @property
    def status_key(self):
        return f"{self.plugin_name}.{self.class_name}"


class PluginHandle:
    #20260716_kpopmodder: Lazy provider handle; imports and constructs only when selected.
    def __init__(self, descriptor, loader):
        self.descriptor = descriptor
        self.loader = loader
        self.instance = None
        self.status = PluginState.READY
        self.error = ""

    @property
    def name(self):
        return self.descriptor.class_name

    def construct(self, expected_interface=None):
        if self.instance is not None:
            return self.instance
        if self.status == PluginState.BROKEN:
            return None

        try:
            module = self.loader.import_descriptor_module(self.descriptor)
            plugin_class = getattr(module, self.descriptor.class_name)
            if not isinstance(plugin_class, type):
                raise PluginLoadError(
                    f"{self.descriptor.class_name} is not a class"
                )
            if plugin_class.__module__ != module.__name__:
                raise PluginLoadError(
                    f"{self.descriptor.class_name} is imported, not defined in {module.__name__}"
                )
            if expected_interface is None:
                expected_interface = self.loader.interface_name_to_class[
                    self.descriptor.interface_name
                ]
            if not issubclass(plugin_class, expected_interface):
                raise PluginLoadError(
                    f"{self.descriptor.class_name} does not implement {expected_interface.__name__}"
                )
            self.instance = plugin_class()
            self.status = PluginState.READY
            self.error = ""
            return self.instance
        except Exception as e:
            self.status = PluginState.BROKEN
            self.error = str(e)
            self.loader.set_plugin_status(
                self.descriptor.status_key,
                PluginState.BROKEN,
                detail=e,
            )
            log_print(
                "[PluginLoader] plugin construct failed; "
                f"skipped {self.descriptor.status_key}: {e}"
            )#20260716_kpopmodder
            return None

    def mark_running(self):
        self.status = PluginState.RUNNING
        self.loader.set_plugin_status(
            self.descriptor.status_key,
            PluginState.RUNNING,
        )


class PluginLoader:
    def __init__(self, plugin_directory):
        self.current_module_directory = os.path.dirname(
            os.path.dirname(__file__),
        )
        self.plugin_directory = os.path.join(
            self.current_module_directory,
            plugin_directory,
        )
        self.paths = get_lavi_paths(self.current_module_directory)
        self.interface_to_category = {
            InputPluginInterface: 'input_gathering',
            LLMPluginInterface: 'language_model',
            TranslationPluginInterface: 'translation',
            TTSPluginInterface: 'text_to_speech',
            VtuberPluginInterface: 'vtuber'
        }
        self.interface_name_to_class = {
            interface.__name__: interface
            for interface in self.interface_to_category
        }
        self.interface_name_to_category = {
            interface.__name__: category
            for interface, category in self.interface_to_category.items()
        }
        self.plugins = {category: []
                        for category in self.interface_to_category.values()}
        self.plugin_status = {}
        self.registry = plugin_registry
        self._descriptor_keys = set()
        self._loaded = False
        self._settings_loaded = False

        self.plugin_setting_path = str(self.paths.config_path("modules.json"))
        self.legacy_plugin_setting_path = str(self.paths.root_path("modules.json"))
        self.plugin_setting_example_path = str(self.paths.config_path("modules.example.json"))
        self.plugin_setting= {}

    def set_plugin_status(self, name, status, kind="core", detail=""):
        self.plugin_status[name] = status
        self.registry.record(name, status, kind=kind, detail=detail)

    def _load_module_settings(self):
        for modules_path in (
            self.plugin_setting_path,
            self.legacy_plugin_setting_path,
            self.plugin_setting_example_path,
        ):
            if not os.path.exists(modules_path):
                continue
            with open(modules_path, "r", encoding="utf-8") as json_file:
                plugin_setting = json.load(json_file)
            if not isinstance(plugin_setting, dict):
                raise ValueError(f"{modules_path} root must be a JSON object")
            return plugin_setting
        return {}

    def _ensure_module_settings_loaded(self):
        if self._settings_loaded:
            return
        try:
            self.plugin_setting = self._load_module_settings()
        except Exception as e:
            #20260630_kpopmodder: Bad modules.json isolates plugin discovery instead of hiding the error.
            log_print(f"[PluginLoader] modules.json read failed: {e}")
            self.plugin_setting = {}
        self._settings_loaded = True

    def load_plugins(self):
        if self._loaded:
            log_print("[PluginLoader] load_plugins skipped; already loaded")#20260716_kpopmodder
            return

        self._ensure_module_settings_loaded()

        # First, discover plugins directly in the plugin_directory.
        self._load_plugins_from_directory(self.plugin_directory)

        # Next, discover plugins from subdirectories.
        for item_name in os.listdir(self.plugin_directory):
            item_path = os.path.join(self.plugin_directory, item_name)

            # Check if the item is a directory.
            if os.path.isdir(item_path):
                log_print(f"checking: {item_path}")#20260612_kpopmodder
                #log_print(f"temp_ignore: {temp_ignore}")#20260612_kpopmodder
                if item_name in temp_ignore:
                    log_print(f"ignoring {item_path}")#20260612_kpopmodder
                    self.set_plugin_status(item_name, PluginState.DISABLED)
                    continue

                # Check for requirements.txt in the plugin directory.
                requirements_path = os.path.join(item_path, 'requirements.txt')
                if os.path.exists(requirements_path):
                    log_print(
                        "[PluginLoader] runtime requirements install disabled; "
                        f"install plugin dependencies through requirements files: {requirements_path}"
                    )#20260716_kpopmodder: Never call pip while discovering plugins.

                self._load_plugins_from_directory(item_path)

        self._loaded = True
        #20260716_kpopmodder: Do not generate modules.json during discovery; copy config/modules.example.json manually.

    def _load_plugins_from_directory(self, directory):
        self._ensure_module_settings_loaded()
        for file in os.listdir(directory):
            if file.endswith('.py') and not file.startswith('_'):
                module_path = os.path.join(directory, file)
                self._register_descriptors_from_file(directory, module_path)

    def _register_descriptors_from_file(self, directory, module_path):
        plugin_name = os.path.basename(directory)

        # Skip module loading based on config file.
        #20260716_kpopmodder: Missing module keys are disabled when a non-empty modules file exists.
        if self.plugin_setting and self.plugin_setting.get(plugin_name) is not True:
            self.set_plugin_status(plugin_name, PluginState.DISABLED)
            return

        try:
            with open(module_path, "r", encoding="utf-8") as python_file:
                syntax_tree = ast.parse(
                    python_file.read(),
                    filename=module_path,
                )
        except Exception as e:
            status_key = f"{plugin_name}/{os.path.basename(module_path)}"
            self.set_plugin_status(status_key, PluginState.BROKEN, detail=e)
            log_print(
                f"[PluginLoader] plugin discovery failed; skipped "
                f"{status_key}: {e}"
            )#20260716_kpopmodder
            return

        module_name = self._module_name_from_path(module_path)
        for node in syntax_tree.body:
            if not isinstance(node, ast.ClassDef):
                continue
            if node.name.endswith("Interface"):
                continue
            category, interface_name = self._category_from_class_node(node)
            if category is None:
                continue

            descriptor = PluginDescriptor(
                plugin_name=plugin_name,
                class_name=node.name,
                category=category,
                interface_name=interface_name,
                module_name=module_name,
                module_path=str(Path(module_path).resolve()),
            )
            key = (descriptor.module_path, descriptor.class_name)
            if key in self._descriptor_keys:
                continue

            self._descriptor_keys.add(key)
            self.plugins[category].append(PluginHandle(descriptor, self))
            self.set_plugin_status(descriptor.status_key, PluginState.READY)

    def _category_from_class_node(self, class_node):
        for base_node in class_node.bases:
            base_name = self._base_name(base_node)
            category = self.interface_name_to_category.get(base_name)
            if category is not None:
                return category, base_name
        return None, None

    def _base_name(self, node):
        if isinstance(node, ast.Name):
            return node.id
        if isinstance(node, ast.Attribute):
            return node.attr
        return ""

    def _module_name_from_path(self, module_path):
        module_file = Path(module_path).resolve()
        candidates = [
            Path(self.plugin_directory).resolve().parent,
            self.paths.project_root,
        ]
        for base_path in candidates:
            try:
                relative_module_path = module_file.relative_to(base_path)
            except ValueError:
                continue
            return ".".join(relative_module_path.with_suffix("").parts)

        safe_stem = module_file.stem.replace("-", "_")
        return f"_lavi_plugin_{abs(hash(str(module_file)))}_{safe_stem}"

    def import_descriptor_module(self, descriptor):
        if descriptor.module_name in sys.modules:
            return sys.modules[descriptor.module_name]

        spec = importlib.util.spec_from_file_location(
            descriptor.module_name,
            descriptor.module_path,
        )
        if spec is None or spec.loader is None:
            raise PluginLoadError(
                f"Cannot create import spec for {descriptor.module_path}"
            )
        module = importlib.util.module_from_spec(spec)
        #20260710_kpopmodder: Register file-loaded modules before execution.
        # dataclasses and other stdlib decorators inspect sys.modules while the module body is executing.
        sys.modules[descriptor.module_name] = module
        try:
            spec.loader.exec_module(module)
        except Exception:
            sys.modules.pop(descriptor.module_name, None)
            raise
        return module


plugin_loader = PluginLoader(plugin_directory)
