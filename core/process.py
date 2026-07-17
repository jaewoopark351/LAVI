#20260717_kpopmodder: Centralized subprocess launch helpers for Windows-sensitive runtime adapters.
import os
import subprocess


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
    }
    kwargs = {key: value for key, value in kwargs.items() if value is not None}
    if os.name == "nt":
        kwargs["creationflags"] = getattr(subprocess, "CREATE_NO_WINDOW", 0)
    return subprocess.Popen(command, **kwargs)
