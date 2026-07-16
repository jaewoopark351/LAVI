#20260622_kpopmodder: Canonical plugin loading implementation.
import ast
from dataclasses import dataclass
import importlib.util
import os
from pathlib import Path
import shutil
import subprocess  # noqa: F401  #20260716_kpopmodder: Kept so tests can assert runtime pip is unused.
import sys

from plugin_system.interfaces import (
    InputPluginInterface,
    LLMPluginInterface,
    TranslationPluginInterface,
    TTSPluginInterface,
    VtuberPluginInterface,
)
from plugin_system.registry import plugin_registry

from core.logger import log_print#20260612_kpopmodder
from core.paths import get_lavi_paths
from core.profile_resolver import ModuleSettingsNotFound, load_module_settings

plugin_directory = "plugins"
temp_ignore = [] #["silero", "Local_LLM", "voicevox"]


class PluginState:
    #20260716_kpopmodder: Track plugin lifecycle without constructing every provider.
    DISABLED = "DISABLED"
    UNAVAILABLE = "UNAVAILABLE"
    READY = "READY"
    STARTING = "STARTING"
    RUNNING = "RUNNING"
    FAILED = "FAILED"
    STOPPED = "STOPPED"
    BROKEN = FAILED  #20260716_kpopmodder: Backward-compatible alias for older tests/log checks.


class PluginLoadError(RuntimeError):
    #20260716_kpopmodder: Clear plugin load error used by lazy handles.
    pass


@dataclass(frozen=True)
class PluginDiagnostic:
    #20260716_kpopmodder: Structured P1 diagnostic without exposing credential values.
    plugin_id: str
    state: str
    reason_code: str
    human_readable_message: str
    missing_python_packages: tuple = ()
    missing_files: tuple = ()
    missing_executables: tuple = ()
    missing_services: tuple = ()
    missing_environment_variables: tuple = ()
    suggested_install_profile: str = ""
    suggested_command: str = ""
    log_reference: str = ""

    def to_dict(self):
        return {
            "plugin_id": self.plugin_id,
            "state": self.state,
            "reason_code": self.reason_code,
            "human_readable_message": self.human_readable_message,
            "missing_python_packages": list(self.missing_python_packages),
            "missing_files": list(self.missing_files),
            "missing_executables": list(self.missing_executables),
            "missing_services": list(self.missing_services),
            "missing_environment_variables": list(self.missing_environment_variables),
            "suggested_install_profile": self.suggested_install_profile,
            "suggested_command": self.suggested_command,
            "log_reference": self.log_reference,
        }


@dataclass(frozen=True)
class PluginDescriptor:
    #20260716_kpopmodder: Describes a provider discovered without importing it.
    plugin_name: str
    class_name: str
    category: str
    interface_name: str
    module_name: str
    module_path: str
    id: str = ""
    display_name: str = ""
    api_version: str = "1"
    dependency_group: str = ""
    capabilities: tuple = ()
    config_schema: dict = None
    required_python_packages: tuple = ()
    required_files: tuple = ()
    required_executables: tuple = ()
    required_services: tuple = ()
    supports_offline: bool = False
    supports_cpu: bool = True

    @property
    def status_key(self):
        return f"{self.plugin_name}.{self.class_name}"

    @property
    def entrypoint(self):
        return f"{self.module_name}:{self.class_name}"

    def __post_init__(self):
        if self.config_schema is None:
            object.__setattr__(self, "config_schema", {})
        if not self.id:
            object.__setattr__(self, "id", self.class_name)
        if not self.display_name:
            object.__setattr__(self, "display_name", self.class_name)
        if not self.dependency_group:
            object.__setattr__(self, "dependency_group", self.category)


class PluginHandle:
    #20260716_kpopmodder: Lazy provider handle; imports and constructs only when selected.
    def __init__(self, descriptor, loader, status=PluginState.READY, diagnostic=None):
        self.descriptor = descriptor
        self.loader = loader
        self.instance = None
        self.status = status
        self.error = ""
        self.diagnostic = diagnostic

    @property
    def name(self):
        return self.descriptor.class_name

    def _set_state(self, state, detail="", diagnostic=None):
        self.status = state
        self.error = str(detail or "")
        self.diagnostic = diagnostic
        self.loader.set_plugin_status(
            self.descriptor.status_key,
            state,
            detail=detail,
            diagnostic=diagnostic.to_dict() if diagnostic else None,
        )

    def check_availability(self):
        if self.status in (PluginState.UNAVAILABLE, PluginState.FAILED):
            return False

        missing_packages = [
            package
            for package in self.descriptor.required_python_packages
            if importlib.util.find_spec(package) is None
        ]
        missing_files = [
            path
            for path in self.descriptor.required_files
            if not self.loader.resolve_required_file(self.descriptor, path).exists()
        ]
        missing_executables = [
            executable
            for executable in self.descriptor.required_executables
            if shutil.which(executable) is None
        ]

        if not (missing_packages or missing_files or missing_executables):
            return True

        diagnostic = PluginDiagnostic(
            plugin_id=self.descriptor.id,
            state=PluginState.UNAVAILABLE,
            reason_code="missing_static_dependency",
            human_readable_message=(
                f"{self.descriptor.display_name} is enabled but required "
                "Python packages, files, or executables are missing."
            ),
            missing_python_packages=tuple(missing_packages),
            missing_files=tuple(missing_files),
            missing_executables=tuple(missing_executables),
            missing_services=tuple(self.descriptor.required_services),
            suggested_install_profile=self.descriptor.dependency_group,
            suggested_command=self.loader.suggested_install_command(self.descriptor),
        )
        self._set_state(
            PluginState.UNAVAILABLE,
            detail=diagnostic.human_readable_message,
            diagnostic=diagnostic,
        )
        log_print(
            "[PluginLoader] plugin unavailable; "
            f"skipped {self.descriptor.status_key}: "
            f"{diagnostic.human_readable_message}"
        )#20260716_kpopmodder
        return False

    def construct(self, expected_interface=None):
        if self.instance is not None:
            return self.instance
        if self.status in (PluginState.UNAVAILABLE, PluginState.FAILED):
            return None
        if not self.check_availability():
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
            self.mark_failed(e, reason_code="construct_failed")
            log_print(
                "[PluginLoader] plugin construct failed; "
                f"skipped {self.descriptor.status_key}: {e}"
            )#20260716_kpopmodder
            return None

    def mark_starting(self):
        self._set_state(PluginState.STARTING)

    def mark_running(self):
        self._set_state(PluginState.RUNNING)

    def mark_failed(self, error, reason_code="plugin_failed"):
        diagnostic = PluginDiagnostic(
            plugin_id=self.descriptor.id,
            state=PluginState.FAILED,
            reason_code=reason_code,
            human_readable_message=str(error),
        )
        self._set_state(PluginState.FAILED, detail=error, diagnostic=diagnostic)

    def mark_stopped(self):
        self._set_state(PluginState.STOPPED)


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
        self._metadata_ids = {}
        self._loaded = False
        self._settings_loaded = False

        self.plugin_setting_path = str(self.paths.config_path("modules.json"))
        self.production_plugin_setting_path = str(self.paths.root_path("modules.json"))
        #20260716_kpopmodder: Compatibility alias for older tests; root modules.json is production config, not legacy policy.
        self.legacy_plugin_setting_path = self.production_plugin_setting_path
        self.plugin_setting_example_path = str(self.paths.config_path("modules.example.json"))
        self.plugin_setting= {}

    def set_plugin_status(self, name, status, kind="core", detail="", diagnostic=None):
        self.plugin_status[name] = status
        self.registry.record(
            name,
            status,
            kind=kind,
            detail=detail,
            diagnostic=diagnostic,
        )

    def _load_module_settings(self):
        default_user_path = self.paths.config_path("modules.json").resolve()
        configured_path = Path(self.plugin_setting_path).resolve()
        explicit_modules_config = None
        if configured_path != default_user_path:
            explicit_modules_config = self.plugin_setting_path

        resolution = load_module_settings(
            self.paths.project_root,
            modules_config=explicit_modules_config,
        )
        return resolution.settings

    def _ensure_module_settings_loaded(self):
        if self._settings_loaded:
            return
        try:
            self.plugin_setting = self._load_module_settings()
        except ModuleSettingsNotFound as e:
            #20260716_kpopmodder: Missing production modules.json is an actionable startup error, not an example fallback.
            log_print(f"[PluginLoader] modules.json not found: {e}")
            raise
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

            metadata = self._metadata_from_class_node(
                class_node=node,
                plugin_name=plugin_name,
                class_name=node.name,
                category=category,
            )
            descriptor = PluginDescriptor(
                plugin_name=plugin_name,
                class_name=node.name,
                category=category,
                interface_name=interface_name,
                module_name=module_name,
                module_path=str(Path(module_path).resolve()),
                **metadata,
            )
            key = (descriptor.module_path, descriptor.class_name)
            if key in self._descriptor_keys:
                continue

            self._descriptor_keys.add(key)
            handle = self._handle_for_descriptor(descriptor)
            self.plugins[category].append(handle)
            self.set_plugin_status(
                descriptor.status_key,
                handle.status,
                detail=handle.error,
                diagnostic=handle.diagnostic.to_dict() if handle.diagnostic else None,
            )

    def _handle_for_descriptor(self, descriptor):
        if descriptor.api_version != "1":
            diagnostic = PluginDiagnostic(
                plugin_id=descriptor.id,
                state=PluginState.FAILED,
                reason_code="api_version_mismatch",
                human_readable_message=(
                    f"Unsupported plugin API version {descriptor.api_version!r}; "
                    "expected '1'."
                ),
            )
            return PluginHandle(
                descriptor,
                self,
                status=PluginState.FAILED,
                diagnostic=diagnostic,
            )

        duplicate = self._metadata_ids.get(descriptor.id)
        if duplicate is not None:
            diagnostic = PluginDiagnostic(
                plugin_id=descriptor.id,
                state=PluginState.FAILED,
                reason_code="duplicate_plugin_id",
                human_readable_message=(
                    f"Duplicate plugin id {descriptor.id!r}; "
                    f"first seen at {duplicate}."
                ),
            )
            return PluginHandle(
                descriptor,
                self,
                status=PluginState.FAILED,
                diagnostic=diagnostic,
            )

        self._metadata_ids[descriptor.id] = descriptor.entrypoint
        return PluginHandle(descriptor, self)

    def _metadata_from_class_node(self, class_node, plugin_name, class_name, category):
        metadata = {}
        for body_node in class_node.body:
            target_name = ""
            value_node = None
            if isinstance(body_node, ast.Assign):
                for target in body_node.targets:
                    if isinstance(target, ast.Name):
                        target_name = target.id
                        value_node = body_node.value
                        break
            elif isinstance(body_node, ast.AnnAssign):
                if isinstance(body_node.target, ast.Name):
                    target_name = body_node.target.id
                    value_node = body_node.value

            if target_name not in ("PLUGIN_METADATA", "plugin_metadata"):
                continue
            try:
                parsed = ast.literal_eval(value_node)
            except Exception:
                parsed = {}
            if isinstance(parsed, dict):
                metadata = parsed
            break

        return {
            "id": str(metadata.get("id") or class_name),
            "display_name": str(metadata.get("display_name") or class_name),
            "api_version": str(metadata.get("api_version") or "1"),
            "dependency_group": str(metadata.get("dependency_group") or category),
            "capabilities": tuple(self._string_list(metadata.get("capabilities"))),
            "config_schema": dict(metadata.get("config_schema") or {}),
            "required_python_packages": tuple(self._string_list(metadata.get("required_python_packages"))),
            "required_files": tuple(self._string_list(metadata.get("required_files"))),
            "required_executables": tuple(self._string_list(metadata.get("required_executables"))),
            "required_services": tuple(self._string_list(metadata.get("required_services"))),
            "supports_offline": bool(metadata.get("supports_offline", False)),
            "supports_cpu": bool(metadata.get("supports_cpu", True)),
        }

    def _string_list(self, value):
        if value is None:
            return []
        if isinstance(value, str):
            return [value]
        if isinstance(value, (list, tuple)):
            return [str(item) for item in value if str(item)]
        return []

    def resolve_required_file(self, descriptor, required_path):
        path = Path(required_path)
        if path.is_absolute():
            return path
        if required_path.startswith("plugin:"):
            plugin_dir = Path(descriptor.module_path).resolve().parent
            return plugin_dir / required_path.removeprefix("plugin:")
        return self.paths.project_root / path

    def suggested_install_command(self, descriptor):
        group = descriptor.dependency_group
        if group.lower() in ("core", "input_gathering", "voice"):
            return ".\\scripts\\install_windows.ps1 -Profile Full -Accelerator cu130"
        return ".\\scripts\\install_windows.ps1 -Profile Full -Accelerator cu130"

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
