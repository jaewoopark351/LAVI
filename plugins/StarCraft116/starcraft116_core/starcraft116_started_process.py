#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260706_kpopmodder: Keeps low-level StarCraft launch process creation isolated from policy logic.

class StarCraft116StartedProcess:
    def __init__(self, label, process, command):
        self.label = label
        self.process = process
        self.command = command
