#20260622_kpopmodder: Canonical plugin loading implementation.
import ast
import importlib
import importlib.util
import os
from pathlib import Path
import subprocess  # noqa: F401  #20260716_kpopmodder: Kept so tests can assert runtime pip is unused.
import sys

from plugin_system.availability_probe_service import AvailabilityProbeService
from plugin_system.availability_diagnostic_service import AvailabilityDiagnosticService
from plugin_system.interfaces import (
    InputPluginInterface,
    LLMPluginInterface,
    TranslationPluginInterface,
    TTSPluginInterface,
    VtuberPluginInterface,
)
from plugin_system.contracts import (
    PluginDiagnostic,
    PluginState,
)
from plugin_system.loader_core.plugin_descriptor import PluginDescriptor
from plugin_system.loader_core.plugin_handle import PluginHandle
from plugin_system.loader_core.plugin_load_error import PluginLoadError
from plugin_system.registry import plugin_registry

from core.logger import log_print#20260612_kpopmodder
from core.paths import get_lavi_paths
from core.profile_resolver import (
    ModuleSettingsError,
    ModuleSettingsNotFound,
    load_module_settings,
)

plugin_directory = "plugins"
temp_ignore = [] #["silero", "Local_LLM", "voicevox"]
_MISSING = object()


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
        self.availability_probe_service = AvailabilityProbeService()
        self.availability_diagnostic_service = AvailabilityDiagnosticService(
            self.availability_probe_service,
        )

    def set_plugin_status(
        self,
        name,
        status,
        kind="core",
        detail="",
        diagnostic=None,
        runtime_contract=None,
    ):
        self.plugin_status[name] = status
        self.registry.record(
            name,
            status,
            kind=kind,
            detail=detail,
            diagnostic=diagnostic,
            runtime_contract=runtime_contract,
        )

    def get_runtime_contracts(self):
        return {
            category: [
                handle.runtime_contract.to_dict()
                for handle in handles
            ]
            for category, handles in self.plugins.items()
        }

    def get_diagnostics(self):
        return {
            category: [
                handle.diagnostic_snapshot()
                for handle in handles
            ]
            for category, handles in self.plugins.items()
        }

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
        except ModuleSettingsError as e:
            #20260716_kpopmodder: Malformed module settings must fail closed before plugin discovery.
            log_print(f"[PluginLoader] modules.json invalid: {e}")
            raise
        except Exception as e:
            #20260716_kpopmodder: Read/parse failures are actionable startup errors, not fail-open discovery.
            log_print(f"[PluginLoader] modules.json read failed: {e}")
            raise ModuleSettingsError(
                f"Unable to load module settings from {self.plugin_setting_path}: {e}"
            ) from e
        self._settings_loaded = True

    def load_plugins(self):
        if self._loaded:
            log_print("[PluginLoader] load_plugins skipped; already loaded")#20260716_kpopmodder
            return

        self._ensure_module_settings_loaded()

        # First, discover plugins directly in the plugin_directory.
        self._load_plugins_from_directory(self.plugin_directory)

        # Next, discover plugins from subdirectories.
        for item_name in self._list_plugin_directory(self.plugin_directory):
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
        for file in self._list_plugin_directory(directory):
            if file.endswith('.py') and not file.startswith('_'):
                module_path = os.path.join(directory, file)
                self._register_descriptors_from_file(directory, module_path)

    def _list_plugin_directory(self, directory):
        try:
            return sorted(os.listdir(directory))
        except OSError as e:
            message = f"Plugin directory unavailable: {directory}: {e}"
            diagnostic = PluginDiagnostic(
                plugin_id="plugin_directory",
                state=PluginState.FAILED,
                reason_code="plugin_directory_unavailable",
                human_readable_message=message,
                missing_files=(str(directory),),
                log_reference="PluginLoader plugin directory discovery",
            )
            self.set_plugin_status(
                f"plugin_directory:{directory}",
                PluginState.FAILED,
                detail=message,
                diagnostic=diagnostic.to_dict(),
            )
            log_print(f"[PluginLoader] {message}")#20260718_kpopmodder
            raise PluginLoadError(message) from e

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

            metadata, metadata_errors = self._metadata_from_class_node(
                class_node=node,
                plugin_name=plugin_name,
                class_name=node.name,
                category=category,
                module_name=module_name,
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
            handle = self._handle_for_descriptor(
                descriptor,
                validation_errors=metadata_errors,
            )
            self.plugins[category].append(handle)
            self.set_plugin_status(
                descriptor.status_key,
                handle.status,
                detail=handle.error,
                diagnostic=handle.diagnostic.to_dict() if handle.diagnostic else None,
                runtime_contract=descriptor.runtime_contract.to_dict(),
            )

    def _handle_for_descriptor(self, descriptor, validation_errors=()):
        if validation_errors:
            reason_code, message = validation_errors[0]
            diagnostic = PluginDiagnostic(
                plugin_id=descriptor.id,
                state=PluginState.FAILED,
                reason_code=reason_code,
                human_readable_message="; ".join(
                    error_message for _code, error_message in validation_errors
                ),
            )
            return PluginHandle(
                descriptor,
                self,
                status=PluginState.FAILED,
                diagnostic=diagnostic,
            )

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

        contract_errors = descriptor.runtime_contract.validation_errors()
        if contract_errors:
            diagnostic = self._contract_issue_diagnostic(
                descriptor,
                contract_errors,
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
        diagnostic = self.availability_diagnostic(descriptor)
        if diagnostic is not None:
            return PluginHandle(
                descriptor,
                self,
                status=PluginState.UNAVAILABLE,
                diagnostic=diagnostic,
            )
        return PluginHandle(descriptor, self)

    def _contract_issue_diagnostic(self, descriptor, issues):
        return PluginDiagnostic(
            plugin_id=descriptor.id,
            state=PluginState.FAILED,
            reason_code=issues[0].code,
            human_readable_message="; ".join(
                issue.message for issue in issues
            ),
            log_reference=f"PluginLoader contract validation for {descriptor.status_key}",
        )

    def availability_diagnostic(self, descriptor):
        availability_probe = descriptor.runtime_contract.availability_probe
        model_missing_files = self._probe_model_file_contract(descriptor)
        return self.availability_diagnostic_service.build_diagnostic(
            plugin_id=descriptor.id,
            display_name=descriptor.display_name,
            availability_probe=availability_probe,
            resolve_file=lambda path: self.resolve_required_file(descriptor, path),
            extra_missing_files=tuple(model_missing_files),
            dependency_group=descriptor.dependency_group,
            suggested_command=self.suggested_install_command(descriptor),
            log_reference=f"PluginLoader availability probe for {descriptor.status_key}",
        )

    def _probe_model_file_contract(self, descriptor):
        if descriptor.id != "GPTSoVITS":
            return []
        missing = []
        ckpt_dir = self.resolve_required_file(descriptor, "plugin:gpt_sovits_ckpt_dir")
        model_dir = self.resolve_required_file(descriptor, "plugin:gpt_sovits_model_dir")
        if not any(ckpt_dir.glob("*.ckpt")):
            missing.append(str(ckpt_dir / "*.ckpt"))
        if not any(model_dir.glob("*.pth")):
            missing.append(str(model_dir / "*.pth"))
        return missing

    def _metadata_from_class_node(
        self,
        class_node,
        plugin_name,
        class_name,
        category,
        module_name,
    ):
        metadata = {}
        errors = []
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
            except Exception as e:
                errors.append((
                    "metadata_not_literal",
                    f"{plugin_name}.{class_name} metadata must be a Python literal: {e}",
                ))
                parsed = {}
            if isinstance(parsed, dict):
                metadata = parsed
            else:
                errors.append((
                    "metadata_root_type",
                    f"{plugin_name}.{class_name} metadata must be a dict",
                ))
            break

        manifest = self._metadata_dict(
            metadata,
            "manifest",
            errors,
            plugin_name,
            class_name,
        )
        availability_probe = self._metadata_dict(
            metadata,
            "availability_probe",
            errors,
            plugin_name,
            class_name,
        )

        expected_entrypoint = f"{module_name}:{class_name}"
        metadata_category = self._contract_text(
            metadata,
            manifest,
            "category",
            category,
            errors,
            plugin_name,
            class_name,
            "manifest",
        )
        if metadata_category != category:
            errors.append((
                "metadata_category_mismatch",
                (
                    f"{plugin_name}.{class_name} metadata category "
                    f"{metadata_category!r} does not match interface category {category!r}"
                ),
            ))

        metadata_entrypoint = self._contract_text(
            metadata,
            manifest,
            "entrypoint",
            expected_entrypoint,
            errors,
            plugin_name,
            class_name,
            "manifest",
        )
        if metadata_entrypoint != expected_entrypoint:
            errors.append((
                "metadata_entrypoint_mismatch",
                (
                    f"{plugin_name}.{class_name} metadata entrypoint "
                    f"{metadata_entrypoint!r} does not match {expected_entrypoint!r}"
                ),
            ))

        config_schema = self._metadata_dict(
            metadata,
            "config_schema",
            errors,
            plugin_name,
            class_name,
        )
        supports_offline, supports_cpu, requires_gpu = self._metadata_supports(
            metadata,
            errors,
            plugin_name,
            class_name,
        )

        return {
            "id": self._contract_text(metadata, manifest, "id", class_name, errors, plugin_name, class_name, "manifest"),
            "display_name": self._contract_text(metadata, manifest, "display_name", class_name, errors, plugin_name, class_name, "manifest"),
            "api_version": self._contract_text(metadata, manifest, "api_version", "1", errors, plugin_name, class_name, "manifest"),
            "dependency_group": self._contract_text(metadata, manifest, "dependency_group", category, errors, plugin_name, class_name, "manifest"),
            "capabilities": tuple(self._metadata_string_sequence(metadata, "capabilities", errors, plugin_name, class_name)),
            "config_schema": config_schema,
            "required_python_packages": tuple(self._contract_string_sequence(metadata, availability_probe, "required_python_packages", errors, plugin_name, class_name, "availability_probe")),
            "required_files": tuple(self._contract_string_sequence(metadata, availability_probe, "required_files", errors, plugin_name, class_name, "availability_probe")),
            "required_executables": tuple(self._contract_string_sequence(metadata, availability_probe, "required_executables", errors, plugin_name, class_name, "availability_probe")),
            "required_services": tuple(self._contract_string_sequence(metadata, availability_probe, "required_services", errors, plugin_name, class_name, "availability_probe")),
            "availability_probe_timeout_sec": self._contract_positive_number(metadata, availability_probe, "timeout_sec", 0.25, errors, plugin_name, class_name, "availability_probe"),
            "availability_probe_log_reference": self._contract_text(metadata, availability_probe, "log_reference", "", errors, plugin_name, class_name, "availability_probe"),
            "supports_offline": supports_offline,
            "supports_cpu": supports_cpu,
            "requires_gpu": requires_gpu,
        }, tuple(errors)

    def _contract_value(self, metadata, nested, key, errors, plugin_name, class_name, section_name):
        has_top_level = key in metadata
        has_nested = key in nested
        if (
            has_top_level
            and has_nested
            and not self._metadata_values_equal(metadata.get(key), nested.get(key))
        ):
            errors.append((
                f"metadata_conflicting_{section_name}_{key}",
                (
                    f"{plugin_name}.{class_name} metadata {key} and "
                    f"{section_name}.{key} must not disagree"
                ),
            ))
        if has_nested:
            return nested.get(key), f"{section_name}.{key}"
        if has_top_level:
            return metadata.get(key), key
        return _MISSING, ""

    def _metadata_values_equal(self, left, right):
        if isinstance(left, (list, tuple)) and isinstance(right, (list, tuple)):
            return list(left) == list(right)
        return left == right

    def _contract_text(self, metadata, nested, key, default, errors, plugin_name, class_name, section_name):
        value, label = self._contract_value(
            metadata,
            nested,
            key,
            errors,
            plugin_name,
            class_name,
            section_name,
        )
        if value is _MISSING:
            return default
        if not isinstance(value, str) or not value.strip():
            errors.append((
                f"metadata_invalid_{key}",
                f"{plugin_name}.{class_name} metadata {label} must be a non-empty string",
            ))
            return default
        return value.strip()

    def _contract_string_sequence(self, metadata, nested, key, errors, plugin_name, class_name, section_name):
        value, label = self._contract_value(
            metadata,
            nested,
            key,
            errors,
            plugin_name,
            class_name,
            section_name,
        )
        if value is _MISSING:
            return []
        return self._validate_string_sequence_value(
            value,
            key,
            label,
            errors,
            plugin_name,
            class_name,
        )

    def _contract_positive_number(self, metadata, nested, key, default, errors, plugin_name, class_name, section_name):
        value, label = self._contract_value(
            metadata,
            nested,
            key,
            errors,
            plugin_name,
            class_name,
            section_name,
        )
        if value is _MISSING:
            return default
        if isinstance(value, bool) or not isinstance(value, (int, float)) or value <= 0:
            errors.append((
                f"metadata_invalid_{key}",
                f"{plugin_name}.{class_name} metadata {label} must be a positive number",
            ))
            return default
        return float(value)

    def _optional_text(self, metadata, key, default, errors, plugin_name, class_name):
        if key not in metadata:
            return default
        value = metadata.get(key)
        if not isinstance(value, str) or not value.strip():
            errors.append((
                f"metadata_invalid_{key}",
                f"{plugin_name}.{class_name} metadata {key} must be a non-empty string",
            ))
            return default
        return value.strip()

    def _metadata_dict(self, metadata, key, errors, plugin_name, class_name):
        if key not in metadata:
            return {}
        value = metadata.get(key)
        if not isinstance(value, dict):
            errors.append((
                f"metadata_invalid_{key}",
                f"{plugin_name}.{class_name} metadata {key} must be an object",
            ))
            return {}
        return dict(value)

    def _metadata_string_sequence(self, metadata, key, errors, plugin_name, class_name):
        if key not in metadata:
            return []
        return self._validate_string_sequence_value(
            metadata.get(key),
            key,
            key,
            errors,
            plugin_name,
            class_name,
        )

    def _validate_string_sequence_value(self, value, key, label, errors, plugin_name, class_name):
        if not isinstance(value, (list, tuple)):
            errors.append((
                f"metadata_invalid_{key}",
                f"{plugin_name}.{class_name} metadata {label} must be a list or tuple of strings",
            ))
            return []
        items = []
        for item in value:
            if not isinstance(item, str) or not item.strip():
                errors.append((
                    f"metadata_invalid_{key}",
                    f"{plugin_name}.{class_name} metadata {label} entries must be non-empty strings",
                ))
                continue
            items.append(item.strip())
        return items

    def _metadata_supports(self, metadata, errors, plugin_name, class_name):
        supports_offline = False
        supports_cpu = True
        requires_gpu = False
        supports = metadata.get("supports")
        if supports is not None:
            if not isinstance(supports, dict):
                errors.append((
                    "metadata_invalid_supports",
                    f"{plugin_name}.{class_name} metadata supports must be an object",
                ))
            else:
                if "offline" in supports:
                    supports_offline = self._metadata_bool(
                        supports,
                        "offline",
                        supports_offline,
                        errors,
                        plugin_name,
                        class_name,
                        "supports.offline",
                    )
                if "cpu" in supports:
                    supports_cpu = self._metadata_bool(
                        supports,
                        "cpu",
                        supports_cpu,
                        errors,
                        plugin_name,
                        class_name,
                        "supports.cpu",
                    )
                if "requires_gpu" in supports:
                    requires_gpu = self._metadata_bool(
                        supports,
                        "requires_gpu",
                        requires_gpu,
                        errors,
                        plugin_name,
                        class_name,
                        "supports.requires_gpu",
                    )
        supports_offline = self._metadata_bool(
            metadata,
            "supports_offline",
            supports_offline,
            errors,
            plugin_name,
            class_name,
        )
        supports_cpu = self._metadata_bool(
            metadata,
            "supports_cpu",
            supports_cpu,
            errors,
            plugin_name,
            class_name,
        )
        requires_gpu = self._metadata_bool(
            metadata,
            "requires_gpu",
            requires_gpu,
            errors,
            plugin_name,
            class_name,
        )
        return supports_offline, supports_cpu, requires_gpu

    def _metadata_bool(
        self,
        metadata,
        key,
        default,
        errors,
        plugin_name,
        class_name,
        label=None,
    ):
        if key not in metadata:
            return default
        value = metadata.get(key)
        if not isinstance(value, bool):
            errors.append((
                f"metadata_invalid_{key}",
                f"{plugin_name}.{class_name} metadata {label or key} must be a boolean",
            ))
            return default
        return value

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
