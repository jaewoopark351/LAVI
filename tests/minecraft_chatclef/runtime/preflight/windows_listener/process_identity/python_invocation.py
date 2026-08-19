#20260819_kpopmodder: Classify the effective Python invocation before entrypoint provenance is evaluated.
from __future__ import annotations

import ntpath
from typing import Mapping

from .windows_command_line import parse_windows_command_line


PYTHON_PROCESS_NAMES = {"python.exe", "pythonw.exe"}
PYTHON_EXECUTABLE_NAMES = {"python.exe"}
PYTHON_COMMAND_NAMES = {"python.exe"}
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
    executable_name = ntpath.basename(executable_path).lower()
    if executable_name not in PYTHON_EXECUTABLE_NAMES:
        return _failure("listener owner executable is not Python")
    reported_name = _required_text(process.get("name")).lower()
    if reported_name != executable_name:
        return _failure("Python process name and executable path disagree")
    arguments = parse_windows_command_line(process.get("command_line"))
    if not arguments:
        return _failure("Python command line is unavailable or malformed")
    command_name = ntpath.basename(arguments[0]).lower()
    if command_name not in PYTHON_COMMAND_NAMES:
        return _failure("Python command line does not start with an interpreter")
    command_path = _normalize_path(arguments[0])
    if ntpath.isabs(command_path) and command_path != _normalize_path(executable_path):
        return _failure("Python command argv0 and executable path disagree")
    if not ntpath.isabs(command_path) and arguments[0].lower() != "python.exe":
        return _failure("Python command argv0 is not the exact interpreter name")

    index = 1
    while index < len(arguments):
        argument = arguments[index]
        if argument == "--":
            index += 1
            break
        if argument == "-c":
            if index + 1 >= len(arguments):
                return _failure("Python -c operand is missing")
            return {
                "ok": True,
                "mode": "python_code",
                "operand": arguments[index + 1],
                "executable_name": executable_name,
                "strict_script_form": False,
            }
        if argument == "-m":
            if index + 1 >= len(arguments):
                return _failure("Python -m operand is missing")
            return {
                "ok": True,
                "mode": "python_module",
                "operand": arguments[index + 1],
                "executable_name": executable_name,
                "strict_script_form": False,
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
    return {
        "ok": True,
        "mode": "python_script",
        "operand": arguments[index],
        "executable_name": executable_name,
        "strict_script_form": index == 1 and len(arguments) == 2,
    }


def _required_text(value: object) -> str:
    return value.strip() if type(value) is str else ""


def _normalize_path(value: object) -> str:
    text = _required_text(value)
    return ntpath.normcase(ntpath.normpath(text.replace("/", "\\"))) if text else ""


def _failure(reason: str) -> dict[str, object]:
    return {
        "ok": False,
        "reason": reason,
        "mode": "invalid",
        "operand": "",
        "executable_name": "",
        "strict_script_form": False,
    }
