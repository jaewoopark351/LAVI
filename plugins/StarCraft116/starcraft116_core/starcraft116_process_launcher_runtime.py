#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260706_kpopmodder: Keeps low-level StarCraft launch process creation isolated from policy logic.
import ctypes
import os
import subprocess
from ctypes import wintypes

from core.process import command_line, launch_process as _default_launch_process

from .starcraft116_shell_process import StarCraft116ShellProcess
from .starcraft116_started_process import StarCraft116StartedProcess

class StarCraft116ProcessLauncherRuntime:
    def launch_command(self, launch_command, env):
        launch_command = self._normalize_launch_command(launch_command)
        if launch_command["run_as_admin"]:
            return self._launch_elevated(launch_command)

        creationflags = None
        if launch_command.get("show_window") and os.name == "nt":
            creationflags = getattr(subprocess, "CREATE_NEW_CONSOLE", 0)

        return _compat_launch_process(
            launch_command["command"],
            cwd=launch_command["cwd"] or None,
            shell=False,
            env=env,
            creationflags=creationflags,
        )

    def _launch_elevated(self, launch_command):
        if os.name != "nt":
            raise PermissionError("run_as_admin is only supported on Windows.")

        executable = str(launch_command["command"][0])
        parameters = command_line(launch_command["command"][1:])
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
            "show_window": bool(getattr(launch_command, "show_window", False)),
        }


def _compat_launch_process(*args, **kwargs):
    #20260717_kpopmodder: Preserve legacy tests that patch starcraft116_launch_executor.launch_process.
    from . import starcraft116_launch_executor as compatibility_module

    launcher = getattr(compatibility_module, "launch_process", _default_launch_process)
    return launcher(*args, **kwargs)
