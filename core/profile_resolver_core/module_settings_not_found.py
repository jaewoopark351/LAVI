#20260717_kpopmodder: Isolates missing module settings error type.
from .module_settings_error import ModuleSettingsError


class ModuleSettingsNotFound(ModuleSettingsError):
    pass
