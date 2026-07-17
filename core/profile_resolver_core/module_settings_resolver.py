#20260717_kpopmodder: Keeps module settings path resolution logic separate from DTO and error classes.
import json
import os
import sys
from pathlib import Path

from core.paths import get_lavi_paths

from .module_settings_error import ModuleSettingsError
from .module_settings_not_found import ModuleSettingsNotFound
from .module_settings_resolution import ModuleSettingsResolution


PROFILE_ENV_VAR = "LAVI_PROFILE"
MODULES_CONFIG_ENV_VAR = "LAVI_MODULES_CONFIG"


def _clean(value) -> str:
    return str(value or "").strip().strip("\"'")


def _arg_value(argv, option_name: str) -> str:
    args = list(sys.argv[1:] if argv is None else argv)
    for index, arg in enumerate(args):
        if arg == option_name and index + 1 < len(args):
            return _clean(args[index + 1])
        prefix = f"{option_name}="
        if arg.startswith(prefix):
            return _clean(arg[len(prefix):])
    return ""


def normalize_profile(profile: str | None) -> str:
    value = _clean(profile)
    if not value:
        return ""
    lowered = value.lower()
    if lowered == "core":
        return "Core"
    if lowered == "voice":
        return "Voice"
    if lowered == "vision":
        return "Vision"
    if lowered == "games":
        return "Games"
    if lowered == "full":
        return "Full"
    return value


def active_profile(argv=None, environ=None, profile: str | None = None) -> str:
    env = os.environ if environ is None else environ
    return normalize_profile(
        profile
        or _arg_value(argv, "--profile")
        or _clean(env.get(PROFILE_ENV_VAR))
    )


def explicit_modules_config(argv=None, environ=None, modules_config: str | None = None) -> str:
    env = os.environ if environ is None else environ
    return (
        _clean(modules_config)
        or _arg_value(argv, "--modules-config")
        or _clean(env.get(MODULES_CONFIG_ENV_VAR))
    )


def _resolve_path(project_root, value: str) -> Path:
    return get_lavi_paths(project_root).resolve_path(value)


def resolve_module_settings_path(
    project_root=None,
    argv=None,
    environ=None,
    profile: str | None = None,
    modules_config: str | None = None,
) -> tuple[Path, str, str]:
    paths = get_lavi_paths(project_root)
    selected_profile = active_profile(argv=argv, environ=environ, profile=profile)

    explicit_config = explicit_modules_config(
        argv=argv,
        environ=environ,
        modules_config=modules_config,
    )
    if explicit_config:
        resolved = _resolve_path(paths.project_root, explicit_config)
        if resolved is None or not resolved.exists():
            raise ModuleSettingsNotFound(
                f"Explicit modules config not found: {explicit_config}"
            )
        return resolved, "explicit", selected_profile

    if selected_profile == "Core":
        core_path = paths.config_path("modules.core.json")
        if not core_path.exists():
            raise ModuleSettingsNotFound(
                f"Core modules config not found: {core_path}"
            )
        return core_path, "profile_core", selected_profile

    user_path = paths.config_path("modules.json")
    if user_path.exists():
        return user_path, "user", selected_profile

    production_path = paths.root_path("modules.json")
    if production_path.exists():
        return production_path, "production", selected_profile

    raise ModuleSettingsNotFound(
        "No modules config found; expected explicit --modules-config, "
        "config/modules.json, or tracked root modules.json"
    )


def load_module_settings(
    project_root=None,
    argv=None,
    environ=None,
    profile: str | None = None,
    modules_config: str | None = None,
) -> ModuleSettingsResolution:
    path, source, selected_profile = resolve_module_settings_path(
        project_root=project_root,
        argv=argv,
        environ=environ,
        profile=profile,
        modules_config=modules_config,
    )
    with open(path, "r", encoding="utf-8") as modules_file:
        settings = json.load(modules_file)
    if not isinstance(settings, dict):
        raise ModuleSettingsError(f"{path} root must be a JSON object")
    return ModuleSettingsResolution(
        path=path,
        source=source,
        profile=selected_profile,
        settings=settings,
    )
