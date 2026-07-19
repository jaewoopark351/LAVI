#20260717_kpopmodder: Centralized subprocess launch helpers for Windows-sensitive runtime adapters.
import os
import subprocess


PIPE = subprocess.PIPE
STDOUT = subprocess.STDOUT
DEVNULL = subprocess.DEVNULL
TimeoutExpired = subprocess.TimeoutExpired
CalledProcessError = subprocess.CalledProcessError


def _windows_process_kwargs(kwargs):
    if os.name == "nt" and "creationflags" not in kwargs:
        kwargs["creationflags"] = getattr(subprocess, "CREATE_NO_WINDOW", 0)
    return kwargs


def command_line(command):
    #20260718_kpopmodder: Keep Windows command display/parameter quoting behind one adapter.
    if isinstance(command, str):
        return command
    return subprocess.list2cmdline([str(part) for part in command])


def launch_process(
    command,
    *,
    cwd=None,
    env=None,
    stdout=None,
    stderr=None,
    stdin=None,
    text=None,
    encoding=None,
    errors=None,
    bufsize=None,
    shell=None,
    creationflags=None,
):
    #20260717_kpopmodder: Keep Windows process flags in one place while preserving subprocess.Popen semantics.
    kwargs = {
        "cwd": cwd,
        "env": env,
        "stdout": stdout,
        "stderr": stderr,
        "stdin": stdin,
        "text": text,
        "encoding": encoding,
        "errors": errors,
        "bufsize": bufsize,
        "shell": shell,
        "creationflags": creationflags,
    }
    kwargs = {key: value for key, value in kwargs.items() if value is not None}
    kwargs = _windows_process_kwargs(kwargs)
    return subprocess.Popen(command, **kwargs)


def run_command(
    command,
    *,
    cwd=None,
    env=None,
    stdout=None,
    stderr=None,
    stdin=None,
    input=None,
    capture_output=None,
    text=None,
    encoding=None,
    errors=None,
    timeout=None,
    check=None,
    shell=None,
):
    #20260718_kpopmodder: Centralize subprocess.run for Windows-only helper commands such as taskkill.
    kwargs = {
        "cwd": cwd,
        "env": env,
        "stdout": stdout,
        "stderr": stderr,
        "stdin": stdin,
        "input": input,
        "capture_output": capture_output,
        "text": text,
        "encoding": encoding,
        "errors": errors,
        "timeout": timeout,
        "check": check,
        "shell": shell,
    }
    kwargs = {key: value for key, value in kwargs.items() if value is not None}
    kwargs = _windows_process_kwargs(kwargs)
    return subprocess.run(command, **kwargs)


def check_output(command, *, stderr=None, timeout=None, text=None, encoding=None, errors=None):
    #20260718_kpopmodder: Centralize subprocess.check_output for read-only Windows process probes.
    kwargs = {
        "stderr": stderr,
        "timeout": timeout,
        "text": text,
        "encoding": encoding,
        "errors": errors,
    }
    kwargs = {key: value for key, value in kwargs.items() if value is not None}
    kwargs = _windows_process_kwargs(kwargs)
    return subprocess.check_output(command, **kwargs)


def kill_process_by_image(process_name, *, force=True):
    #20260718_kpopmodder: Keep taskkill invocation in the Windows process adapter.
    name = str(process_name or "").strip()
    if not name:
        return None
    command = ["taskkill", "/IM", name]
    if force:
        command.append("/F")
    return run_command(
        command,
        stdout=DEVNULL,
        stderr=DEVNULL,
        check=False,
    )
