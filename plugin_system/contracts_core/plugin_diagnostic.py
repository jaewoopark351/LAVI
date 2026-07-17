#20260717_kpopmodder: Added this module to keep one project class per Python file.
from dataclasses import dataclass


@dataclass(frozen=True)
class PluginDiagnostic:
    #20260716_kpopmodder: Structured plugin diagnostic without exposing credential values.
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
            "missing_environment_variables": list(
                self.missing_environment_variables
            ),
            "suggested_install_profile": self.suggested_install_profile,
            "suggested_command": self.suggested_command,
            "log_reference": self.log_reference,
        }
