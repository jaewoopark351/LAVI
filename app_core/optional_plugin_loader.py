#20260703_kpopmodder: Added helpers to isolate optional direct plugin imports during startup.
import importlib
import importlib.util
from pathlib import Path
import shutil
import traceback

from app_core.module_config import module_enabled
from core.logger import log_print
from plugin_system.contracts import (
    AvailabilityProbeContract,
    PluginDiagnostic,
    PluginRuntimeContract,
    PluginState,
    PluginSupports,
)
from plugin_system.registry import plugin_registry


def instantiate_optional_plugin(
    plugin_name,
    module_path,
    class_name,
    default_enabled,
    project_root,
    *args,
    manifest=None,
    **kwargs,
):
    manifest = dict(manifest or {})
    contract = _optional_runtime_contract(
        plugin_name,
        module_path,
        class_name,
        manifest,
    )
    runtime_contract = contract.to_dict()
    contract_errors = contract.validation_errors()
    if contract_errors:
        diagnostic = _optional_contract_issue_diagnostic(
            plugin_name,
            manifest,
            contract_errors,
        )
        plugin_registry.record(
            plugin_name,
            PluginState.FAILED,
            kind="optional",
            detail=diagnostic.human_readable_message,
            diagnostic=diagnostic.to_dict(),
            runtime_contract=runtime_contract,
        )
        log_print(
            f"[Startup][BROKEN] [{plugin_name}] optional plugin contract invalid: "
            f"{diagnostic.human_readable_message}"
        )
        return None

    if not module_enabled(plugin_name, default_enabled, project_root):
        plugin_registry.record(
            plugin_name,
            PluginState.DISABLED,
            kind="optional",
            runtime_contract=runtime_contract,
        )
        log_print(
            f"[Startup][DISABLED] [{plugin_name}] optional plugin disabled in modules.json "
            f"(default_enabled={default_enabled})"
        )
        return None

    diagnostic = _optional_unavailable_diagnostic(
        plugin_name,
        module_path,
        manifest,
        project_root,
    )
    if diagnostic is not None:
        plugin_registry.record(
            plugin_name,
            PluginState.UNAVAILABLE,
            kind="optional",
            detail=diagnostic.human_readable_message,
            diagnostic=diagnostic.to_dict(),
            runtime_contract=runtime_contract,
        )
        log_print(
            f"[Startup][UNAVAILABLE] [{plugin_name}] optional plugin skipped: "
            f"{diagnostic.human_readable_message}"
        )
        return None

    try:
        module = importlib.import_module(module_path)
    except Exception as e:
        trace = traceback.format_exc().strip()
        diagnostic = _optional_failure_diagnostic(
            plugin_name,
            manifest,
            "optional_import_failed",
            f"Enabled optional plugin module import failed: {type(e).__name__}: {e}",
        )
        plugin_registry.record(
            plugin_name,
            PluginState.FAILED,
            kind="optional",
            detail=e,
            diagnostic=diagnostic.to_dict(),
            runtime_contract=runtime_contract,
        )
        log_print(
            f"[Startup][BROKEN] [{plugin_name}] enabled plugin module import failed: "
            f"{type(e).__name__}: {e}\n{trace}"
        )
        return None

    try:
        plugin_class = getattr(module, class_name)
    except Exception as e:
        trace = traceback.format_exc().strip()
        diagnostic = _optional_failure_diagnostic(
            plugin_name,
            manifest,
            "optional_class_missing",
            f"Enabled optional plugin class {class_name!r} is missing: {type(e).__name__}: {e}",
        )
        plugin_registry.record(
            plugin_name,
            PluginState.FAILED,
            kind="optional",
            detail=e,
            diagnostic=diagnostic.to_dict(),
            runtime_contract=runtime_contract,
        )
        log_print(
            f"[Startup][BROKEN] [{plugin_name}] enabled plugin missing class '{class_name}': "
            f"{class_name}: {type(e).__name__}: {e}\n{trace}"
        )
        return None

    try:
        plugin = plugin_class(*args, **kwargs)
        plugin_registry.record(
            plugin_name,
            PluginState.READY,
            kind="optional",
            runtime_contract=runtime_contract,
        )
        return plugin
    except KeyboardInterrupt:
        log_print(
            f"[Startup] [{plugin_name}] optional plugin constructor interrupted."
        )
        raise
    except Exception as e:
        trace = traceback.format_exc().strip()
        diagnostic = _optional_failure_diagnostic(
            plugin_name,
            manifest,
            "optional_construct_failed",
            f"Enabled optional plugin constructor failed: {type(e).__name__}: {e}",
        )
        plugin_registry.record(
            plugin_name,
            PluginState.FAILED,
            kind="optional",
            detail=e,
            diagnostic=diagnostic.to_dict(),
            runtime_contract=runtime_contract,
        )
        log_print(
            f"[Startup][BROKEN] [{plugin_name}] enabled plugin constructor failed: "
            f"{type(e).__name__}: {e}\n{trace}"
        )
        return None


def _optional_runtime_contract(plugin_name, module_path, class_name, manifest):
    entrypoint = _contract_text(
        manifest,
        "entrypoint",
        f"{module_path}:{class_name}",
    )
    category = _contract_text(manifest, "category", "optional")
    dependency_group = _contract_text(manifest, "dependency_group", "Full")
    supports = manifest.get("supports")
    if not isinstance(supports, dict):
        supports = {}

    return PluginRuntimeContract(
        plugin_id=_contract_text(manifest, "id", plugin_name),
        manifest={
            "id": _contract_text(manifest, "id", plugin_name),
            "display_name": _contract_text(
                manifest,
                "display_name",
                plugin_name,
            ),
            "api_version": _contract_text(manifest, "api_version", "1"),
            "category": category,
            "entrypoint": entrypoint,
            "dependency_group": dependency_group,
        },
        config_schema=_contract_dict(manifest, "config_schema"),
        availability_probe=AvailabilityProbeContract(
            required_python_packages=_availability_tuple(
                manifest,
                "required_python_packages",
            ),
            required_files=_availability_tuple(manifest, "required_files"),
            required_executables=_availability_tuple(
                manifest,
                "required_executables",
            ),
            required_services=_availability_tuple(
                manifest,
                "required_services",
            ),
            timeout_sec=_availability_positive_number(
                manifest,
                "timeout_sec",
                0.25,
            ),
            log_reference=f"Optional plugin availability probe for {plugin_name}",
        ),
        capabilities=_string_tuple(manifest.get("capabilities")),
        supports=PluginSupports(
            offline=_manifest_bool(
                manifest,
                supports,
                "supports_offline",
                "offline",
                False,
            ),
            cpu=_manifest_bool(
                manifest,
                supports,
                "supports_cpu",
                "cpu",
                True,
            ),
            requires_gpu=_manifest_bool(
                manifest,
                supports,
                "requires_gpu",
                "requires_gpu",
                False,
            ),
        ),
    )


def _optional_unavailable_diagnostic(plugin_name, module_path, manifest, project_root):
    missing_packages = [
        package
        for package in _availability_tuple(manifest, "required_python_packages")
        if importlib.util.find_spec(package) is None
    ]
    missing_files = [
        required_path
        for required_path in _availability_tuple(manifest, "required_files")
        if not _resolve_required_file(
            module_path,
            required_path,
            project_root,
        ).exists()
    ]
    missing_executables = [
        executable
        for executable in _availability_tuple(manifest, "required_executables")
        if shutil.which(executable) is None
    ]

    if not (missing_packages or missing_files or missing_executables):
        return None

    display_name = _contract_text(manifest, "display_name", plugin_name)
    return PluginDiagnostic(
        plugin_id=_contract_text(manifest, "id", plugin_name),
        state=PluginState.UNAVAILABLE,
        reason_code="missing_static_dependency",
        human_readable_message=(
            f"{display_name} is enabled but required Python packages, files, "
            "or executables are missing."
        ),
        missing_python_packages=tuple(missing_packages),
        missing_files=tuple(missing_files),
        missing_executables=tuple(missing_executables),
        missing_services=tuple(_availability_tuple(manifest, "required_services")),
        suggested_install_profile=_contract_text(
            manifest,
            "dependency_group",
            "Full",
        ),
        suggested_command=_suggested_install_command(),
    )


def _optional_contract_issue_diagnostic(plugin_name, manifest, issues):
    return PluginDiagnostic(
        plugin_id=_contract_text(manifest, "id", plugin_name),
        state=PluginState.FAILED,
        reason_code=issues[0].code,
        human_readable_message="; ".join(issue.message for issue in issues),
        suggested_install_profile=_contract_text(
            manifest,
            "dependency_group",
            "Full",
        ),
        suggested_command=_suggested_install_command(),
    )


def _optional_failure_diagnostic(plugin_name, manifest, reason_code, message):
    return PluginDiagnostic(
        plugin_id=_contract_text(manifest, "id", plugin_name),
        state=PluginState.FAILED,
        reason_code=reason_code,
        human_readable_message=message,
        missing_services=tuple(_availability_tuple(manifest, "required_services")),
        suggested_install_profile=_contract_text(
            manifest,
            "dependency_group",
            "Full",
        ),
        suggested_command=_suggested_install_command(),
    )


def _resolve_required_file(module_path, required_path, project_root):
    path = Path(required_path)
    if path.is_absolute():
        return path
    if required_path.startswith("plugin:"):
        relative_module = Path(*module_path.split(".")[:-1])
        return Path(project_root) / relative_module / required_path.removeprefix("plugin:")
    return Path(project_root) / path


def _string_tuple(value):
    if value is None:
        return ()
    if isinstance(value, str):
        return (value,)
    if isinstance(value, (list, tuple)):
        return tuple(str(item) for item in value if str(item))
    return ()


def _manifest_section(manifest):
    section = manifest.get("manifest")
    if isinstance(section, dict):
        return section
    return {}


def _availability_section(manifest):
    section = manifest.get("availability_probe")
    if isinstance(section, dict):
        return section
    return {}


def _contract_value(manifest, key, default=None):
    section = _manifest_section(manifest)
    if key in section:
        return section.get(key)
    return manifest.get(key, default)


def _contract_text(manifest, key, default):
    value = _contract_value(manifest, key, default)
    if value is None:
        return str(default)
    return str(value)


def _contract_dict(manifest, key):
    value = manifest.get(key) or {}
    if isinstance(value, dict):
        return dict(value)
    return {}


def _availability_value(manifest, key, default=None):
    section = _availability_section(manifest)
    if key in section:
        return section.get(key)
    return manifest.get(key, default)


def _availability_tuple(manifest, key):
    return _string_tuple(_availability_value(manifest, key))


def _availability_positive_number(manifest, key, default):
    value = _availability_value(manifest, key, default)
    if isinstance(value, bool) or not isinstance(value, (int, float)) or value <= 0:
        return default
    return float(value)


def _manifest_bool(manifest, supports, legacy_key, supports_key, default):
    if legacy_key in manifest:
        return bool(manifest.get(legacy_key))
    if supports_key in supports:
        return bool(supports.get(supports_key))
    return default


def _suggested_install_command():
    return ".\\scripts\\install_windows.ps1 -Profile Full -Accelerator cu130"


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
