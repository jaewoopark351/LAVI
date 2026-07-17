#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260706_kpopmodder: Keeps low-level StarCraft launch process creation isolated from policy logic.
import ctypes
from ctypes import wintypes


class StarCraft116ShellProcess:
    #20260703_kpopmodder: Wraps ShellExecuteEx handles from UAC-elevated launches.
    WAIT_OBJECT_0 = 0
    WAIT_TIMEOUT = 258

    def __init__(self, handle, pid=None):
        self.handle = handle
        self.pid = pid or 0

    def poll(self):
        result = ctypes.windll.kernel32.WaitForSingleObject(self.handle, 0)
        if result == self.WAIT_TIMEOUT:
            return None
        if result == self.WAIT_OBJECT_0:
            exit_code = wintypes.DWORD()
            if ctypes.windll.kernel32.GetExitCodeProcess(
                self.handle,
                ctypes.byref(exit_code),
            ):
                return int(exit_code.value)
            return 0
        return None

    def terminate(self):
        ctypes.windll.kernel32.TerminateProcess(self.handle, 1)
