#20260819_kpopmodder: Parse Windows process command lines without treating arbitrary tokens as entrypoints.
from __future__ import annotations

import ctypes
import os


def parse_windows_command_line(value: object) -> tuple[str, ...]:
    if type(value) is not str or not value.strip():
        return ()
    if not _quotes_are_balanced(value):
        return ()
    if os.name == "nt":
        return _native_windows_arguments(value)
    return _portable_windows_arguments(value)


def _native_windows_arguments(value: str) -> tuple[str, ...]:
    try:
        argument_count = ctypes.c_int(0)
        shell32 = ctypes.WinDLL("shell32", use_last_error=True)
        command_line_to_argv = shell32.CommandLineToArgvW
        command_line_to_argv.argtypes = (
            ctypes.c_wchar_p,
            ctypes.POINTER(ctypes.c_int),
        )
        command_line_to_argv.restype = ctypes.POINTER(ctypes.c_wchar_p)
        arguments = command_line_to_argv(value, ctypes.byref(argument_count))
    except (AttributeError, OSError, TypeError, ValueError):
        return ()
    if not arguments or argument_count.value <= 0:
        return ()
    try:
        return tuple(arguments[index] for index in range(argument_count.value))
    finally:
        try:
            kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)
            kernel32.LocalFree.argtypes = (ctypes.c_void_p,)
            kernel32.LocalFree.restype = ctypes.c_void_p
            kernel32.LocalFree(ctypes.cast(arguments, ctypes.c_void_p))
        except (AttributeError, OSError, TypeError, ValueError):
            pass


def _portable_windows_arguments(value: str) -> tuple[str, ...]:
    arguments: list[str] = []
    index = 0
    length = len(value)
    while index < length:
        while index < length and value[index] in " \t":
            index += 1
        if index >= length:
            break
        argument: list[str] = []
        quoted = False
        while index < length:
            character = value[index]
            if character in " \t" and not quoted:
                break
            if character == "\\":
                slash_start = index
                while index < length and value[index] == "\\":
                    index += 1
                slash_count = index - slash_start
                if index < length and value[index] == '"':
                    argument.extend("\\" * (slash_count // 2))
                    if slash_count % 2:
                        argument.append('"')
                        index += 1
                        continue
                    quoted = not quoted
                    index += 1
                    continue
                argument.extend("\\" * slash_count)
                continue
            if character == '"':
                if quoted and index + 1 < length and value[index + 1] == '"':
                    argument.append('"')
                    index += 2
                    continue
                quoted = not quoted
                index += 1
                continue
            argument.append(character)
            index += 1
        arguments.append("".join(argument))
        while index < length and value[index] in " \t":
            index += 1
    return tuple(arguments)


def _quotes_are_balanced(value: str) -> bool:
    quoted = False
    slash_count = 0
    for character in value:
        if character == "\\":
            slash_count += 1
            continue
        if character == '"' and slash_count % 2 == 0:
            quoted = not quoted
        slash_count = 0
    return not quoted
