#20260818_kpopmodder: Read one file while allowing active Windows log sharing.
from __future__ import annotations

import ctypes
import os
from pathlib import Path


def read_shared_bytes(path: Path) -> bytes:
    if os.name != "nt":
        return path.read_bytes()
    return _read_shared_bytes_windows(path)


def _read_shared_bytes_windows(path: Path) -> bytes:
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

    generic_read = 0x80000000
    share_read_write_delete = 0x00000001 | 0x00000002 | 0x00000004
    open_existing = 3
    file_attribute_normal = 0x00000080
    invalid_handle = wintypes.HANDLE(-1).value
    handle = create_file(
        str(path),
        generic_read,
        share_read_write_delete,
        None,
        open_existing,
        file_attribute_normal,
        None,
    )
    if handle == invalid_handle:
        raise ctypes.WinError(ctypes.get_last_error())
    try:
        size = path.stat().st_size
        output = bytearray()
        remaining = size
        chunk_size = 1024 * 1024
        while remaining > 0:
            requested = min(chunk_size, remaining)
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
