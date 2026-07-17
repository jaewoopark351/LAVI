#20260717_kpopmodder: Compatibility facade for module profile resolution helpers.
from core.profile_resolver_core.module_settings_error import ModuleSettingsError
from core.profile_resolver_core.module_settings_not_found import ModuleSettingsNotFound
from core.profile_resolver_core.module_settings_resolution import (
    ModuleSettingsResolution,
)
from core.profile_resolver_core.module_settings_resolver import (
    MODULES_CONFIG_ENV_VAR,
    PROFILE_ENV_VAR,
    _arg_value,
    _clean,
    _resolve_path,
    active_profile,
    explicit_modules_config,
    load_module_settings,
    normalize_profile,
    resolve_module_settings_path,
)

__all__ = [
    "MODULES_CONFIG_ENV_VAR",
    "PROFILE_ENV_VAR",
    "ModuleSettingsError",
    "ModuleSettingsNotFound",
    "ModuleSettingsResolution",
    "active_profile",
    "explicit_modules_config",
    "load_module_settings",
    "normalize_profile",
    "resolve_module_settings_path",
]
