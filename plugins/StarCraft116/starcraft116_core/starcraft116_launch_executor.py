#20260706_kpopmodder: Keeps low-level StarCraft launch process creation isolated from policy logic.
import ctypes
import os
import subprocess
from ctypes import wintypes


class StarCraft116StartedProcess:
    def __init__(self, label, process, command):
        self.label = label
        self.process = process
        self.command = command


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


class StarCraft116ProcessLauncherRuntime:
    def launch_command(self, launch_command, env):
        launch_command = self._normalize_launch_command(launch_command)
        if launch_command["run_as_admin"]:
            return self._launch_elevated(launch_command)

        return subprocess.Popen(
            launch_command["command"],
            cwd=launch_command["cwd"] or None,
            shell=False,
            env=env,
        )

    def _launch_elevated(self, launch_command):
        if os.name != "nt":
            raise PermissionError("run_as_admin is only supported on Windows.")

        executable = str(launch_command["command"][0])
        parameters = subprocess.list2cmdline(
            [str(part) for part in launch_command["command"][1:]]
        )
        cwd = launch_command["cwd"] or os.path.dirname(executable)

        class ShellExecuteInfo(ctypes.Structure):
            _fields_ = [
                ("cbSize", wintypes.DWORD),
                ("fMask", wintypes.ULONG),
                ("hwnd", wintypes.HWND),
                ("lpVerb", wintypes.LPCWSTR),
                ("lpFile", wintypes.LPCWSTR),
                ("lpParameters", wintypes.LPCWSTR),
                ("lpDirectory", wintypes.LPCWSTR),
                ("nShow", ctypes.c_int),
                ("hInstApp", wintypes.HINSTANCE),
                ("lpIDList", ctypes.c_void_p),
                ("lpClass", wintypes.LPCWSTR),
                ("hkeyClass", wintypes.HKEY),
                ("dwHotKey", wintypes.DWORD),
                ("hIcon", wintypes.HANDLE),
                ("hProcess", wintypes.HANDLE),
            ]

        shell32 = ctypes.WinDLL("shell32", use_last_error=True)
        shell32.ShellExecuteExW.argtypes = [ctypes.POINTER(ShellExecuteInfo)]
        shell32.ShellExecuteExW.restype = wintypes.BOOL

        info = ShellExecuteInfo()
        info.cbSize = ctypes.sizeof(ShellExecuteInfo)
        info.fMask = 0x00000040
        info.hwnd = None
        info.lpVerb = "runas"
        info.lpFile = executable
        info.lpParameters = parameters
        info.lpDirectory = cwd
        info.nShow = 1

        if not shell32.ShellExecuteExW(ctypes.byref(info)):
            raise ctypes.WinError(ctypes.get_last_error())

        pid = 0
        if info.hProcess:
            pid = ctypes.windll.kernel32.GetProcessId(info.hProcess)
        return StarCraft116ShellProcess(info.hProcess, pid)

    @staticmethod
    def _normalize_launch_command(launch_command):
        if isinstance(launch_command, dict):
            return launch_command

        return {
            "label": getattr(launch_command, "label", ""),
            "command": getattr(launch_command, "command", []),
            "cwd": getattr(launch_command, "cwd", ""),
            "run_as_admin": bool(getattr(launch_command, "run_as_admin", False)),
            "launch_delay_sec": float(
                getattr(launch_command, "launch_delay_sec", 0.0) or 0.0
            ),
        }
