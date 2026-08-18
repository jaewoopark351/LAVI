#20260819_kpopmodder: Classify the effective Python invocation before entrypoint provenance is evaluated.
from __future__ import annotations

import ntpath
from typing import Mapping

from .windows_command_line import parse_windows_command_line


PYTHON_EXECUTABLE_NAMES = {"python", "python.exe", "pythonw", "pythonw.exe"}
NO_VALUE_OPTIONS = {
    "-b",
    "-bb",
    "-B",
    "-E",
    "-i",
    "-I",
    "-O",
    "-OO",
    "-q",
    "-s",
    "-S",
    "-u",
    "-v",
    "-x",
}
VALUE_OPTIONS = {"-W", "-X"}


def inspect_python_invocation(process: Mapping[str, object]) -> dict[str, object]:
    executable_path = _required_text(process.get("executable_path"))
    if not executable_path or not ntpath.isabs(executable_path):
        return _failure("Python executable path is unavailable")
    if ntpath.basename(executable_path).lower() not in PYTHON_EXECUTABLE_NAMES:
        return _failure("listener owner executable is not Python")
    arguments = parse_windows_command_line(process.get("command_line"))
    if not arguments:
        return _failure("Python command line is unavailable or malformed")
    if ntpath.basename(arguments[0]).lower() not in PYTHON_EXECUTABLE_NAMES:
        return _failure("Python command line does not start with an interpreter")

    index = 1
    while index < len(arguments):
        argument = arguments[index]
        if argument == "--":
            index += 1
            break
        if argument == "-c":
            if index + 1 >= len(arguments):
                return _failure("Python -c operand is missing")
            return {"ok": True, "mode": "python_code", "operand": arguments[index + 1]}
        if argument == "-m":
            if index + 1 >= len(arguments):
                return _failure("Python -m operand is missing")
            return {
                "ok": True,
                "mode": "python_module",
                "operand": arguments[index + 1],
            }
        if argument in NO_VALUE_OPTIONS:
            index += 1
            continue
        if argument in VALUE_OPTIONS:
            if index + 1 >= len(arguments):
                return _failure(f"Python {argument} operand is missing")
            index += 2
            continue
        if argument.startswith("-"):
            return _failure("Python invocation uses an unsupported option")
        break

    if index >= len(arguments):
        return _failure("Python script operand is missing")
    return {"ok": True, "mode": "python_script", "operand": arguments[index]}


def _required_text(value: object) -> str:
    return value.strip() if type(value) is str else ""


def _failure(reason: str) -> dict[str, object]:
    return {"ok": False, "reason": reason, "mode": "invalid", "operand": ""}
