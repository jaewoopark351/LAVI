#20260717_kpopmodder: Added this module to keep one project class per Python file.
from dataclasses import dataclass


@dataclass(frozen=True)
class PluginContractIssue:
    #20260717_kpopmodder: Typed issue DTO for validating plugin runtime contracts.
    code: str
    message: str
    path: str = ""
    severity: str = "error"

    def to_dict(self):
        return {
            "code": self.code,
            "message": self.message,
            "path": self.path,
            "severity": self.severity,
        }
