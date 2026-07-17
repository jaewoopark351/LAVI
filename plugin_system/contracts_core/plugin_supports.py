#20260717_kpopmodder: Added this module to keep one project class per Python file.
from dataclasses import dataclass


@dataclass(frozen=True)
class PluginSupports:
    #20260716_kpopmodder: Runtime capability flags separated from plugin metadata parsing.
    offline: bool = False
    cpu: bool = True
    requires_gpu: bool = False

    def to_dict(self):
        return {
            "offline": self.offline,
            "cpu": self.cpu,
            "requires_gpu": self.requires_gpu,
        }
