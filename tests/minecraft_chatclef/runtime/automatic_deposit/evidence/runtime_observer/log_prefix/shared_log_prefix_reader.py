# 20260901_kpopmodder: Read no more than the cursor-sealed prefix of an active shared log.
from __future__ import annotations

import ctypes
import os
from pathlib import Path


def read_shared_log_prefix(path: Path, size: int) -> bytes:
    if type(size) is not int or size < 0:
        raise ValueError("log prefix size must be a non-negative integer")
    if os.name != "nt":
        with path.open("rb") as stream:
            return stream.read(size)
    return _read_shared_log_prefix_windows(path, size)


def _read_shared_log_prefix_windows(path: Path, size: int) -> bytes:
    from ctypes import wintypes

    kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)
    create_file = kernel32.CreateFileW
    create_file.argtypes = (
        wintypes.LPCWSTR,
        wintypes.DWORD,
        wintypes.DWORD,
        wintypes.LPVOID,
        wintypes.DWORD,
        wintypes.DWORD,
        wintypes.HANDLE,
    )
    create_file.restype = wintypes.HANDLE
    read_file = kernel32.ReadFile
    read_file.argtypes = (
        wintypes.HANDLE,
        wintypes.LPVOID,
        wintypes.DWORD,
        ctypes.POINTER(wintypes.DWORD),
        wintypes.LPVOID,
    )
    read_file.restype = wintypes.BOOL
    close_handle = kernel32.CloseHandle
    close_handle.argtypes = (wintypes.HANDLE,)
    close_handle.restype = wintypes.BOOL

    handle = create_file(
        str(path),
        0x80000000,
        0x00000001 | 0x00000002 | 0x00000004,
        None,
        3,
        0x00000080,
        None,
    )
    if handle == wintypes.HANDLE(-1).value:
        raise ctypes.WinError(ctypes.get_last_error())
    try:
        output = bytearray()
        remaining = size
        while remaining > 0:
            requested = min(1024 * 1024, remaining)
            buffer = ctypes.create_string_buffer(requested)
            read = wintypes.DWORD(0)
            if not read_file(handle, buffer, requested, ctypes.byref(read), None):
                raise ctypes.WinError(ctypes.get_last_error())
            if read.value == 0:
                break
            output.extend(buffer.raw[: read.value])
            remaining -= read.value
        return bytes(output)
    finally:
        close_handle(handle)
