#20260717_kpopmodder: Added this module to keep one project class per Python file.
from dataclasses import dataclass


@dataclass(frozen=True)
class AvailabilityProbeContract:
    #20260716_kpopmodder: Declarative probe inputs; actual probing stays owned by adapters/loader.
    required_python_packages: tuple = ()
    required_files: tuple = ()
    required_executables: tuple = ()
    required_services: tuple = ()
    timeout_sec: float = 0.25
    log_reference: str = ""

    def to_dict(self):
        return {
            "required_python_packages": list(self.required_python_packages),
            "required_files": list(self.required_files),
            "required_executables": list(self.required_executables),
            "required_services": list(self.required_services),
            "timeout_sec": self.timeout_sec,
            "log_reference": self.log_reference,
        }
